import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")

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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // 플랙스 레이아웃 사용을 위한 라이브러리
    implementation(libs.flexbox)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    //헬스 커넥터 라이브러리
    implementation("androidx.health.connect:connect-client:1.1.0-alpha08")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //제트팩 컴포즈 라이브러리
    implementation(platform("androidx.compose:compose-bom:2024.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")


}