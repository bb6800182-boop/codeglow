import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.c520.barcode"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.c520.barcode"
        // Android 12 起：小工具可重新設定、PendingIntent 可變旗標、ImageDecoder 都有，程式不用寫舊版分支
        minSdk = 31
        targetSdk = 36
        versionCode = 6
        versionName = "0.2.1"
    }

    // 簽章放在專案資料夾旁邊的 barcode-widget-signing（不進版控；用 Dropbox 同步，每台電腦都用同一份）：
    // 正式版的金鑰與密碼寫在 signing.properties；開發版用那裡的 debug.keystore。
    // 沒有這個資料夾的電腦照樣能編 debug 版（用 Android 預設的 debug 金鑰），只是編不出簽好章的正式版。
    val signingDir = rootDir.resolveSibling("barcode-widget-signing")
    val signing = Properties().apply { signingDir.resolve("signing.properties").takeIf { it.isFile }?.reader()?.use { load(it) } }
    signingConfigs {
        signingDir.resolve("debug.keystore").takeIf { it.isFile }?.let { getByName("debug").storeFile = it }
        signing.getProperty("storeFile")?.let { store ->
            create("release") {
                storeFile = signingDir.resolve(store)
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    // 正式版不做程式碼縮減：App 很小，而且這樣正式版和測試過的開發版是同一份程式
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // 資安相關的 lint 一律當錯誤
        error += listOf(
            "ExportedReceiver", "ExportedService", "UnsafeImplicitIntentLaunch",
            "MutableImplicitPendingIntent", "UnspecifiedRegisterReceiverFlag",
        )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
}
