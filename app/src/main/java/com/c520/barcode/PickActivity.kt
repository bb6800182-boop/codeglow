package com.c520.barcode

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView

/**
 * 選「單一條碼」或「單一圖示」小工具要顯示哪一張（單一圖示還要選圖示位置）：
 * 放上桌面時由啟動器打開；Android 12 起長按小工具也能重新選。
 */
class PickActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var iconWidget = false
    private var alignGroup: RadioGroup? = null
    private val alignIds = IntArray(3) // 靠左／置中／靠右三個選項的 view id，索引同 Widgets.ALIGN_*
    private var listView: LinearLayout? = null
    private lateinit var perm: View

    companion object {
        fun intent(c: Context, id: Int): Intent = Intent(c, PickActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // 先回「取消」：沒選就離開，啟動器就不放這個小工具
        setResult(RESULT_CANCELED, result())
        // 別的 App 也打得開這頁：只接受本 App 的單一條碼／單一圖示小工具，不能讓人亂指定 ID
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider
        iconWidget = provider == ComponentName(this, IconWidget::class.java)
        if (!iconWidget && provider != ComponentName(this, SingleWidget::class.java)) {
            finish()
            return
        }
        setContentView(buildUi())
    }

    // 從新增頁回來要看到新的條碼
    override fun onResume() {
        super.onResume()
        render()
    }

    private fun result() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    private fun render() {
        val list = listView ?: return
        perm.visibility = if (Bright.allowed(this)) View.GONE else View.VISIBLE
        list.removeAllViews()
        val cards = Store.all(this)
        if (cards.isEmpty()) {
            list.addView(text(L.t("還沒有條碼，先新增一張。", "No barcodes yet. Add one first."), color = R.color.secondary))
            list.addView(gap(R.dimen.space_md))
            list.addView(button(L.t("新增條碼", "Add barcode"), Kind.PRIMARY) { startActivity(EditActivity.intent(this, 0)) }, lp(WRAP_CONTENT))
            return
        }
        for (k in cards) {
            list.addView(
                cardRow(k, null, L.t("${k.name}，${k.value}，點一下放到小工具", "${k.name}, ${k.value}. Tap to put it on the widget")) { pick(k) },
                lp().apply { bottomMargin = px(R.dimen.space_sm) },
            )
        }
    }

    private fun pick(k: Card) {
        val align = alignGroup?.let { g -> alignIds.indexOf(g.checkedRadioButtonId).takeIf { it >= 0 } ?: Widgets.ALIGN_CENTER }
        Widgets.setCard(this, widgetId, k.id, align)
        setResult(RESULT_OK, result())
        finish()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = px(R.dimen.space_lg)
            setPadding(p, p, p, p)
        }
        root.addView(text(L.t("選擇要放在小工具的條碼", "Choose the barcode for this widget"), R.dimen.text_h3, bold = true).apply { isAccessibilityHeading = true })
        root.addView(gap(R.dimen.space_xs))
        val hint = if (iconWidget) {
            L.t("平常只有圖示（背景透明）；點圖示變白底顯示條碼並調到最亮，再點一下恢復。", "Normally just an icon on a clear background. Tap it to show the barcode on white at full brightness; tap again to restore.")
        } else {
            L.t("點條碼右下的太陽調到最亮，最亮時點條碼恢復。", "Tap the sun at the lower right for full brightness; tap the barcode to restore.")
        }
        root.addView(text(hint, R.dimen.text_body_sm, R.color.secondary))
        root.addView(gap(R.dimen.space_lg))
        if (iconWidget) root.addView(alignChooser(), lp().apply { bottomMargin = px(R.dimen.space_md) })
        perm = permissionCard()
        root.addView(perm, lp().apply { bottomMargin = px(R.dimen.space_md) })
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list, lp())
        listView = list
        return ScrollView(this).apply {
            isFillViewport = true
            fitsSystemWindows = true
            addView(root)
        }
    }

    /** 單一圖示小工具的圖示位置：靠左、置中、靠右（對齊桌面第一格／正中間／最後一格）。 */
    private fun alignChooser(): View {
        val group = RadioGroup(this).apply {
            id = View.generateViewId()
            orientation = RadioGroup.HORIZONTAL
        }
        listOf(L.t("靠左", "Left"), L.t("置中", "Center"), L.t("靠右", "Right")).forEachIndexed { i, s ->
            val b = radio(s)
            alignIds[i] = b.id
            group.addView(b, RadioGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { if (i > 0) marginStart = px(R.dimen.space_lg) })
        }
        group.check(alignIds[Widgets.align(this, widgetId).coerceIn(alignIds.indices)])
        alignGroup = group
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(L.t("圖示位置", "Icon position"), R.dimen.text_body_sm, R.color.secondary).apply { labelFor = group.id })
            addView(group)
        }
    }
}
