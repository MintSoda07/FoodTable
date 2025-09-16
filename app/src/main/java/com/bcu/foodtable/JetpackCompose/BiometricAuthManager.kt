package com.bcu.foodtable.JetpackCompose

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

object BiometricAuthManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "bft.bio.aes"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    // 프롬프트 허용(키스토어 조건과 일치: STRONG 또는 기기 잠금)
    private val PROMPT_ALLOW = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canBiometricStrong(ctx: Context): Boolean {
        val r = BiometricManager.from(ctx).canAuthenticate(PROMPT_ALLOW)
        return r == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun ensureKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.getKey(KEY_ALIAS, null) != null) return

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)

        // ⚠️ 여기는 KeyProperties 상수만 허용됩니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        }

        kg.init(builder.build())
        kg.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val key = ks.getKey(KEY_ALIAS, null)
        require(key is SecretKey) { "Secret key missing; call ensureKey() first" }
        return key
    }

    fun getEncryptCipher(): Cipher {
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return c
    }

    fun getDecryptCipher(iv: ByteArray): Cipher {
        val c = Cipher.getInstance(TRANSFORMATION)
        val gcm = GCMParameterSpec(128, iv)
        c.init(Cipher.DECRYPT_MODE, getSecretKey(), gcm)
        return c
    }

    fun buildPromptInfoForLogin(): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("얼굴/지문으로 로그인")
            .setSubtitle("기기 생체/잠금으로 빠른 로그인")
            .setAllowedAuthenticators(PROMPT_ALLOW) // STRONG | DEVICE_CREDENTIAL
            .build()
}
