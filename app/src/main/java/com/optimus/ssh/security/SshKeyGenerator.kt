package com.optimus.ssh.security

import java.util.Base64
import com.optimus.ssh.model.KeyAlgorithm
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

data class GeneratedSshKeyPair(
    val algorithm: KeyAlgorithm,
    val publicKeyOpenSsh: String,
    val privateKeyPem: String,
    val fingerprint: String
)

object SshKeyGenerator {

    fun generateKey(
        algorithm: KeyAlgorithm,
        comment: String = "optimus-key"
    ): GeneratedSshKeyPair {
        return when (algorithm) {
            KeyAlgorithm.ED25519 -> generateEd25519(comment)
            KeyAlgorithm.ECDSA_256 -> generateEcdsa("secp256r1", KeyAlgorithm.ECDSA_256, comment)
            KeyAlgorithm.ECDSA_384 -> generateEcdsa("secp384r1", KeyAlgorithm.ECDSA_384, comment)
            KeyAlgorithm.ECDSA_521 -> generateEcdsa("secp521r1", KeyAlgorithm.ECDSA_521, comment)
            KeyAlgorithm.RSA_2048 -> generateRsa(2048, KeyAlgorithm.RSA_2048, comment)
            KeyAlgorithm.RSA_4096 -> generateRsa(4096, KeyAlgorithm.RSA_4096, comment)
        }
    }

    private fun generateEd25519(comment: String): GeneratedSshKeyPair {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()

        val priv = keyPair.private as Ed25519PrivateKeyParameters
        val pub = keyPair.public as Ed25519PublicKeyParameters

        // OpenSSH wire format for Ed25519 public key
        val pubBlob = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                writeSshString(dos, "ssh-ed25519")
                writeSshBlob(dos, pub.encoded)
            }
            baos.toByteArray()
        }
        val pubBase64 = Base64.getEncoder().encodeToString(pubBlob)
        val openSshPub = "ssh-ed25519 $pubBase64 $comment"

        // PEM private key encoding (PKCS#8 with Ed25519)
        val privPem = encodePrivateKeyPem("ED25519 PRIVATE KEY", priv.encoded)
        val fingerprint = calculateFingerprint(pubBlob)

        return GeneratedSshKeyPair(
            algorithm = KeyAlgorithm.ED25519,
            publicKeyOpenSsh = openSshPub,
            privateKeyPem = privPem,
            fingerprint = fingerprint
        )
    }

    private fun generateRsa(keySize: Int, algo: KeyAlgorithm, comment: String): GeneratedSshKeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(keySize, SecureRandom())
        val pair = kpg.generateKeyPair()

        val rsaPub = pair.public as RSAPublicKey
        val pubBlob = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                writeSshString(dos, "ssh-rsa")
                writeSshMpint(dos, rsaPub.publicExponent)
                writeSshMpint(dos, rsaPub.modulus)
            }
            baos.toByteArray()
        }
        val pubBase64 = Base64.getEncoder().encodeToString(pubBlob)
        val openSshPub = "ssh-rsa $pubBase64 $comment"

        val privPem = encodePrivateKeyPem("RSA PRIVATE KEY", pair.private.encoded)
        val fingerprint = calculateFingerprint(pubBlob)

        return GeneratedSshKeyPair(
            algorithm = algo,
            publicKeyOpenSsh = openSshPub,
            privateKeyPem = privPem,
            fingerprint = fingerprint
        )
    }

    private fun generateEcdsa(curveName: String, algo: KeyAlgorithm, comment: String): GeneratedSshKeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(curveName), SecureRandom())
        val pair = kpg.generateKeyPair()

        val curveIdentifier = when (curveName) {
            "secp256r1" -> "nistp256"
            "secp384r1" -> "nistp384"
            else -> "nistp521"
        }
        val sshKeyType = "ecdsa-sha2-$curveIdentifier"

        // EC Point from public key X.509
        val pubEncoded = pair.public.encoded
        val pubBlob = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                writeSshString(dos, sshKeyType)
                writeSshString(dos, curveIdentifier)
                writeSshBlob(dos, pubEncoded)
            }
            baos.toByteArray()
        }
        val pubBase64 = Base64.getEncoder().encodeToString(pubBlob)
        val openSshPub = "$sshKeyType $pubBase64 $comment"

        val privPem = encodePrivateKeyPem("EC PRIVATE KEY", pair.private.encoded)
        val fingerprint = calculateFingerprint(pubBlob)

        return GeneratedSshKeyPair(
            algorithm = algo,
            publicKeyOpenSsh = openSshPub,
            privateKeyPem = privPem,
            fingerprint = fingerprint
        )
    }

    fun calculateFingerprint(publicKeyBlob: ByteArray): String {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(publicKeyBlob)
        val b64 = Base64.getEncoder().encodeToString(sha256).removeSuffix("=")
        return "SHA256:$b64"
    }

    private fun encodePrivateKeyPem(type: String, keyBytes: ByteArray): String {
        val sw = StringWriter()
        JcaPEMWriter(sw).use { writer ->
            writer.writeObject(PemObject(type, keyBytes))
        }
        return sw.toString()
    }

    private fun writeSshString(dos: DataOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.US_ASCII)
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun writeSshBlob(dos: DataOutputStream, blob: ByteArray) {
        dos.writeInt(blob.size)
        dos.write(blob)
    }

    private fun writeSshMpint(dos: DataOutputStream, bi: BigInteger) {
        val bytes = bi.toByteArray()
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }
}
