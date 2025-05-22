
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    id("com.google.gms.google-services") version "4.4.2" apply false
    //id("com.google.dagger.hilt.android") version "2.48" apply false
    //id("org.jetbrains.kotlin.kapt") version "2.0.0" apply false
    //id("com.google.devtools.ksp") version "1.0.20"'
}
