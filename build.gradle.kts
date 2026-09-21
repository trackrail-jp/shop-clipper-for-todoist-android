// Top-level build file where you can add configuration options common to all sub-projects/modules.
// AGP 9's built-in Kotlin brings KGP 2.2.10; overriding it here is the only way
// to move the Kotlin compiler plugins forward (AGP 9.0 release notes).
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}