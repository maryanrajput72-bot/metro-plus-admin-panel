plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.io.FileInputStream
import java.util.Properties

android {
    namespace = "com.metroplusadmin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.metroplusadmin"
        minSdk = 24
        targetSdk = 34
        // Version scheme: version(build) — ek version me build 01 se 100 tak.
        versionCode = 7
        versionName = "1.0.0(07)"
    }

    signingConfigs {
        named("debug") {
            // Apni key — Metro+ staff app wali key NAHI, taake dono apps
            // ek doosre ke upar install na hon. Keystore + passwords
            // sirf is machine par hain, GitHub par kabhi nahi.
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Zabii ka rule: iteration builds DEBUG, R8 release sirf final stage pe.
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
