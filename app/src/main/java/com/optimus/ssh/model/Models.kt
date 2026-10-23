package com.optimus.ssh.model

data class Host(
    val id: String,
    val label: String,
    val hostname: String,
    val port: Int = 22,
    val username: String = "root",
    val authType: AuthType = AuthType.PASSWORD,
    val identityId: String? = null,
    val privateKeyId: String? = null,
    val groupId: String? = null,
    val jumpHostId: String? = null,
    val terminalTheme: String = "Dracula",
    val keepAliveIntervalSeconds: Int = 30,
    val startupSnippetId: String? = null,
    val tags: List<String> = emptyList(),
    val status: HostStatus = HostStatus.DISCONNECTED,
    val lastConnectedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Identity(
    val id: String,
    val label: String,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val privateKeyId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class HostGroup(
    val id: String,
    val name: String,
    val colorHex: String = "#6200EE",
    val parentGroupId: String? = null
)

data class KeyRecord(
    val id: String,
    val label: String,
    val algorithm: KeyAlgorithm,
    val publicKey: String,
    val fingerprint: String,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PortForwardRule(
    val id: String,
    val hostId: String,
    val label: String,
    val type: TunnelType = TunnelType.LOCAL,
    val localPort: Int,
    val remoteHost: String = "127.0.0.1",
    val remotePort: Int,
    val isEnabled: Boolean = false
)

data class Snippet(
    val id: String,
    val title: String,
    val script: String,
    val tags: List<String> = emptyList()
)
