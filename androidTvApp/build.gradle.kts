plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            optIn.addAll(
                "androidx.tv.material3.ExperimentalTvMaterial3Api",
                "androidx.compose.ui.ExperimentalComposeUiApi",
                "androidx.compose.foundation.ExperimentalFoundationApi",
            )
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":androidCommon"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

val appVersion = (findProperty("APP_VERSION") as? String)?.trimStart('v') ?: "dev"
val buildNumber = (findProperty("BUILD_NUMBER") as? String)?.toIntOrNull() ?: 0
// Same formula as the phone app plus 500, so the two artifacts of the one Play listing never share a versionCode.
val computedVersionCode =
    if (appVersion != "dev") {
        val parts = appVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val semverCode = parts.getOrElse(0) { 0 } * 10000 + parts.getOrElse(1) { 0 } * 100 + parts.getOrElse(2) { 0 }
        (semverCode * 1000 + buildNumber).coerceAtLeast(1)
    } else {
        1
    }

android {
    namespace = "ch.orellbuehler.acolyte.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.orellbuehler.acolyte"
        minSdk = 26
        targetSdk = 36
        versionCode = computedVersionCode + 500
        versionName = appVersion
    }

    signingConfigs {
        create("release") {
            val ksFile = System.getenv("ANDROID_KEYSTORE_FILE")
            if (ksFile != null) {
                storeFile = file(ksFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), rootProject.file("androidApp/proguard-rules.pro"))
        }
    }

    tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
        doFirst {
            check(System.getenv("ANDROID_KEYSTORE_FILE") != null) {
                "ANDROID_KEYSTORE_FILE is not set — refusing to build an unsigned release artifact"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        lintConfig = rootProject.file("androidApp/lint.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    dependencies {
        implementation(platform(libs.compose.bom))
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.tv.material)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.navigation)
        implementation(libs.activity.compose)
        implementation(libs.lifecycle.runtime.compose)
        implementation(libs.lifecycle.viewmodel.compose)
        implementation(libs.media3.exoplayer)
        implementation(libs.media3.exoplayer.hls)
        implementation(libs.media3.ui)
        implementation(libs.kotlinx.coroutines.android)
        debugImplementation(libs.compose.ui.tooling)
        testImplementation(libs.kotlin.test)
        testImplementation(libs.kotlin.test.junit)
        testImplementation(libs.kotlinx.coroutines.test)
        testImplementation(libs.robolectric)
        testImplementation(libs.compose.ui.test)
        debugImplementation(libs.compose.ui.test.manifest)
    }
}
