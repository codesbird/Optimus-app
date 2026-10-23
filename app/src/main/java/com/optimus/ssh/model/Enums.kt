package com.optimus.ssh.model

enum class AuthType {
    PASSWORD,
    KEY,
    PASSWORD_AND_KEY,
    AGENT
}

enum class HostStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
    ERROR
}

enum class TunnelType {
    LOCAL,   // -L local:remote_host:remote_port
    REMOTE,  // -R remote:local_host:local_port
    DYNAMIC  // -D SOCKS5
}

enum class KeyAlgorithm {
    ED25519,
    ECDSA_256,
    ECDSA_384,
    ECDSA_521,
    RSA_2048,
    RSA_4096
}
