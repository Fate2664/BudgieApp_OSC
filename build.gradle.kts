// Firebase BoM 35.x includes libraries compiled with Kotlin 2.4 metadata. AGP 9
// defaults to an older built-in Kotlin Gradle plugin, so select Kotlin 2.4 explicitly.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
