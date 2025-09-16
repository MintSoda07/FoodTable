// LoginViewModel.kt
package com.bcu.foodtable.JetpackCompose

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class LoginViewModel : ViewModel() {

    sealed class Event {
        data class ShowSnack(val msg: String): Event()
        data class BiometricReady(val available: Boolean, val hasStored: Boolean): Event()
        object BiometricLoginSuccess: Event()
    }
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun refreshBiometricState(activity: android.app.Activity) {
        val ctx = activity.applicationContext
        val available = BiometricAuthManager.canBiometricStrong(ctx)
        val hasStored = SecretStore.loadEncryptedToken(ctx) != null
        viewModelScope.launch { _events.send(Event.BiometricReady(available, hasStored)) }
    }

    /**  최초 1회 등록: ENCRYPT 모드로 프롬프트 띄워서 토큰을 암호화해 저장 */
    fun registerBiometric(
        activity: FragmentActivity,
        sessionToken: ByteArray,
        onComplete: (Boolean) -> Unit //  추가: 등록 완료 여부 콜백
    ) {
        try {
            BiometricAuthManager.ensureKey()
            val encryptCipher: Cipher = BiometricAuthManager.getEncryptCipher()

            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val c = result.cryptoObject!!.cipher!!
                            val encrypted = c.doFinal(sessionToken)
                            val iv = c.iv
                            SecretStore.saveEncryptedToken(activity.applicationContext, iv, encrypted)

                            // ✅ 저장 확인 로그는 여기서!
                            val ok = SecretStore.loadEncryptedToken(activity.applicationContext) != null
                            Log.d("BIO", "register done: stored? $ok, iv=${iv.size}, ct=${encrypted.size}")

                            viewModelScope.launch {
                                _events.send(Event.BiometricReady(true, ok))
                                _events.send(Event.ShowSnack("생체 로그인 등록 완료"))
                            }
                            onComplete(ok) // ✅ 콜백
                        } catch (e: Exception) {
                            viewModelScope.launch { _events.send(Event.ShowSnack("등록 실패: ${e.message}")) }
                            onComplete(false)
                        }
                    }

                    override fun onAuthenticationError(code: Int, errString: CharSequence) {
                        viewModelScope.launch { _events.send(Event.ShowSnack(errString.toString())) }
                        onComplete(false)
                    }
                })

            prompt.authenticate(
                BiometricAuthManager.buildPromptInfoForLogin(),
                BiometricPrompt.CryptoObject(encryptCipher)
            )
        } catch (e: Exception) {
            viewModelScope.launch { _events.send(Event.ShowSnack("생체 등록 시작 실패: ${e.message}")) }
            onComplete(false)
        }
    }

    /**  저장된 토큰으로 생체 로그인(복호화) */
    fun startBiometricLogin(
        activity: FragmentActivity,
        onUseToken: (tokenPlain: ByteArray) -> Unit
    ) {
        val ctx = activity.applicationContext
        val stored = SecretStore.loadEncryptedToken(ctx)
        if (stored == null) {
            viewModelScope.launch { _events.send(Event.ShowSnack("저장된 생체 로그인 정보가 없어요")) }
            return
        }
        val (iv, ciphertext) = stored
        try {
            val decryptCipher = BiometricAuthManager.getDecryptCipher(iv)

            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val c = result.cryptoObject!!.cipher!!
                            val plain = c.doFinal(ciphertext)
                            onUseToken(plain)
                            viewModelScope.launch { _events.send(Event.BiometricLoginSuccess) }
                        } catch (e: Exception) {
                            viewModelScope.launch { _events.send(Event.ShowSnack("복호화 실패: ${e.message}")) }
                        }
                    }
                    override fun onAuthenticationError(code: Int, errString: CharSequence) {
                        viewModelScope.launch { _events.send(Event.ShowSnack(errString.toString())) }
                    }
                })

            prompt.authenticate(
                BiometricAuthManager.buildPromptInfoForLogin(),
                BiometricPrompt.CryptoObject(decryptCipher)
            )
        } catch (e: Exception) {
            viewModelScope.launch { _events.send(Event.ShowSnack("생체 로그인 시작 실패: ${e.message}")) }
        }
    }
}
