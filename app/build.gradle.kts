import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
}

val appVersionName = (project.findProperty("versionOverride") as String?) ?: "1.0"
val abiSplits = (project.findProperty("abiSplits") as String?)?.toBoolean() ?: true
val appVersionCode =
    appVersionName
        .substringBefore("-")
        .split(".")
        .mapNotNull { it.toIntOrNull() }
        .let {
            (it.getOrElse(0) { 0 } * 10_000) + (it.getOrElse(1) { 0 } * 100) + it.getOrElse(2) { 0 }
        }.coerceAtLeast(1)

val keystoreProps: Properties? =
    rootProject
        .file("keystore.properties")
        .takeIf { it.exists() }
        ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

fun signingValue(propertyKey: String, envKey: String): String? =
    keystoreProps?.getProperty(propertyKey) ?: System.getenv(envKey)

base {
    archivesName.set("dmt-$appVersionName")
}

android {
    namespace = "dev.jyotiraditya.dmt"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.jyotiraditya.dmt"
        minSdk = 30
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    splits {
        abi {
            isEnable = abiSplits
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("storeFile", "SIGNING_KEYSTORE_PATH")
            if (storePath != null && rootProject.file(storePath).exists()) {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS") ?: "dmt"
                keyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                signingConfigs
                    .getByName("release")
                    .takeIf { it.storeFile != null }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

baselineProfile {
    dexLayoutOptimization = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.coil)
    implementation(project(":lyrics"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    ksp(libs.hilt.compiler)
    implementation(project(":library"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
