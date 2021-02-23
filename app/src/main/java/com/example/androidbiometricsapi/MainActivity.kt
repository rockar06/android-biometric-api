package com.example.androidbiometricsapi

import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import com.example.androidbiometricsapi.biometric.*
import com.example.androidbiometricsapi.crypto.CryptographyManagerImpl
import com.example.androidbiometricsapi.crypto.EncryptedData
import com.example.androidbiometricsapi.databinding.ActivityMainBinding

private const val REQUEST_CODE = 1
private const val EMPTY_STRING = ""

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var biometricType: BiometricType = None
    private val cryptographyManager = CryptographyManagerImpl()
    private lateinit var cipherText: ByteArray
    private lateinit var initializationVector: ByteArray
    private val biometricHelper: BiometricHelper by lazy {
        BiometricHelperImpl(this, getBiometricCallback())
    }

    private fun getBiometricCallback(): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    this@MainActivity,
                    "Authentication error: $errString", Toast.LENGTH_SHORT
                )
                    .show()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(
                    this@MainActivity,
                    "Authentication succeeded!", Toast.LENGTH_SHORT
                )
                    .show()
                processCryptoOperation(result.cryptoObject)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    this@MainActivity, "Authentication failed",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun processCryptoOperation(cryptoObject: BiometricPrompt.CryptoObject?) {
        val data = when (biometricType) {
            EncryptText -> getEncryptedText(cryptoObject)
            DecryptText -> getDecryptedText(cryptoObject)
            None -> EMPTY_STRING
        }
        binding.contentMain.layoutCrypto.tvEncrypted.text = data
    }

    private fun getEncryptedText(cryptoObject: BiometricPrompt.CryptoObject?): String {
        val text = binding.contentMain.layoutCrypto.etMessage.text.toString()
        val encryptedData = cryptoObject?.cipher?.let {
            return@let cryptographyManager.encryptData(text, it)
        } ?: EncryptedData()
        cipherText = encryptedData.cipherText ?: ByteArray(0)
        initializationVector = encryptedData.initializationVector ?: ByteArray(0)
        return String(cipherText, Charsets.UTF_8)
    }

    private fun getDecryptedText(cryptoObject: BiometricPrompt.CryptoObject?): String {
        return cryptoObject?.cipher?.let {
            return@let cryptographyManager.decryptData(cipherText, it)
        } ?: EMPTY_STRING
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupViews()
    }

    private fun setupViews() {
        setupMessageTv()
        setupEnrollButton()
        setupCryptoLayout()
        setupFabButton()
    }

    private fun setupMessageTv() {
        binding.contentMain.layoutBiometric.tvMessage.text = biometricHelper.getBiometricsAvailability()
    }

    private fun setupEnrollButton() {
        binding.contentMain.layoutBiometric.btnEnrollBiometrics.visibility =
            if (biometricHelper.canEnrollBiometrics) View.VISIBLE else View.GONE
        binding.contentMain.layoutBiometric.btnEnrollBiometrics.setOnClickListener { launchEnrollmentBiometricsView() }
    }

    private fun launchEnrollmentBiometricsView() {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        }
        startActivityForResult(enrollIntent, REQUEST_CODE)
        finish()
    }

    private fun setupCryptoLayout() {
        binding.contentMain.layoutCrypto.cryptoLayoutView.visibility =
            if (biometricHelper.canEnrollBiometrics) View.GONE else View.VISIBLE
        binding.contentMain.layoutCrypto.btnEncrypt.setOnClickListener { encryptText() }
        binding.contentMain.layoutCrypto.btnDecrypt.setOnClickListener { decryptData() }
    }

    private fun encryptText() {
        biometricType = EncryptText
        val cipher =
            cryptographyManager.getInitializedCipherForEncryption(BuildConfig.secretKeyName)
        biometricHelper.authenticate(cipher)
    }

    private fun decryptData() {
        biometricType = DecryptText
        val cipher =
            cryptographyManager.getInitializedCipherForDecryption(
                BuildConfig.secretKeyName,
                initializationVector
            )
        biometricHelper.authenticate(cipher)
    }

    private fun setupFabButton() {
        binding.fab.setOnClickListener {
            biometricType = None
            biometricHelper.authenticate()
        }
    }
}
