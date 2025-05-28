package com.bcu.foodtable

import android.app.Application
import android.util.Log
import com.bcu.foodtable.di.AppContainer
// Firebase App Check 관련 import 문들
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.bcu.foodtable.BuildConfig

// ProviderInstaller 관련 import 추가
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller

class FoodTableApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // 보안 공급자 설치/업데이트 시도 (App Check 및 기타 Firebase 서비스 안정성 향상)
        installSecurityProvider()

        // 1. Firebase 앱 초기화
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "FirebaseApp.initializeApp called.")
        } else {
            Log.d("FirebaseInit", "FirebaseApp already initialized.")
        }

        // 2. Firebase App Check 인스턴스 가져오기
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // 3. App Check 공급자 설치 및 초기화
        if (BuildConfig.DEBUG) {
            Log.d("AppCheckSetup", "디버그 모드 감지. DebugAppCheckProviderFactory를 설치합니다.")
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            Log.d("AppCheckSetup", "릴리즈 모드 감지. PlayIntegrityAppCheckProviderFactory를 설치합니다.")
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        Log.d("AppCheckSetup", "Firebase App Check ProviderFactory 설치 시도 완료.")
        // --- Firebase App Check 초기화 코드 끝 ---

        appContainer = AppContainer()
    }

    private fun installSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this)
            Log.i("SecurityUpdate", "ProviderInstaller: Security Provider installed or updated.")
        } catch (e: GooglePlayServicesRepairableException) {
            Log.e("SecurityUpdate", "ProviderInstaller: GooglePlayServicesRepairableException. User action may be required.", e)
            // 프로덕션 앱에서는 사용자에게 Google Play 서비스 업데이트를 안내하는 것이 좋습니다.
            // 이 클래스는 Activity가 아니므로 직접 다이얼로그를 표시할 수 없습니다.
            // 필요하다면 Activity에서 이 로직을 호출하거나 다른 방식으로 사용자에게 알려야 합니다.
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.e("SecurityUpdate", "ProviderInstaller: GooglePlayServicesNotAvailableException. Essential for security.", e)
        } catch (e: Exception) {
            Log.e("SecurityUpdate", "ProviderInstaller: General Exception during security provider installation.", e)
        }
    }
}