package com.bcu.foodtable.JetpackCompose.biometric

import android.content.Context
import android.util.Base64
import android.util.Log

object SecretStore {
    private const val PREF_NAME = "babsang_bio_pref"
    private const val CT = "enc_token"
    private const val IV = "enc_iv"

    /** 이미 Keystore AES/GCM으로 암호화된 (iv, ciphertext)를 Base64로 일반 SharedPreferences에 저장 */
    fun saveEncryptedToken(ctx: Context, iv: ByteArray, ciphertext: ByteArray) {
        Log.d("BIO-STORE", "saveEncryptedToken(): iv=${iv.size}, ct=${ciphertext.size}")
        val b64Iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        val b64Ct = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        val ok = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(IV, b64Iv)
            .putString(CT, b64Ct)
            .commit() // 디버깅 편의상 commit으로 즉시 결과 확인

        Log.d("BIO-STORE", "saveEncryptedToken(): commit=$ok, ivB64Len=${b64Iv.length}, ctB64Len=${b64Ct.length}")
    }

    fun loadEncryptedToken(ctx: Context): Pair<ByteArray, ByteArray>? {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ivS = sp.getString(IV, null)
        val ctS = sp.getString(CT, null)

        val has = (ivS != null && ctS != null)
        Log.d(
            "BIO-STORE",
            "loadEncryptedToken(): has=$has, ivB64Len=${ivS?.length ?: -1}, ctB64Len=${ctS?.length ?: -1}"
        )
        if (!has) return null

        if (ivS!!.isEmpty() || ctS!!.isEmpty()) {
            Log.w("BIO-STORE", "loadEncryptedToken(): empty strings found (iv/ct). treating as missing")
            return null
        }

        return try {
            val iv = Base64.decode(ivS, Base64.NO_WRAP)
            val ct = Base64.decode(ctS, Base64.NO_WRAP)
            Log.d("BIO-STORE", "loadEncryptedToken(): decoded iv=${iv.size}, ct=${ct.size}")
            iv to ct
        } catch (t: Throwable) {
            Log.e("BIO-STORE", "loadEncryptedToken(): Base64 decode failed", t)
            null
        }
    }

    fun clear(ctx: Context) {
        Log.d("BIO-STORE", "clear(): remove all")
        val ok = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        Log.d("BIO-STORE", "clear(): commit=$ok")
    }
}
