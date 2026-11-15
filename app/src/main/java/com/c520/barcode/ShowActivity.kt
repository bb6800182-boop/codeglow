package com.c520.barcode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.zxing.BarcodeFormat

/** 全螢幕條碼：這個視窗調到最亮、不自動關螢幕，點一下關閉。只影響這個視窗，離開就恢復，不用權限。 */
class ShowActivity : Activity() {
    companion object {
        private const val EXTRA_ID = "id"
        fun intent(c: Context, id: Long): Intent = Intent(c, ShowActivity::class.java).putExtra(EXTRA_ID, id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val k = Store.get(this, intent.getLongExtra(EXTRA_ID, 0))
        if (k == null) {
            finish()
            return
        }
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(buildUi(k))
    }

    private fun buildUi(k: Card): View {
        val dm = resources.displayMetrics
        val pad = px(R.dimen.space_lg)
        val w = dm.widthPixels - pad * 2
        val format = Barcode.formatOf(k.format) ?: BarcodeFormat.CODE_128
        // 一維條碼高度取寬度的一半、QR Code 取正方形，都不超過螢幕高度的一半
        val h = minOf(if (format == BarcodeFormat.QR_CODE) w else w / 2, dm.heightPixels / 2)
        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            try {
                setImageBitmap(Barcode.bitmap(Barcode.encode(k.value, format), w, h, getColor(R.color.barcode_ink), getColor(R.color.barcode_paper)))
            } catch (e: IllegalArgumentException) {
                // 存檔時檢查過，照理不會到這裡；真的發生就只顯示號碼
            }
        }
        // 條碼下面：名稱（小字）在前、號碼在後
        val label = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(text(k.name, R.dimen.text_body_sm, R.color.secondary))
            addView(text(k.value, R.dimen.text_mono, bold = true).mono(bold = true).apply {
                letterSpacing = resources.getFloat(R.dimen.mono_tracking)
            }, lp(WRAP_CONTENT).apply { marginStart = px(R.dimen.space_sm) })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            fitsSystemWindows = true
            addView(img, lp())
            addView(gap(R.dimen.space_md))
            addView(label, lp(WRAP_CONTENT))
            addView(gap(R.dimen.space_xl))
            addView(text(L.t("點一下關閉", "Tap to close"), R.dimen.text_caption, R.color.secondary))
            contentDescription = L.t("${k.name} 條碼，號碼 ${k.value}。點一下關閉", "${k.name} barcode, number ${k.value}. Tap to close")
            setOnClickListener { finish() }
        }
    }
}
