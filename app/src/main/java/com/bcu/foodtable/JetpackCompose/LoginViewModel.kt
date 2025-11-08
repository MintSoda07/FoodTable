package com.bcu.foodtable.JetpackCompose

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.JetpackCompose.biometric.BiometricAuthManager
import com.bcu.foodtable.JetpackCompose.biometric.SecretStore
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
        Log.d("BIO-VM", "refreshBiometricState(): available=$available, hasStored=$hasStored")
        viewModelScope.launch {
            _events.send(Event.BiometricReady(available, hasStored))
        }
    }

    /** 최초 1회 등록: ENCRYPT 모드로 프롬프트 띄워서 토큰을 암호화해 저장 */
    fun registerBiometric(
        activity: FragmentActivity,
        sessionToken: ByteArray,
        onComplete: (Boolean) -> Unit // 등록 완료 여부 콜백
    ) {
        Log.d("BIO-VM", "registerBiometric(): start, tokenLen=${sessionToken.size}")
        try {
            BiometricAuthManager.ensureKey()
            Log.d("BIO-VM", "registerBiometric(): ensureKey() ok")

            val encryptCipher: Cipher = BiometricAuthManager.getEncryptCipher()
            Log.d("BIO-VM", "registerBiometric(): got encryptCipher")

            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        Log.d("BIO-PROMPT", "registerBiometric(): onSucceeded, hasCipher=${result.cryptoObject?.cipher != null}")
                        try {
                            val c = result.cryptoObject!!.cipher!!
                            val encrypted = c.doFinal(sessionToken)
                            val iv = c.iv

                            Log.d("BIO-STORE", "registerBiometric(): saveEncryptedToken iv=${iv.size}, ct=${encrypted.size}")
                            SecretStore.saveEncryptedToken(activity.applicationContext, iv, encrypted)

                            val ok = SecretStore.loadEncryptedToken(activity.applicationContext) != null
                            Log.d("BIO-VM", "registerBiometric(): stored? $ok, iv=${iv.size}, ct=${encrypted.size}")

                            viewModelScope.launch {
                                _events.send(Event.BiometricReady(true, ok))
                                _events.send(Event.ShowSnack("생체 로그인 등록 완료"))
                            }
                            onComplete(ok)
                        } catch (e: Exception) {
                            Log.e("BIO-VM", "registerBiometric(): encrypt/save error", e)
                            viewModelScope.launch { _events.send(Event.ShowSnack("등록 실패: ${e.message}")) }
                            onComplete(false)
                        }
                    }

                    override fun onAuthenticationError(code: Int, errString: CharSequence) {
                        Log.w("BIO-PROMPT", "registerBiometric(): onError code=$code, msg=$errString")
                        viewModelScope.launch { _events.send(Event.ShowSnack(errString.toString())) }
                        onComplete(false)
                    }

                    override fun onAuthenticationFailed() {
                        Log.w("BIO-PROMPT", "registerBiometric(): onFailed (will retry by system)")
                    }
                }
            )

            Log.d("BIO-PROMPT", "registerBiometric(): show prompt (ENCRYPT)")
            // 등록 프롬프트(문구 다르게 하고 싶으면 별도 buildPromptInfoForEnroll 사용)
            prompt.authenticate(
                BiometricAuthManager.buildPromptInfoForLogin(),
                BiometricPrompt.CryptoObject(encryptCipher)
            )

        } catch (e: KeyPermanentlyInvalidatedException) {
            // 생체 등록 변경 등으로 키 무효화 → 저장값 폐기 후 재등록 유도
            Log.e("BIO-VM", "registerBiometric(): KeyPermanentlyInvalidatedException, clear & re-enroll", e)
            val ctx = activity.applicationContext
            SecretStore.clear(ctx)
            BiometricAuthManager.ensureKey()
            viewModelScope.launch {
                _events.send(Event.ShowSnack("보안키가 변경되어 재등록이 필요합니다. 비밀번호 로그인 후 생체 등록해 주세요."))
                _events.send(
                    Event.BiometricReady(
                        BiometricAuthManager.canBiometricStrong(ctx),
                        hasStored = false
                    )
                )
            }
            onComplete(false)
        } catch (e: Exception) {
            Log.e("BIO-VM", "registerBiometric(): start error", e)
            viewModelScope.launch { _events.send(Event.ShowSnack("생체 등록 시작 실패: ${e.message}")) }
            onComplete(false)
        }
    }

    /** 저장된 토큰으로 생체 로그인(복호화) */
    fun startBiometricLogin(
        activity: FragmentActivity,
        onUseToken: (tokenPlain: ByteArray) -> Unit
    ) {
        Log.d("BIO-VM", "startBiometricLogin(): begin")
        val ctx = activity.applicationContext
        val stored = SecretStore.loadEncryptedToken(ctx)
        if (stored == null) {
            Log.w("BIO-STORE", "startBiometricLogin(): no stored pair")
            viewModelScope.launch { _events.send(Event.ShowSnack("저장된 생체 로그인 정보가 없어요")) }
            return
        }
        val (iv, ciphertext) = stored
        Log.d("BIO-STORE", "startBiometricLogin(): loaded iv=${iv.size}, ct=${ciphertext.size}")
        try {
            val decryptCipher = BiometricAuthManager.getDecryptCipher(iv)
            Log.d("BIO-VM", "startBiometricLogin(): got decryptCipher")

            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        Log.d("BIO-PROMPT", "startBiometricLogin(): onSucceeded, hasCipher=${result.cryptoObject?.cipher != null}")
                        try {
                            val c = result.cryptoObject!!.cipher!!
                            val plain = c.doFinal(ciphertext)
                            Log.d("BIO-VM", "startBiometricLogin(): decrypt ok, tokenLen=${plain.size}")

                            onUseToken(plain)
                            viewModelScope.launch { _events.send(Event.BiometricLoginSuccess) }
                        } catch (e: Exception) {
                            Log.e("BIO-VM", "startBiometricLogin(): decrypt failed", e)
                            viewModelScope.launch { _events.send(Event.ShowSnack("복호화 실패: ${e.message}")) }
                        }
                    }

                    override fun onAuthenticationError(code: Int, errString: CharSequence) {
                        Log.w("BIO-PROMPT", "startBiometricLogin(): onError code=$code, msg=$errString")
                        viewModelScope.launch { _events.send(Event.ShowSnack(errString.toString())) }
                    }

                    override fun onAuthenticationFailed() {
                        Log.w("BIO-PROMPT", "startBiometricLogin(): onFailed (will retry by system)")
                    }
                }
            )

            Log.d("BIO-PROMPT", "startBiometricLogin(): show prompt (DECRYPT)")
            prompt.authenticate(
                BiometricAuthManager.buildPromptInfoForLogin(),
                BiometricPrompt.CryptoObject(decryptCipher)
            )

        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e("BIO-VM", "startBiometricLogin(): KeyPermanentlyInvalidatedException, clear & re-enroll", e)
            // 키 무효화 → 저장값 폐기 후 재등록 유도
            SecretStore.clear(ctx)
            BiometricAuthManager.ensureKey()
            viewModelScope.launch {
                _events.send(Event.ShowSnack("보안키가 변경되어 재등록이 필요합니다. 비밀번호 로그인 후 생체 등록해 주세요."))
                _events.send(
                    Event.BiometricReady(
                        BiometricAuthManager.canBiometricStrong(ctx),
                        hasStored = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("BIO-VM", "startBiometricLogin(): start error", e)
            viewModelScope.launch { _events.send(Event.ShowSnack("생체 로그인 시작 실패: ${e.message}")) }
        }
    }
}
