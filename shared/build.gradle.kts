plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
    alias(libs.plugins.kover)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.test.junit)
            }
        }
    }
}

android {
    namespace = "ch.orellbuehler.acolyte.shared"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val specFile = rootProject.layout.projectDirectory.file("api/openapi.json")
val liveUrl = providers.environmentVariable("ACOLYTE_LIVE_URL").orElse("")

tasks.withType<Test>().configureEach {
    inputs.file(specFile).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("liveUrl", liveUrl)
    systemProperty("acolyte.spec", specFile.asFile.absolutePath)
    systemProperty(
        "acolyte.reportDir",
        layout.buildDirectory
            .dir("reports/api")
            .get()
            .asFile.absolutePath,
    )
}

skie {
    // SKIE uploads anonymized build analytics by default; disable both the
    // capture and upload phases so CI/dev framework builds make no network
    // calls and leak no project metadata.
    analytics {
        enabled.set(false)
    }
}
