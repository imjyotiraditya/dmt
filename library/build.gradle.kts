plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "dev.jyotiraditya.dmt.library"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
}

dependencies {
    implementation(libraryLibs.bundles.media3)
    implementation(libraryLibs.bundles.room)
    implementation(libraryLibs.androidx.core.ktx)
    implementation(libraryLibs.kotlinx.coroutines.guava)
    ksp(libraryLibs.androidx.room.compiler)
    testImplementation(libraryLibs.junit)
    testImplementation(libraryLibs.robolectric)
}
