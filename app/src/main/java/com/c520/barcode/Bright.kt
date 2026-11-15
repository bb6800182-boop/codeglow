package com.c520.barcode

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * 小工具調亮螢幕：小工具沒有自己的視窗，只能改系統亮度（要「修改系統設定」權限）。
 * 調亮前記下原本的亮度與自動調整，恢復時寫回去。App 自己的全螢幕畫面不用這個（改視窗亮度就好）。
 */
object Bright {
    private const val BOOSTED = "boosted"
    private const val MODE = "mode"
    private const val LEVEL = "level"

    private fun prefs(c: Context) = c.getSharedPreferences("bright", Context.MODE_PRIVATE)

    fun allowed(c: Context) = Settings.System.canWrite(c)

    /** 調到最亮；沒有權限傳回 false。已經調亮了就不再記錄（不然會把「最亮」當成原本的亮度）。 */
    fun boost(c: Context): Boolean {
        if (!allowed(c)) return false
        val cr = c.contentResolver
        return try {
            val p = prefs(c)
            if (!p.getBoolean(BOOSTED, false)) {
                p.edit()
                    .putInt(MODE, Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL))
                    .putInt(LEVEL, Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, -1))
                    .putBoolean(BOOSTED, true)
                    .apply()
            }
            // 先關自動調整再調亮：自動模式下改亮度，系統會當成使用者在教它自動亮度
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, maxLevel())
            true
        } catch (e: SecurityException) {
            false // 權限剛好被收回
        }
    }

    /** 恢復原本的亮度與自動調整；沒調亮過就什麼都不做。 */
    fun restore(c: Context) {
        val p = prefs(c)
        if (!p.getBoolean(BOOSTED, false)) return
        if (allowed(c)) {
            try {
                val cr = c.contentResolver
                // 還在手動模式時寫回亮度，再切回原本的模式（理由同上）
                val level = p.getInt(LEVEL, -1)
                if (level >= 0) Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, level)
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, p.getInt(MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL))
            } catch (e: SecurityException) {
                // 權限被收回：亮度改不了，只清掉記錄
            }
        }
        p.edit().putBoolean(BOOSTED, false).apply()
    }

    /** 系統亮度設定的最大值：原生 Android 是 255，少數廠牌更大，讀系統設定，讀不到就用 255。 */
    private fun maxLevel(): Int {
        val r = Resources.getSystem()
        @Suppress("DiscouragedApi")
        val id = r.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android")
        return maxOf(255, if (id != 0) r.getInteger(id) else 0)
    }

    /** 打開本 App 的「修改系統設定」頁。 */
    fun openSettings(a: Activity) {
        try {
            a.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + a.packageName)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(a, "找不到設定頁，請到 設定 > 應用程式 > 碼上亮 > 修改系統設定", Toast.LENGTH_LONG).show()
        }
    }
}
