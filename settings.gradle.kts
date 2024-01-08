pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

includeBuild("../cliniko-kt")
includeBuild("../mongo-types")

rootProject.name = "gemini"