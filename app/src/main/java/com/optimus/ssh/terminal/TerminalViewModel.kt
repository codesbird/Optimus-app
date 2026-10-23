package com.optimus.ssh.terminal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.optimus.ssh.core.network.ActiveSession
import com.optimus.ssh.core.network.SshConnectionManager
import com.optimus.ssh.data.repository.HostRepository
import com.optimus.ssh.data.repository.IdentityRepository
import com.optimus.ssh.data.repository.KeyRepository
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Host
import com.optimus.ssh.model.HostStatus
import com.optimus.ssh.service.SshForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class TerminalUiState(
    val host: Host? = null,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = true,
    val needsPasswordInput: Boolean = false,
    val title: String = "Terminal",
    val errorMessage: String? = null
)

class TerminalViewModel(
    private val hostId: String,
    private val hostRepository: HostRepository,
    private val keyRepository: KeyRepository,
    private val identityRepository: IdentityRepository,
    private val sshConnectionManager: SshConnectionManager,
    private val context: Context
) : ViewModel() {

    val emulator = TerminalEmulator()

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var activeSession: ActiveSession? = null
    private var outputStream: OutputStream? = null

    private var lastCols = 0
    private var lastRows = 0

    init {
        connect()
        observeWindowTitle()
    }

    fun connect(enteredPassword: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null, needsPasswordInput = false) }
            val host = hostRepository.getHostById(hostId)
            _uiState.update { it.copy(host = host) }

            if (host == null) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = "Host not found in database") }
                return@launch
            }

            // 1. Resolve effective credentials (direct host vs associated identity)
            var effectiveUsername = host.username
            var effectiveAuthType = host.authType
            var effectivePassword = enteredPassword ?: hostRepository.getDecryptedPassword(hostId)
            var effectivePrivateKeyId = host.privateKeyId

            if (host.identityId != null) {
                val identity = identityRepository.getIdentityById(host.identityId)
                if (identity != null) {
                    effectiveUsername = identity.username
                    effectiveAuthType = identity.authType
                    effectivePassword = identityRepository.getDecryptedPassword(identity.id) ?: effectivePassword
                    effectivePrivateKeyId = identity.privateKeyId ?: effectivePrivateKeyId
                }
            }

            val privateKeyContent = if (effectivePrivateKeyId != null) {
                keyRepository.getDecryptedPrivateKey(effectivePrivateKeyId)
            } else null

            // If password authentication is selected and password is missing, prompt user
            if (effectivePassword.isNullOrEmpty() && effectiveAuthType == AuthType.PASSWORD && privateKeyContent == null) {
                _uiState.update { it.copy(isConnecting = false, needsPasswordInput = true) }
                return@launch
            }

            // 2. Resolve Jump Host / Bastion (if configured)
            val jumpHost = if (host.jumpHostId != null) {
                hostRepository.getHostById(host.jumpHostId)
            } else null
            val jumpHostPassword = if (host.jumpHostId != null) {
                var p = hostRepository.getDecryptedPassword(host.jumpHostId)
                if (p == null && jumpHost?.identityId != null) {
                    p = identityRepository.getDecryptedPassword(jumpHost.identityId)
                }
                p
            } else null
            val jumpHostKeyContent = if (jumpHost?.privateKeyId != null) {
                keyRepository.getDecryptedPrivateKey(jumpHost.privateKeyId)
            } else if (jumpHost?.identityId != null) {
                val jumpIdentity = identityRepository.getIdentityById(jumpHost.identityId)
                if (jumpIdentity?.privateKeyId != null) {
                    keyRepository.getDecryptedPrivateKey(jumpIdentity.privateKeyId)
                } else null
            } else null

            val effectiveHost = host.copy(
                username = effectiveUsername,
                authType = effectiveAuthType
            )

            // 3. Connect to SSH session
            var session = sshConnectionManager.getActiveSession(hostId)
            if (session == null || session.outputStream == null) {
                val connectResult = sshConnectionManager.connect(
                    host = effectiveHost,
                    password = effectivePassword,
                    privateKeyContent = privateKeyContent,
                    jumpHost = jumpHost,
                    jumpHostPassword = jumpHostPassword,
                    jumpHostKeyContent = jumpHostKeyContent
                )
                if (connectResult.isSuccess) {
                    session = connectResult.getOrNull()
                    if (!enteredPassword.isNullOrEmpty()) {
                        hostRepository.saveHost(host, enteredPassword)
                    }
                } else {
                    val rawErr = connectResult.exceptionOrNull()
                    val err = rawErr?.localizedMessage ?: rawErr?.toString() ?: "Failed to connect"
                    _uiState.update { it.copy(isConnecting = false, isConnected = false, errorMessage = err) }
                    return@launch
                }
            }

            activeSession = session
            outputStream = session?.outputStream

            _uiState.update {
                it.copy(
                    isConnected = true,
                    isConnecting = false,
                    title = host.label
                )
            }

            // Start foreground service keep-alive
            SshForegroundService.startService(context, host.label)

            // Start reading incoming bytes
            session?.inputStream?.let { inputStream ->
                startReading(inputStream)
            }
        }
    }

    private fun startReading(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            try {
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    if (read > 0) {
                        val text = String(buffer, 0, read, Charsets.UTF_8)
                        emulator.feed(text)
                    }
                }
            } catch (e: Exception) {
                // Stream closed
            } finally {
                withContext(Dispatchers.Main) {
                    val wasConnected = _uiState.value.isConnected
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            errorMessage = if (wasConnected) "Session closed" else "Connection failed"
                        )
                    }
                    hostRepository.updateHostStatus(hostId, HostStatus.DISCONNECTED)
                    SshForegroundService.stopService(context)
                }
            }
        }
    }

    private fun observeWindowTitle() {
        viewModelScope.launch {
            emulator.windowTitle.collect { title ->
                _uiState.update { it.copy(title = title.ifBlank { it.host?.label ?: "Terminal" }) }
            }
        }
    }

    fun sendInput(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = outputStream
                if (stream != null) {
                    stream.write(text.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            } catch (ignored: Exception) {}
        }
    }

    fun resizePty(cols: Int, rows: Int) {
        if (cols <= 0 || rows <= 0 || (cols == lastCols && rows == lastRows)) return
        lastCols = cols
        lastRows = rows
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shell = activeSession?.shell
                if (shell != null && shell.isOpen) {
                    shell.changeWindowDimensions(cols, rows, 0, 0)
                }
            } catch (ignored: Exception) {}
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            sshConnectionManager.disconnect(hostId)
            hostRepository.updateHostStatus(hostId, HostStatus.DISCONNECTED)
            SshForegroundService.stopService(context)
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    class Factory(
        private val hostId: String,
        private val hostRepository: HostRepository,
        private val keyRepository: KeyRepository,
        private val identityRepository: IdentityRepository,
        private val sshConnectionManager: SshConnectionManager,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TerminalViewModel(
                hostId,
                hostRepository,
                keyRepository,
                identityRepository,
                sshConnectionManager,
                context
            ) as T
        }
    }
}
