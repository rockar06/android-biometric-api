package com.example.androidbiometricsapi.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
private const val CIPHER_TRANSFORMATION =
    "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

data class EncryptedData(
    var cipherText: ByteArray? = null,
    var initializationVector: ByteArray? = null
)

interface CryptographyManager {

    fun getInitializedCipherForEncryption(keyName: String): Cipher

    fun getInitializedCipherForDecryption(keyName: String, initializationVector: ByteArray): Cipher

    fun encryptData(plainText: String, cipher: Cipher): EncryptedData

    fun decryptData(cipherText: ByteArray, cipher: Cipher): String
}

class CryptographyManagerImpl : CryptographyManager {

    override fun getInitializedCipherForEncryption(keyName: String): Cipher {
        val secretKey = getSecretKey(keyName)
        return getCipher().apply { init(Cipher.ENCRYPT_MODE, secretKey) }
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(CIPHER_TRANSFORMATION)
    }

    private fun getSecretKey(keyName: String): SecretKey {
        return getSavedSecretKey(keyName) ?: createSecretKey(keyName)
    }

    private fun getSavedSecretKey(keyName: String): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(keyName, null)?.let { return it as SecretKey } ?: return null
    }

    private fun createSecretKey(keyName: String): SecretKey {
        val keyGenerator = getKeyGenerator(getKeyGenParamSpec(keyName))
        return keyGenerator.generateKey()
    }

    private fun getKeyGenParamSpec(keyName: String): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
        }.build()
    }

    private fun getKeyGenerator(keyGenParams: KeyGenParameterSpec) = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        ANDROID_KEYSTORE
    ).apply { init(keyGenParams) }

    override fun getInitializedCipherForDecryption(
        keyName: String,
        initializationVector: ByteArray
    ): Cipher {
        val secretKey = getSecretKey(keyName)
        return getCipher().apply {
            init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(initializationVector))
        }
    }

    override fun encryptData(plainText: String, cipher: Cipher): EncryptedData {
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedData(cipherText, cipher.iv)
    }

    override fun decryptData(cipherText: ByteArray, cipher: Cipher): String {
        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }
}
