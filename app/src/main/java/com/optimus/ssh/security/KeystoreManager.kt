package com.optimus.ssh.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "optimus_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        getOrCreateMasterKey()
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plaintext data with AES-256 GCM using hardware-backed master key.
     * Output format: Base64(IV + CipherText)
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts Base64(IV + CipherText) with AES-256 GCM using hardware-backed master key.
     */
    fun decrypt(encodedCiphertext: String): String {
        if (encodedCiphertext.isEmpty()) return ""
        val combined = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH) throw IllegalArgumentException("Invalid encrypted payload")

        val iv = ByteArray(GCM_IV_LENGTH)
        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Retrieves or generates a secure random 256-bit passphrase for SQLCipher database encryption.
     */
    fun getDatabasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences("optimus_sec_prefs", Context.MODE_PRIVATE)
        val encryptedPass = prefs.getString("encrypted_db_pass", null)
        return if (encryptedPass != null) {
            val decrypted = decrypt(encryptedPass)
            Base64.decode(decrypted, Base64.NO_WRAP)
        } else {
            val randomBytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(randomBytes)
            val passString = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
            val encrypted = encrypt(passString)
            prefs.edit().putString("encrypted_db_pass", encrypted).apply()
            randomBytes
        }
    }
}
