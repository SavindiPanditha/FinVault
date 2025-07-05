pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Corrected syntax for JitPack
    }
}

rootProject.name = "ImiliPocket"
include(":app")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.5.0")
}