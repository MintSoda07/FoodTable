package com.bcu.foodtable.JetpackCompose.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
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
        Log.d(
            "BIO-PROMPT",
            "canBiometricStrong(): result=$r, allow=$PROMPT_ALLOW, sdk=${Build.VERSION.SDK_INT} " +
                    "(0=SUCCESS, 11=NONE_ENROLLED, 12=NO_HARDWARE, 1=HW_UNAVAILABLE)"
        )
        return r == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun ensureKey() {
        Log.d("BIO-KS", "ensureKey(): enter")
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existed = ks.getKey(KEY_ALIAS, null) != null
        Log.d("BIO-KS", "ensureKey(): existed=$existed")
        if (existed) return

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true) // 권장

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d("BIO-KS", "ensureKey(): spec for R+, authParams=0, STRONG|DEVICE_CREDENTIAL")
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            run {
                Log.d("BIO-KS", "ensureKey(): spec for pre-R, validitySeconds=0 (auth every time)")
                builder.setUserAuthenticationValidityDurationSeconds(0) // 매번 인증
            }
        }

        kg.init(builder.build())
        kg.generateKey()
        Log.d("BIO-KS", "ensureKey(): key generated")
    }

    private fun getSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val key = ks.getKey(KEY_ALIAS, null)
        Log.d("BIO-KS", "getSecretKey(): keyExists=${key != null}")
        require(key is SecretKey) { "Secret key missing; call ensureKey() first" }
        return key
    }

    fun getEncryptCipher(): Cipher {
        Log.d("BIO-KS", "getEncryptCipher(): init ENCRYPT with $TRANSFORMATION")
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.ENCRYPT_MODE, getSecretKey())
        Log.d("BIO-KS", "getEncryptCipher(): ready, ivSize=${c.iv?.size ?: -1}")
        return c
    }

    fun getDecryptCipher(iv: ByteArray): Cipher {
        Log.d("BIO-KS", "getDecryptCipher(): init DECRYPT with ivSize=${iv.size}, $TRANSFORMATION")
        val c = Cipher.getInstance(TRANSFORMATION)
        val gcm = GCMParameterSpec(128, iv)
        c.init(Cipher.DECRYPT_MODE, getSecretKey(), gcm)
        Log.d("BIO-KS", "getDecryptCipher(): ready")
        return c
    }

    fun buildPromptInfoForLogin(): BiometricPrompt.PromptInfo {
        Log.d(
            "BIO-PROMPT",
            "buildPromptInfoForLogin(): sdk=${Build.VERSION.SDK_INT}, mode=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "ALLOWED_AUTH" else "DEVICE_CRED_ALLOWED"}"
        )
        val b = BiometricPrompt.PromptInfo.Builder()
            .setTitle("얼굴/지문으로 로그인")
            .setSubtitle("기기 생체/잠금으로 빠른 로그인")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            b.setAllowedAuthenticators(PROMPT_ALLOW)
        } else {
            @Suppress("DEPRECATION")
            b.setDeviceCredentialAllowed(true) // API 29↓ 필수 폴백
        }
        return b.build()
    }
}
