package com.bcu.foodtable.JetpackCompose.biometric

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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

        // ★ 기본 허용 조합을 STRONG | DEVICE_CREDENTIAL 로 통일
        private const val DEFAULT_ALLOW =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

        /**
         * 게이트 액티비티 인텐트 생성
         * - 등록/복호화(토큰 저장/사용) 용도가 아니라, "단순 게이트"일 때만 사용하세요.
         *   (토큰 저장/사용은 반드시 BiometricPrompt + CryptoObject 로!)
         */
        fun createIntent(
            ctx: Context,
            title: String = DEFAULT_TITLE,
            subtitle: String = DEFAULT_SUBTITLE,
            allow: Int = DEFAULT_ALLOW
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
        val allow = intent.getIntExtra("allow", DEFAULT_ALLOW)

        Log.d("BIO-GATE", "onCreate(): title='$title', subtitle='$subtitle', allow=$allow, sdk=${Build.VERSION.SDK_INT}")

        // 단말 상태 점검 (허용 조합과 동일한 기준으로)
        val can = BiometricManager.from(this).canAuthenticate(allow)
        Log.d(
            "BIO-GATE",
            "canAuthenticate(allow=$allow)=$can (0=SUCCESS, 1=HW_UNAVAILABLE, 11=NONE_ENROLLED, 12=NO_HARDWARE)"
        )

        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            Log.w("BIO-GATE", "Device not ready for prompt, finishing. code=$can")
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
                Log.d("BIO-PROMPT", "Gate onSucceeded()")
                setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_SUCCESS, true) })
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w("BIO-PROMPT", "Gate onError(): code=$errorCode, msg=$errString")
                setResult(RESULT_CANCELED, Intent().apply {
                    putExtra(EXTRA_SUCCESS, false)
                    putExtra(EXTRA_ERROR_CODE, errorCode)
                    putExtra(EXTRA_ERROR_MSG, errString.toString())
                })
                finish()
                overridePendingTransition(0, 0)
            }

            override fun onAuthenticationFailed() {
                Log.w("BIO-PROMPT", "Gate onFailed(): will retry by system UI")
                // 시스템 UI가 자체 재시도 처리
            }
        }

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        //  R 이상은 setAllowedAuthenticators, R 미만은 deviceCredentialAllowed 폴백
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d("BIO-PROMPT", "PromptInfo: using setAllowedAuthenticators(allow=$allow)")
            infoBuilder.setAllowedAuthenticators(allow)
        } else {
            @Suppress("DEPRECATION")
            run {
                Log.d("BIO-PROMPT", "PromptInfo: using setDeviceCredentialAllowed(true) [pre-R]")
                infoBuilder.setDeviceCredentialAllowed(true)
            }
        }

        val info = infoBuilder.build()
        Log.d("BIO-PROMPT", "authenticate(): show gate prompt now")
        BiometricPrompt(this, executor, callback).authenticate(info)
    }
}
