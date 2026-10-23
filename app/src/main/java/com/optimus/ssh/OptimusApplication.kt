package com.optimus.ssh

import android.app.Application
import com.optimus.ssh.core.network.SshConnectionManager
import com.optimus.ssh.data.db.OptimusDatabase
import com.optimus.ssh.data.repository.HostRepository
import com.optimus.ssh.security.BiometricHelper
import com.optimus.ssh.security.KeystoreManager

class OptimusApplication : Application() {

    lateinit var keystoreManager: KeystoreManager
        private set

    lateinit var database: OptimusDatabase
        private set

    lateinit var hostRepository: HostRepository
        private set

    lateinit var keyRepository: com.optimus.ssh.data.repository.KeyRepository
        private set

    lateinit var identityRepository: com.optimus.ssh.data.repository.IdentityRepository
        private set

    lateinit var sshConnectionManager: SshConnectionManager
        private set

    lateinit var biometricHelper: BiometricHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize BouncyCastle Security Provider at position 1
        java.security.Security.removeProvider("BC")
        java.security.Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 1)

        // Initialize SQLCipher native binaries
        System.loadLibrary("sqlcipher")

        keystoreManager = KeystoreManager(this)
        database = OptimusDatabase.getInstance(this, keystoreManager)
        hostRepository = HostRepository(database.hostDao(), keystoreManager)
        keyRepository = com.optimus.ssh.data.repository.KeyRepository(database.keyDao(), keystoreManager)
        identityRepository = com.optimus.ssh.data.repository.IdentityRepository(database.identityDao(), keystoreManager)
        sshConnectionManager = SshConnectionManager()
        biometricHelper = BiometricHelper(this)
    }

    companion object {
        lateinit var instance: OptimusApplication
            private set
    }
}
