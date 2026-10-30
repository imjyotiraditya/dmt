plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.jyotiraditya.dmt.core.source"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":lyrics"))
    api(libs.ktor.client.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    testImplementation(libs.junit)
}
