package com.optimus.ssh.core.network

import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Host
import com.optimus.ssh.model.HostStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

data class ActiveSession(
    val hostId: String,
    val sshClient: SSHClient,
    val jumpHostClient: SSHClient? = null,
    val shellSession: Session? = null,
    val shell: Session.Shell? = null,
    val inputStream: InputStream? = null,
    val outputStream: OutputStream? = null
)

class SshConnectionManager {

    private val _connectionStates = ConcurrentHashMap<String, MutableStateFlow<HostStatus>>()
    private val activeSessions = ConcurrentHashMap<String, ActiveSession>()

    fun getConnectionState(hostId: String): StateFlow<HostStatus> {
        return _connectionStates.getOrPut(hostId) {
            MutableStateFlow(HostStatus.DISCONNECTED)
        }.asStateFlow()
    }

    suspend fun connect(
        host: Host,
        password: String? = null,
        privateKeyContent: String? = null,
        passphrase: String? = null,
        jumpHost: Host? = null,
        jumpHostPassword: String? = null,
        jumpHostKeyContent: String? = null
    ): Result<ActiveSession> = withContext(Dispatchers.IO) {
        val stateFlow = _connectionStates.getOrPut(host.id) { MutableStateFlow(HostStatus.DISCONNECTED) }
        stateFlow.value = HostStatus.CONNECTING

        var jumpClient: SSHClient? = null

        try {
            // 1. If Jump Host / Bastion is specified, establish hop 1 first
            if (jumpHost != null) {
                jumpClient = createConfiguredClient()
                jumpClient.connect(jumpHost.hostname, jumpHost.port)

                if (jumpHost.authType == AuthType.KEY && jumpHostKeyContent != null) {
                    val keyProvider = jumpClient.loadKeys(jumpHostKeyContent, null, null)
                    jumpClient.authPublickey(jumpHost.username, keyProvider)
                } else if (!jumpHostPassword.isNullOrEmpty()) {
                    jumpClient.authPassword(jumpHost.username, jumpHostPassword)
                }

                if (!jumpClient.isAuthenticated) {
                    throw Exception("Authentication to Jump Host (${jumpHost.label}) failed")
                }
            }

            // 2. Establish connection to Target Host
            val client = createConfiguredClient()

            if (jumpClient != null) {
                // ProxyJump via direct-tcpip channel
                val tunnel = jumpClient.newDirectConnection(host.hostname, host.port)
                client.connectVia(tunnel)
            } else {
                client.connect(host.hostname, host.port)
            }

            stateFlow.value = HostStatus.CONNECTED

            // 3. Authenticate to Target Host
            val passwordFinder = if (!passphrase.isNullOrEmpty()) {
                net.schmizz.sshj.userauth.password.PasswordUtils.createOneOff(passphrase.toCharArray())
            } else null

            when (host.authType) {
                AuthType.PASSWORD -> {
                    if (password != null) {
                        client.authPassword(host.username, password)
                    } else {
                        throw IllegalArgumentException("Password required for password authentication")
                    }
                }
                AuthType.KEY -> {
                    if (!privateKeyContent.isNullOrBlank()) {
                        val keyProvider = client.loadKeys(privateKeyContent, null, passwordFinder)
                        client.authPublickey(host.username, keyProvider)
                    } else {
                        throw IllegalArgumentException("Key material required for key authentication")
                    }
                }
                AuthType.PASSWORD_AND_KEY -> {
                    if (!privateKeyContent.isNullOrBlank()) {
                        val keyProvider = client.loadKeys(privateKeyContent, null, passwordFinder)
                        client.authPublickey(host.username, keyProvider)
                    }
                    if (password != null) {
                        client.authPassword(host.username, password)
                    }
                }
                AuthType.AGENT -> {
                    // Agent forwarding
                }
            }

            if (!client.isAuthenticated) {
                stateFlow.value = HostStatus.ERROR
                client.disconnect()
                jumpClient?.disconnect()
                return@withContext Result.failure(Exception("SSH Authentication Failed for ${host.username}@${host.hostname}"))
            }

            stateFlow.value = HostStatus.AUTHENTICATED

            // 4. Start interactive shell session
            val session = client.startSession()
            session.allocatePTY("xterm-256color", 80, 24, 0, 0, emptyMap())
            val shell = session.startShell()

            val active = ActiveSession(
                hostId = host.id,
                sshClient = client,
                jumpHostClient = jumpClient,
                shellSession = session,
                shell = shell,
                inputStream = shell.inputStream,
                outputStream = shell.outputStream
            )
            activeSessions[host.id] = active

            Result.success(active)
        } catch (e: Exception) {
            stateFlow.value = HostStatus.ERROR
            disconnect(host.id)
            jumpClient?.let {
                try { it.disconnect() } catch (ignored: Exception) {}
            }
            Result.failure(e)
        }
    }

    private fun createConfiguredClient(): SSHClient {
        return SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connectTimeout = 15000
            timeout = 30000

            // Exclude curve25519 and group-exchange on Android to prevent system provider conflicts
            val kex = ArrayList(transport.config.keyExchangeFactories)
            kex.removeIf { factory ->
                val name = factory.name.lowercase()
                name.contains("curve25519") || name.contains("group-exchange")
            }
            transport.config.keyExchangeFactories = kex
        }
    }

    fun getActiveSession(hostId: String): ActiveSession? = activeSessions[hostId]

    fun disconnect(hostId: String) {
        val session = activeSessions.remove(hostId)
        try {
            session?.shell?.close()
            session?.shellSession?.close()
            session?.sshClient?.disconnect()
            session?.jumpHostClient?.disconnect()
        } catch (ignored: Exception) {}

        _connectionStates[hostId]?.value = HostStatus.DISCONNECTED
    }

    fun disconnectAll() {
        activeSessions.keys.forEach { disconnect(it) }
    }
}
