package com.optimus.ssh.security

import com.optimus.ssh.model.KeyAlgorithm
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.security.Security

class SshKeyGeneratorTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUp() {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    @Test
    fun testGenerateEd25519Key() {
        val keyPair = SshKeyGenerator.generateKey(KeyAlgorithm.ED25519, "test-user@optimus")

        assertEquals(KeyAlgorithm.ED25519, keyPair.algorithm)
        assertTrue("Public key must start with ssh-ed25519", keyPair.publicKeyOpenSsh.startsWith("ssh-ed25519 "))
        assertTrue("Public key must end with comment", keyPair.publicKeyOpenSsh.endsWith(" test-user@optimus"))
        assertTrue("Fingerprint must start with SHA256:", keyPair.fingerprint.startsWith("SHA256:"))
        assertTrue("Private key must contain PEM header", keyPair.privateKeyPem.contains("-----BEGIN ED25519 PRIVATE KEY-----"))
        assertTrue("Private key must contain PEM footer", keyPair.privateKeyPem.contains("-----END ED25519 PRIVATE KEY-----"))
    }

    @Test
    fun testGenerateRsa2048Key() {
        val keyPair = SshKeyGenerator.generateKey(KeyAlgorithm.RSA_2048, "rsa-test")

        assertEquals(KeyAlgorithm.RSA_2048, keyPair.algorithm)
        assertTrue("Public key must start with ssh-rsa", keyPair.publicKeyOpenSsh.startsWith("ssh-rsa "))
        assertTrue("Public key must end with comment", keyPair.publicKeyOpenSsh.endsWith(" rsa-test"))
        assertTrue("Fingerprint must start with SHA256:", keyPair.fingerprint.startsWith("SHA256:"))
        assertTrue("Private key must contain PEM header", keyPair.privateKeyPem.contains("-----BEGIN RSA PRIVATE KEY-----"))
    }

    @Test
    fun testGenerateEcdsa256Key() {
        val keyPair = SshKeyGenerator.generateKey(KeyAlgorithm.ECDSA_256, "ecdsa-test")

        assertEquals(KeyAlgorithm.ECDSA_256, keyPair.algorithm)
        assertTrue("Public key must start with ecdsa-sha2-nistp256", keyPair.publicKeyOpenSsh.startsWith("ecdsa-sha2-nistp256 "))
        assertTrue("Fingerprint must start with SHA256:", keyPair.fingerprint.startsWith("SHA256:"))
        assertTrue("Private key must contain PEM header", keyPair.privateKeyPem.contains("-----BEGIN EC PRIVATE KEY-----"))
    }
}
