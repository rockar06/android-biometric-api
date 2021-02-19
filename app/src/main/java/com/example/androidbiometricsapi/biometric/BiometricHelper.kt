package com.example.androidbiometricsapi.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import javax.crypto.Cipher

interface BiometricHelper {

    var canEnrollBiometrics: Boolean

    fun authenticate(cipher: Cipher? = null)

    fun getBiometricsAvailability(): String
}

class BiometricHelperImpl(
    activity: FragmentActivity,
    callback: BiometricPrompt.AuthenticationCallback
): BiometricHelper {

    private val biometricManager = BiometricManager.from(activity)
    private val executor: Executor by lazy { ContextCompat.getMainExecutor(activity) }
    private val promptInfo: BiometricPrompt.PromptInfo by lazy { getBiometricPromptInfo() }
    private val biometricPrompt: BiometricPrompt by lazy {
        setupBiometricPrompt(activity, callback)
    }
    override var canEnrollBiometrics = false

    private fun setupBiometricPrompt(
        activity: FragmentActivity,
        callback: BiometricPrompt.AuthenticationCallback
    ): BiometricPrompt {
        return BiometricPrompt(activity, executor, callback)
    }

    private fun getBiometricPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Playground")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()
    }

    override fun authenticate(cipher: Cipher?) {
        cipher?.let {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(it))
        } ?: biometricPrompt.authenticate(promptInfo)
    }

    override fun getBiometricsAvailability() = when (biometricManager.canAuthenticate()) {
        BiometricManager.BIOMETRIC_SUCCESS -> "App can authenticate using biometrics."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            "No biometric features available on this device."
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            "Biometric features are currently unavailable."
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            canEnrollBiometrics = true
            "Biometrics not enrolled yet"
        }
        else -> throw IllegalStateException("There is no param matching biometrics state")
    }
}
