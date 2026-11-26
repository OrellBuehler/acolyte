plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

android {
    namespace = "ch.orellbuehler.acolyte.common"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        lintConfig = rootProject.file("androidApp/lint.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        api(project(":shared"))
        api(platform(libs.compose.bom))
        api(libs.compose.ui)
        api(libs.compose.material3)
        api(libs.coil.compose)
        api(libs.coil.network.okhttp)
        api(libs.okhttp)
        api(libs.kotlinx.coroutines.android)
        api(libs.lifecycle.viewmodel.compose)
    }
}
