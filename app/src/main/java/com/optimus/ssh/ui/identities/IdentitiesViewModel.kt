package com.optimus.ssh.ui.identities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.optimus.ssh.data.repository.IdentityRepository
import com.optimus.ssh.data.repository.KeyRepository
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.Identity
import com.optimus.ssh.model.KeyRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class IdentitiesUiState(
    val identities: List<Identity> = emptyList(),
    val availableKeys: List<KeyRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isAddEditDialogOpen: Boolean = false,
    val identityToEdit: Identity? = null,
    val message: String? = null
)

class IdentitiesViewModel(
    private val identityRepository: IdentityRepository,
    private val keyRepository: KeyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdentitiesUiState(isLoading = true))
    val uiState: StateFlow<IdentitiesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            identityRepository.getIdentities().collect { idList ->
                _uiState.update { it.copy(identities = idList, isLoading = false) }
            }
        }
        viewModelScope.launch {
            keyRepository.getKeys().collect { keyList ->
                _uiState.update { it.copy(availableKeys = keyList) }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = true, identityToEdit = null) }
    }

    fun openEditDialog(identity: Identity) {
        _uiState.update { it.copy(isAddEditDialogOpen = true, identityToEdit = identity) }
    }

    fun dismissAddEditDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = false, identityToEdit = null) }
    }

    fun saveIdentity(
        label: String,
        username: String,
        authType: AuthType,
        password: String?,
        privateKeyId: String?
    ) {
        viewModelScope.launch {
            try {
                val currentEdit = _uiState.value.identityToEdit
                val identity = currentEdit?.copy(
                    label = label,
                    username = username,
                    authType = authType,
                    privateKeyId = privateKeyId
                ) ?: Identity(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    username = username,
                    authType = authType,
                    privateKeyId = privateKeyId,
                    createdAt = System.currentTimeMillis()
                )

                identityRepository.saveIdentity(identity, password)
                dismissAddEditDialog()
                _uiState.update {
                    it.copy(message = if (currentEdit == null) "Identity '$label' created" else "Identity '$label' updated")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to save identity: ${e.message}") }
            }
        }
    }

    fun deleteIdentity(id: String) {
        viewModelScope.launch {
            try {
                identityRepository.deleteIdentity(id)
                _uiState.update { it.copy(message = "Identity deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to delete identity: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    class Factory(
        private val identityRepository: IdentityRepository,
        private val keyRepository: KeyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentitiesViewModel(identityRepository, keyRepository) as T
        }
    }
}
