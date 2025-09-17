package com.bcu.foodtable.JetpackCompose.biometric

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricGateActivity : FragmentActivity() {

    companion object {
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_ERROR_CODE = "error_code"
        const val EXTRA_ERROR_MSG = "error_msg"

        private const val DEFAULT_TITLE = "인증 필요"
        private const val DEFAULT_SUBTITLE = "얼굴/지문 또는 기기 잠금으로 진행하세요"

        fun createIntent(
            ctx: Context,
            title: String = DEFAULT_TITLE,
            subtitle: String = DEFAULT_SUBTITLE,
            allow: Int = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ): Intent = Intent(ctx, BiometricGateActivity::class.java).apply {
            putExtra("title", title)
            putExtra("subtitle", subtitle)
            putExtra("allow", allow)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title") ?: DEFAULT_TITLE
        val subtitle = intent.getStringExtra("subtitle") ?: DEFAULT_SUBTITLE
        val allow = intent.getIntExtra(
            "allow",
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        val can = BiometricManager.from(this).canAuthenticate(allow)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra(EXTRA_SUCCESS, false)
                putExtra(EXTRA_ERROR_CODE, can)
                putExtra(EXTRA_ERROR_MSG, "canAuthenticate=$can")
            })
            finish()
            overridePendingTransition(0, 0)
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_SUCCESS, true) })
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                setResult(RESULT_CANCELED, Intent().apply {
                    putExtra(EXTRA_SUCCESS, false)
                    putExtra(EXTRA_ERROR_CODE, errorCode)
                    putExtra(EXTRA_ERROR_MSG, errString.toString())
                })
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onAuthenticationFailed() {
                // 시스템 UI가 자체 재시도 유도
            }
        }

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            infoBuilder.setAllowedAuthenticators(allow)
        } else {
            @Suppress("DEPRECATION")
            infoBuilder.setDeviceCredentialAllowed(true)
        }

        BiometricPrompt(this, executor, callback).authenticate(infoBuilder.build())
    }
}