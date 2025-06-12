import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    // Hilt/KSP 관련 플러그인은 제거되었습니다.
}

android {
    namespace = "com.bcu.foodtable"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bcu.foodtable"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release-key.keystore")
            storePassword = "ftrelease1515"          // 직접 입력한 keystore 비밀번호
            keyAlias = "foodtable_release_key" // keytool에 입력한 alias
            keyPassword = "ftrelease1515"           // 키 비밀번호 (같은 경우 그대로 입력)
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            ) // 누들 순한맛 분모자토핑 // 밥 순한맛 x2
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig  = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

dependencies {
    // 1) Firebase BoM으로 버전 일괄 관리
    implementation(platform(libs.firebase.bom))

    // 2) Firebase 핵심 모듈들 (BoM이 버전을 관리)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.ml.modeldownloader.ktx)

    // 3) Firebase Functions (BoM에 포함되지 않으므로 별도 선언)
    implementation("com.google.firebase:firebase-functions-ktx:20.3.1")
    // 기타 유틸
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.converter.gson)
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    //카카오톡
    implementation("com.kakao.sdk:v2-all:2.21.4") // 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation("com.kakao.maps.open:android:2.12.8") // 카카오 로그인 API 모듈

// App Check
    implementation("com.google.firebase:firebase-appcheck-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")
    // 포트원 결제
    implementation("com.github.portone-io:android-sdk:0.1.0")

    // 성능 측정 및 오류 추적
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.firebase.perf.ktx)
    implementation(libs.firebase.crashlytics.buildtools)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.tv.material)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")

    // 광고, 차트, UI 보조
    implementation(libs.ads.mobile.sdk)
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha08")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.0")
    implementation("com.patrykandpatrick.vico:core:1.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("com.google.accompanist:accompanist-flowlayout:0.34.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
