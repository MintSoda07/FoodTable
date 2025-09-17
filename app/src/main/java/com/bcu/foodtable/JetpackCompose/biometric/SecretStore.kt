package com.bcu.foodtable.JetpackCompose.biometric

import android.content.Context
import android.util.Base64

object SecretStore {
    private const val PREF_NAME = "babsang_bio_pref"
    private const val CT = "enc_token"
    private const val IV = "enc_iv"

    /** 이미 Keystore AES/GCM으로 암호화된 (iv, ciphertext)를 Base64로 일반 SharedPreferences에 저장 */
    fun saveEncryptedToken(ctx: Context, iv: ByteArray, ciphertext: ByteArray) {
        val b64Iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        val b64Ct = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(IV, b64Iv)
            .putString(CT, b64Ct)
            .apply()
    }

    fun loadEncryptedToken(ctx: Context): Pair<ByteArray, ByteArray>? {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ivS = sp.getString(IV, null) ?: return null
        val ctS = sp.getString(CT, null) ?: return null
        return Base64.decode(ivS, Base64.NO_WRAP) to Base64.decode(ctS, Base64.NO_WRAP)
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
