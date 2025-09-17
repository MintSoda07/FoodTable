package com.bcu.foodtable.JetpackCompose.biometric
import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager

fun debugDumpBio(context: Context) {
    val pair = SecretStore.loadEncryptedToken(context)
    val has = pair != null
    val ivLen = pair?.first?.size ?: -1
    val ctLen = pair?.second?.size ?: -1

    val allow = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val r = BiometricManager.from(context).canAuthenticate(allow)

    Log.d("BIO", "=== BIO DUMP ===")
    Log.d("BIO", "stored? $has, ivLen=$ivLen, ctLen=$ctLen")
    Log.d(
        "BIO",
        "canAuthenticate=$r (0=SUCCESS, 11=NONE_ENROLLED, 12=NO_HARDWARE, 1=HW_UNAVAILABLE)"
    )
}