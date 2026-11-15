# 碼上亮

放發票載具和會員卡條碼的 Android App。桌面小工具點一下，條碼就顯示並自動調到最亮，掃完恢復原本的亮度。免費、開源（MIT），不連網、沒有廣告。

- 介紹與下載：https://c520.pages.dev/mashangliang/
- 下載 APK：https://c520-dl.bb6800182.workers.dev/mashangliang/android （Android 12 以上）

## 功能

- 兩類條碼：發票載具（手機條碼，Code 39）和店家會員卡（Code 128、EAN-13、EAN-8、UPC-A、Code 39、ITF、Codabar、QR Code；打完號碼自動判斷格式，也可以自己選）。
- 三種桌面小工具：
  - 單一條碼：條碼直接顯示，點太陽鈕調到最亮，再點一下恢復。
  - 多個店家：圖示一覽，點圖示只留那張條碼並調到最亮，點條碼收回。
  - 單一圖示：平常只有一個圖示，點一下變成條碼。
- App 裡點條碼會全螢幕顯示、調到最亮，而且不自動關螢幕。
- 每張條碼可以選圖片當圖示；沒選就用名稱第一個字。
- 資料只存在手機（不備份），App 沒有網路權限。

## 權限

- 「修改系統設定」：桌面小工具沒有自己的視窗，要調亮只能改系統亮度。使用者在 App 裡按「前往允許」手動打開；不給的話條碼照樣顯示，只是不會變亮。

## 自己編譯

需要 JDK 17 和 Android SDK（compileSdk 36）。

```
gradlew.bat assembleDebug
```

- 不用 AndroidX；唯一的第三方套件是產生條碼的 [ZXing](https://github.com/zxing/zxing) core（Apache License 2.0）。
- 正式版要自己的簽章金鑰：在專案資料夾旁邊建 `barcode-widget-signing`，放金鑰檔和 `signing.properties`（`storeFile`＝金鑰檔名、`storePassword`、`keyAlias`、`keyPassword`），再 `gradlew.bat assembleRelease`。金鑰與密碼不要放進版控。

## 授權

[MIT](LICENSE) © 2026 C520。意見、問題歡迎來信：bb6800182@gmail.com；作者的其他作品在 [C520](https://c520.pages.dev/)。
