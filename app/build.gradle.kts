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
    implementation("com.github.portone-io:android-sdk:0.1.0")
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.firebase.perf.ktx)
    implementation(libs.firebase.crashlytics.buildtools)

    // CoreLibraryDesugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

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
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // 헬스 커넥트
    implementation("androidx.health.connect:connect-client:1.1.0-alpha08")

    // 그래프
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Vico (Charts for Compose)
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.0")
    implementation("com.patrykandpatrick.vico:core:1.13.0")

    implementation (libs.lottie.compose)
    // Material 아이콘 확장 (EmojiEvents, Star, MilitaryTech 등)
    implementation("androidx.compose.material:material-icons-extended")
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.navigation:navigation-compose")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("com.google.accompanist:accompanist-flowlayout:0.34.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-common:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    kapt("com.google.dagger:hilt-compiler:2.48")
    //  Kotlin 2.0에서는 필요 없음! → 삭제
    // implementation("androidx.compose.compiler:compiler:1.5.11")
}