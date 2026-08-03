pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    versionCatalogs {
        create("libraryLibs") { from(files("library/gradle/libs.versions.toml")) }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://raw.githubusercontent.com/imjyotiraditya/dmt-media3/maven/maven")
            content { includeGroup("androidx.media3") }
        }
    }
}

rootProject.name = "DMT"
include(":app")
include(":library")
include(":metadata")
include(":lyrics")
include(":baselineprofile")
