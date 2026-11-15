package com.c520.barcode

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView

/** 主畫面：條碼清單（點一下全螢幕顯示）、新增、權限提示、怎麼放小工具。 */
class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var perm: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    // 從設定頁、編輯頁回來都重畫
    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        perm.visibility = if (Bright.allowed(this)) View.GONE else View.VISIBLE
        list.removeAllViews()
        val cards = Store.all(this)
        if (cards.isEmpty()) {
            list.addView(text("還沒有條碼。按「新增」加入發票載具或會員卡。", color = R.color.secondary))
            return
        }
        for (k in cards) {
            val edit = button("編輯") { startActivity(EditActivity.intent(this, k.id)) }.apply { contentDescription = "編輯「${k.name}」" }
            list.addView(
                cardRow(k, edit, "${k.name}，${k.value}，點一下全螢幕顯示條碼") { startActivity(ShowActivity.intent(this, k.id)) },
                lp().apply { bottomMargin = px(R.dimen.space_sm) },
            )
        }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = px(R.dimen.space_lg)
            setPadding(p, p, p, p)
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text(getString(R.string.app_name), R.dimen.text_h3, bold = true).apply { isAccessibilityHeading = true }, lp(0, WRAP_CONTENT, 1f))
            addView(button("＋ 新增", Kind.PRIMARY) { startActivity(EditActivity.intent(this@MainActivity, 0)) }.apply { contentDescription = "新增條碼" })
        })
        root.addView(gap(R.dimen.space_lg))
        perm = permissionCard()
        root.addView(perm, lp().apply { bottomMargin = px(R.dimen.space_md) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list, lp())
        root.addView(gap(R.dimen.space_md))
        root.addView(text("加到桌面：長按桌面空白處 →「小工具」→「碼上亮」，有「單一條碼」「多個店家」「單一圖示」三種。", R.dimen.text_body_sm, R.color.secondary))
        // 關於：聯繫作者、更多軟體（只開寄信的 App 和瀏覽器，App 自己不連網）
        root.addView(gap(R.dimen.space_lg))
        root.addView(text("關於", bold = true).apply { isAccessibilityHeading = true })
        root.addView(text("${getString(R.string.app_name)} ${About.version(this)}", R.dimen.text_body_sm, R.color.secondary))
        root.addView(gap(R.dimen.space_sm))
        root.addView(text(About.CONTACT_TEXT, R.dimen.text_body_sm, R.color.secondary))
        root.addView(text(About.MORE_TEXT, R.dimen.text_body_sm, R.color.secondary))
        root.addView(gap(R.dimen.space_sm))
        root.addView(LinearLayout(this).apply {
            addView(button("寄信給作者") { About.mail(this@MainActivity, getString(R.string.app_name)) })
            addView(button("更多軟體") { About.more(this@MainActivity) }, lp(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = px(R.dimen.space_sm) })
        })
        return ScrollView(this).apply {
            isFillViewport = true
            fitsSystemWindows = true // Android 15 起強制滿版，內容要自己讓開狀態列與導覽列
            addView(root)
        }
    }
}
