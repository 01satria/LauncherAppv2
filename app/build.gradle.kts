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
        versionName = "1.1"
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
            // proguard-android-optimize.txt: lebih agresif dari proguard-android.txt
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            // Optimasi tambahan: hapus unused resources via resource shrinker
            // (sudah di-handle isShrinkResources = true di atas)
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

    // ── FIX: versionCodeOverride sudah dihapus di AGP 8.x
    // Gunakan cara baru: override per-output via outputFileName saja,
    // versionCode unik diset lewat abiVersionCode di defaultConfig.
    //
    // Untuk Play Store multi-ABI upload, versionCode di-encode:
    //   arm64-v8a   → base * 10 + 1
    //   armeabi-v7a → base * 10 + 2
    //   x86_64      → base * 10 + 3
    //
    // Karena AGP 8.x tidak lagi mendukung versionCodeOverride,
    // kita pakai productFlavors atau cukup rename file saja.
    // Untuk launcher personal (non Play Store), rename file sudah cukup.
    applicationVariants.all {
        val variant = this
        outputs.all {
            // Cast ke ApkVariantOutput untuk akses outputFileName
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val abiFilter = output.getFilter("ABI")
            if (variant.buildType.name == "release") {
                output.outputFileName =
                    "CloudysLauncher-${variant.versionName}-${abiFilter ?: "universal"}.apk"
            }
        }
    }

    buildFeatures { compose = true }

    packaging {
        resources {
            // Buang file meta yang tidak dipakai — kecil tapi hemat RAM saat class loading
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/*.txt"
            excludes += "/*.properties"
        }
    }

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
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.tooling)
}