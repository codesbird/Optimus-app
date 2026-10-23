package com.optimus.ssh.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.optimus.ssh.model.AuthType
import com.optimus.ssh.model.HostStatus
import com.optimus.ssh.model.KeyAlgorithm
import com.optimus.ssh.model.TunnelType

@Entity(tableName = "hosts")
data class HostEntity(
    @PrimaryKey val id: String,
    val label: String,
    val hostname: String,
    val port: Int = 22,
    val username: String = "root",
    val authType: AuthType = AuthType.PASSWORD,
    val encryptedPassword: String? = null,
    val identityId: String? = null,
    val privateKeyId: String? = null,
    val groupId: String? = null,
    val jumpHostId: String? = null,
    val terminalTheme: String = "Dracula",
    val keepAliveIntervalSeconds: Int = 30,
    val startupSnippetId: String? = null,
    val tags: String = "", // Comma-separated
    val status: HostStatus = HostStatus.DISCONNECTED,
    val lastConnectedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "identities")
data class IdentityEntity(
    @PrimaryKey val id: String,
    val label: String,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val encryptedPassword: String? = null,
    val privateKeyId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String = "#6200EE",
    val parentGroupId: String? = null
)

@Entity(tableName = "keys")
data class KeyEntity(
    @PrimaryKey val id: String,
    val label: String,
    val algorithm: KeyAlgorithm,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val fingerprint: String,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "port_forwards")
data class PortForwardEntity(
    @PrimaryKey val id: String,
    val hostId: String,
    val label: String,
    val type: TunnelType = TunnelType.LOCAL,
    val localPort: Int,
    val remoteHost: String = "127.0.0.1",
    val remotePort: Int,
    val isEnabled: Boolean = false
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val script: String,
    val tags: String = ""
)
