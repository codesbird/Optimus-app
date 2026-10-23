package com.optimus.ssh.data.repository

import com.optimus.ssh.data.db.dao.HostDao
import com.optimus.ssh.data.db.entity.HostEntity
import com.optimus.ssh.model.Host
import com.optimus.ssh.model.HostStatus
import com.optimus.ssh.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HostRepository(
    private val hostDao: HostDao,
    private val keystoreManager: KeystoreManager
) {

    fun getHosts(): Flow<List<Host>> = hostDao.getAllHosts().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getHostById(id: String): Host? {
        return hostDao.getHostById(id)?.toDomain()
    }

    suspend fun saveHost(host: Host, plaintextPassword: String? = null) {
        val existing = hostDao.getHostById(host.id)
        val encryptedPassword = if (!plaintextPassword.isNullOrEmpty()) {
            keystoreManager.encrypt(plaintextPassword)
        } else {
            existing?.encryptedPassword
        }

        val entity = HostEntity(
            id = host.id,
            label = host.label,
            hostname = host.hostname,
            port = host.port,
            username = host.username,
            authType = host.authType,
            encryptedPassword = encryptedPassword,
            identityId = host.identityId,
            privateKeyId = host.privateKeyId,
            groupId = host.groupId,
            jumpHostId = host.jumpHostId,
            terminalTheme = host.terminalTheme,
            keepAliveIntervalSeconds = host.keepAliveIntervalSeconds,
            startupSnippetId = host.startupSnippetId,
            tags = host.tags.joinToString(","),
            status = host.status,
            lastConnectedAt = host.lastConnectedAt,
            createdAt = host.createdAt
        )
        hostDao.insertHost(entity)
    }

    suspend fun getDecryptedPassword(hostId: String): String? {
        val entity = hostDao.getHostById(hostId) ?: return null
        val encrypted = entity.encryptedPassword ?: return null
        return try {
            keystoreManager.decrypt(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateHostStatus(hostId: String, status: HostStatus) {
        hostDao.updateStatus(hostId, status)
    }

    suspend fun updateLastConnected(hostId: String, timestamp: Long = System.currentTimeMillis()) {
        hostDao.updateLastConnected(hostId, timestamp)
    }

    suspend fun deleteHost(hostId: String) {
        hostDao.deleteHostById(hostId)
    }

    private fun HostEntity.toDomain(): Host {
        val tagList = if (tags.isNotEmpty()) tags.split(",").map { it.trim() } else emptyList()
        return Host(
            id = id,
            label = label,
            hostname = hostname,
            port = port,
            username = username,
            authType = authType,
            identityId = identityId,
            privateKeyId = privateKeyId,
            groupId = groupId,
            jumpHostId = jumpHostId,
            terminalTheme = terminalTheme,
            keepAliveIntervalSeconds = keepAliveIntervalSeconds,
            startupSnippetId = startupSnippetId,
            tags = tagList,
            status = status,
            lastConnectedAt = lastConnectedAt,
            createdAt = createdAt
        )
    }
}
