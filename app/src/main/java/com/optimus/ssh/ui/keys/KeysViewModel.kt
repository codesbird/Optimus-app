package com.optimus.ssh.ui.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.optimus.ssh.data.repository.KeyRepository
import com.optimus.ssh.model.KeyAlgorithm
import com.optimus.ssh.model.KeyRecord
import com.optimus.ssh.security.SshKeyGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KeysUiState(
    val keys: List<KeyRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerateDialogOpen: Boolean = false,
    val isImportDialogOpen: Boolean = false,
    val message: String? = null
)

class KeysViewModel(
    private val keyRepository: KeyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeysUiState(isLoading = true))
    val uiState: StateFlow<KeysUiState> = _uiState.asStateFlow()

    init {
        loadKeys()
    }

    private fun loadKeys() {
        viewModelScope.launch {
            keyRepository.getKeys().collect { list ->
                _uiState.update { it.copy(keys = list, isLoading = false) }
            }
        }
    }

    fun openGenerateDialog() {
        _uiState.update { it.copy(isGenerateDialogOpen = true) }
    }

    fun dismissGenerateDialog() {
        _uiState.update { it.copy(isGenerateDialogOpen = false) }
    }

    fun openImportDialog() {
        _uiState.update { it.copy(isImportDialogOpen = true) }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(isImportDialogOpen = false) }
    }

    fun generateKey(label: String, algorithm: KeyAlgorithm, comment: String) {
        viewModelScope.launch {
            try {
                val keyPair = SshKeyGenerator.generateKey(algorithm, comment.ifBlank { label })
                keyRepository.saveGeneratedKey(label, keyPair)
                dismissGenerateDialog()
                _uiState.update { it.copy(message = "SSH Key '$label' generated successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Key generation failed: ${e.message}") }
            }
        }
    }

    fun importKey(label: String, privateKeyPem: String, publicKeyOpenSsh: String, algorithm: KeyAlgorithm) {
        viewModelScope.launch {
            try {
                keyRepository.importKey(label, privateKeyPem, publicKeyOpenSsh, algorithm)
                dismissImportDialog()
                _uiState.update { it.copy(message = "Key '$label' imported successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Key import failed: ${e.message}") }
            }
        }
    }

    fun deleteKey(id: String) {
        viewModelScope.launch {
            keyRepository.deleteKey(id)
            _uiState.update { it.copy(message = "Key deleted") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    class Factory(private val keyRepository: KeyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KeysViewModel(keyRepository) as T
        }
    }
}
