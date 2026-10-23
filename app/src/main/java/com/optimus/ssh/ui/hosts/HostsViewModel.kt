package com.optimus.ssh.ui.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.optimus.ssh.core.network.SshConnectionManager
import com.optimus.ssh.data.repository.HostRepository
import com.optimus.ssh.data.repository.IdentityRepository
import com.optimus.ssh.data.repository.KeyRepository
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Host
import com.optimus.ssh.model.HostStatus
import com.optimus.ssh.model.Identity
import com.optimus.ssh.model.KeyRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class HostsUiState(
    val hosts: List<Host> = emptyList(),
    val filteredHosts: List<Host> = emptyList(),
    val availableIdentities: List<Identity> = emptyList(),
    val availableKeys: List<KeyRecord> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isAddEditDialogOpen: Boolean = false,
    val hostToEdit: Host? = null,
    val errorMessage: String? = null
)

class HostsViewModel(
    private val hostRepository: HostRepository,
    private val identityRepository: IdentityRepository,
    private val keyRepository: KeyRepository,
    private val sshConnectionManager: SshConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostsUiState(isLoading = true))
    val uiState: StateFlow<HostsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            hostRepository.getHosts().collect { hostList ->
                _uiState.update { state ->
                    state.copy(
                        hosts = hostList,
                        filteredHosts = filterHosts(hostList, state.searchQuery),
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            identityRepository.getIdentities().collect { idList ->
                _uiState.update { it.copy(availableIdentities = idList) }
            }
        }
        viewModelScope.launch {
            keyRepository.getKeys().collect { keyList ->
                _uiState.update { it.copy(availableKeys = keyList) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredHosts = filterHosts(state.hosts, query)
            )
        }
    }

    private fun filterHosts(hosts: List<Host>, query: String): List<Host> {
        if (query.isBlank()) return hosts
        val lower = query.lowercase().trim()
        return hosts.filter { host ->
            host.label.lowercase().contains(lower) ||
            host.hostname.lowercase().contains(lower) ||
            host.username.lowercase().contains(lower) ||
            host.tags.any { it.lowercase().contains(lower) }
        }
    }

    fun openAddHostDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = true, hostToEdit = null) }
    }

    fun openEditHostDialog(host: Host) {
        _uiState.update { it.copy(isAddEditDialogOpen = true, hostToEdit = host) }
    }

    fun dismissAddEditDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = false, hostToEdit = null) }
    }

    fun saveHost(
        label: String,
        hostname: String,
        port: Int,
        username: String,
        password: String?,
        authType: AuthType,
        identityId: String?,
        privateKeyId: String?,
        jumpHostId: String?,
        tags: List<String>
    ) {
        viewModelScope.launch {
            val currentEdit = _uiState.value.hostToEdit
            val host = currentEdit?.copy(
                label = label,
                hostname = hostname,
                port = port,
                username = username,
                authType = authType,
                identityId = identityId,
                privateKeyId = privateKeyId,
                jumpHostId = jumpHostId,
                tags = tags
            ) ?: Host(
                id = UUID.randomUUID().toString(),
                label = label,
                hostname = hostname,
                port = port,
                username = username,
                authType = authType,
                identityId = identityId,
                privateKeyId = privateKeyId,
                jumpHostId = jumpHostId,
                tags = tags
            )

            hostRepository.saveHost(host, password)
            dismissAddEditDialog()
        }
    }

    fun deleteHost(hostId: String) {
        viewModelScope.launch {
            sshConnectionManager.disconnect(hostId)
            hostRepository.deleteHost(hostId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val hostRepository: HostRepository,
        private val identityRepository: IdentityRepository,
        private val keyRepository: KeyRepository,
        private val sshConnectionManager: SshConnectionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HostsViewModel(
                hostRepository = hostRepository,
                identityRepository = identityRepository,
                keyRepository = keyRepository,
                sshConnectionManager = sshConnectionManager
            ) as T
        }
    }
}
