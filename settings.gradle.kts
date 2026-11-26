pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Acolyte"
include(":shared")
include(":androidCommon")
include(":androidApp")
include(":androidTvApp")
