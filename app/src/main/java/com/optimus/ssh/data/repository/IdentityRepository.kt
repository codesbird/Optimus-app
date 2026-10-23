package com.optimus.ssh.data.repository

import com.optimus.ssh.data.db.dao.IdentityDao
import com.optimus.ssh.data.db.entity.IdentityEntity
import com.optimus.ssh.model.Identity
import com.optimus.ssh.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class IdentityRepository(
    private val identityDao: IdentityDao,
    private val keystoreManager: KeystoreManager
) {

    fun getIdentities(): Flow<List<Identity>> = identityDao.getAllIdentities().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getIdentityById(id: String): Identity? {
        return identityDao.getIdentityById(id)?.toDomain()
    }

    suspend fun saveIdentity(identity: Identity, plaintextPassword: String? = null): Identity {
        val existing = identityDao.getIdentityById(identity.id)
        val encryptedPassword = if (!plaintextPassword.isNullOrEmpty()) {
            keystoreManager.encrypt(plaintextPassword)
        } else {
            existing?.encryptedPassword
        }

        val id = if (identity.id.isBlank()) UUID.randomUUID().toString() else identity.id
        val entity = IdentityEntity(
            id = id,
            label = identity.label,
            username = identity.username,
            authType = identity.authType,
            encryptedPassword = encryptedPassword,
            privateKeyId = identity.privateKeyId,
            createdAt = identity.createdAt
        )
        identityDao.insertIdentity(entity)
        return entity.toDomain()
    }

    suspend fun getDecryptedPassword(identityId: String): String? {
        val entity = identityDao.getIdentityById(identityId) ?: return null
        val encrypted = entity.encryptedPassword ?: return null
        return try {
            keystoreManager.decrypt(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteIdentity(id: String) {
        identityDao.getIdentityById(id)?.let { identityDao.deleteIdentity(it) }
    }

    private fun IdentityEntity.toDomain() = Identity(
        id = id,
        label = label,
        username = username,
        authType = authType,
        privateKeyId = privateKeyId,
        createdAt = createdAt
    )
}
