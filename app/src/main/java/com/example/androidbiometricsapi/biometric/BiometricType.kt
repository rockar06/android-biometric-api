package com.example.androidbiometricsapi.biometric

sealed class BiometricType
object EncryptText : BiometricType()
object DecryptText : BiometricType()
object None : BiometricType()
