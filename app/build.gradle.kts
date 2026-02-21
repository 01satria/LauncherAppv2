plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "id.satria.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.satria.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // ── Signing — baca dari environment variable yang di-inject GitHub Actions
    signingConfigs {
        create("release") {
            storeFile     = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias      = System.getenv("KEY_ALIAS") ?: ""
            keyPassword   = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ── ABI Split — hasilkan APK terpisah per arsitektur CPU
    // arm64-v8a  : HP modern 64-bit (mayoritas device 2017+)
    // armeabi-v7a: HP lama 32-bit
    // x86_64     : Emulator / ChromeOS
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false   // tidak buat APK universal (hemat storage)
        }
    }

    // versionCode unik per ABI agar bisa upload ke Play Store sekaligus
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abiFilter = output.getFilter(com.android.build.OutputFile.ABI)
            val abiCode = when (abiFilter) {
                "arm64-v8a"   -> 1
                "armeabi-v7a" -> 2
                "x86_64"      -> 3
                else          -> 0
            }
            if (variant.buildType.name == "release") {
                output.outputFileName = "SatriaLauncher-${variant.versionName}-${abiFilter ?: "universal"}.apk"
                variant.mergedFlavor.versionCode?.let { base ->
                    // contoh: versionCode 10001 untuk arm64, 10002 untuk armeabi
                    output.versionCodeOverride = base * 10 + abiCode
                }
            }
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.tooling)
}
