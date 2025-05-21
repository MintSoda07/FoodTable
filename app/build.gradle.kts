import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"

    //  Kotlin 2.0 + Compose 필수 플러그인
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("dagger.hilt.android.plugin") //  Hilt 플러그인 추가
    kotlin("kapt")

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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10" // 최신 버전 가능

    }
}

dependencies {
    // 파이어베이스 라이브러리
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    
    // 이미지 로드 라이브러리
    implementation(libs.glide)
    // JSON 파싱 라이브러리
    implementation(libs.gson)

    // HTTP 요청 라이브러리
    implementation(libs.okhttp) 
    implementation(libs.logging.interceptor)
    implementation(libs.converter.gson)  // JSON 변환

    // 포트원 결제 라이브러리
    implementation(libs.android.sdk.v010)
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.firebase.perf.ktx)
    implementation(libs.firebase.crashlytics.buildtools)

    // CoreLibraryDesugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs.v215)

    // 플랙스 레이아웃 사용을 위한 라이브러리
    implementation(libs.flexbox)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.flexbox)

    // 헬스 커넥트
    implementation(libs.androidx.connect.client)

    // 그래프
    implementation(libs.mpandroidchart)
    // Vico (Charts for Compose)
    implementation(libs.compose.m3)
    implementation(libs.core)

    implementation (libs.lottie.compose)
    // Material 아이콘 확장 (EmojiEvents, Star, MilitaryTech 등)
    implementation(libs.androidx.material.icons.extended)
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    debugImplementation(libs.androidx.ui.tooling)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.coil.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.common)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.activity.compose)
    implementation(libs.androidx.navigation.compose.v273)
    implementation(libs.lifecycle.viewmodel.compose)

    kapt(libs.hilt.compiler)

}