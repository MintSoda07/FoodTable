pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        id("org.jetbrains.kotlin.kapt") version "2.0.0"
        id("com.google.devtools.ksp") version "2.0.0-1.0.20" // ✅ 안정 버전
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven { url = java.net.URI("https://devrepo.kakao.com/nexus/content/groups/public/")}
        maven { url = java.net.URI("https://devrepo.kakao.com/nexus/repository/kakaomap-releases/")}
        flatDir { dirs("unity/Final_AR_Library/unityLibrary/libs") }
    }
}

rootProject.name = "FoodTable"
include(":app")
//include(":unityLibrary")
//project(":unityLibrary").projectDir = file("unity/Final/unityLibrary")