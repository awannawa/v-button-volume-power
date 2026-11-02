import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing (T10) is read from ~/keystores/vbutton-signing.properties
// when that file exists (personal machine; the file and the keystore it names
// live outside the repo and are never committed). On any other machine the
// file is absent and the release build falls back to the default debug
// signing config, so assembleRelease still produces an installable APK.
val releaseSigningProperties: Properties? = run {
    val file = File(System.getProperty("user.home"), "keystores/vbutton-signing.properties")
    if (file.isFile) Properties().apply { file.inputStream().use { load(it) } } else null
}

android {
    namespace = "com.awannawa.vbutton"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.awannawa.vbutton"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        releaseSigningProperties?.let { props ->
            create("release") {
                storeFile = File(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
