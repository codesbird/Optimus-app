package com.optimus.ssh.data.repository

import com.optimus.ssh.data.db.dao.KeyDao
import com.optimus.ssh.data.db.entity.KeyEntity
import com.optimus.ssh.model.KeyAlgorithm
import com.optimus.ssh.model.KeyRecord
import com.optimus.ssh.security.GeneratedSshKeyPair
import com.optimus.ssh.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class KeyRepository(
    private val keyDao: KeyDao,
    private val keystoreManager: KeystoreManager
) {

    fun getKeys(): Flow<List<KeyRecord>> = keyDao.getAllKeys().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getKeyById(id: String): KeyRecord? {
        return keyDao.getKeyById(id)?.toDomain()
    }

    suspend fun getDecryptedPrivateKey(id: String): String? {
        val entity = keyDao.getKeyById(id) ?: return null
        return try {
            keystoreManager.decrypt(entity.encryptedPrivateKey)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveGeneratedKey(label: String, keyPair: GeneratedSshKeyPair): KeyRecord {
        val encryptedPrivateKey = keystoreManager.encrypt(keyPair.privateKeyPem)
        val id = UUID.randomUUID().toString()

        val entity = KeyEntity(
            id = id,
            label = label,
            algorithm = keyPair.algorithm,
            publicKey = keyPair.publicKeyOpenSsh,
            encryptedPrivateKey = encryptedPrivateKey,
            fingerprint = keyPair.fingerprint,
            isEncrypted = false
        )
        keyDao.insertKey(entity)
        return entity.toDomain()
    }

    suspend fun importKey(
        label: String,
        privateKeyPem: String,
        publicKeyOpenSsh: String = "",
        algorithm: KeyAlgorithm = KeyAlgorithm.ED25519
    ): KeyRecord {
        val encryptedPrivateKey = keystoreManager.encrypt(privateKeyPem)
        val id = UUID.randomUUID().toString()
        val fingerprint = if (publicKeyOpenSsh.isNotEmpty()) {
            "SHA256:imported"
        } else "SHA256:imported"

        val entity = KeyEntity(
            id = id,
            label = label,
            algorithm = algorithm,
            publicKey = publicKeyOpenSsh,
            encryptedPrivateKey = encryptedPrivateKey,
            fingerprint = fingerprint,
            isEncrypted = false
        )
        keyDao.insertKey(entity)
        return entity.toDomain()
    }

    suspend fun deleteKey(id: String) {
        keyDao.getKeyById(id)?.let { keyDao.deleteKey(it) }
    }

    private fun KeyEntity.toDomain() = KeyRecord(
        id = id,
        label = label,
        algorithm = algorithm,
        publicKey = publicKey,
        fingerprint = fingerprint,
        isEncrypted = isEncrypted,
        createdAt = createdAt
    )
}
