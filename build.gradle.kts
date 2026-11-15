plugins {
    alias(libs.plugins.android.application) apply false
}

// 專案在 Dropbox 裡，Dropbox 會把新檔換成雲端預留位置，Gradle 讀不到自己的輸出，所以編譯輸出放到 Dropbox 外面。
val localAppData: String? = System.getenv("LOCALAPPDATA")
if (localAppData != null && "Dropbox" in rootDir.path) {
    allprojects { layout.buildDirectory.set(File(localAppData, "barcode-widget-build/$name")) }
}
