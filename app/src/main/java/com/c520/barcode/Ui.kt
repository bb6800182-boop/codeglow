package com.c520.barcode

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView

// 介面用程式碼組，不另外加套件。顏色、字級、間距、圓角一律引用 res/values 的設計 token（見 DESIGN.md）。

fun Context.px(dimen: Int) = resources.getDimensionPixelSize(dimen)

fun lp(w: Int = MATCH_PARENT, h: Int = WRAP_CONTENT, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

fun Context.text(s: CharSequence, size: Int = R.dimen.text_body, color: Int = R.color.primary, bold: Boolean = false) =
    TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(size))
        setTextColor(getColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

/** 號碼用等寬字，一碼一碼比較好對。 */
fun TextView.mono(bold: Boolean = false) = apply {
    typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
}

fun Context.box(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
    setColor(getColor(fill))
    cornerRadius = resources.getDimension(radius)
    if (stroke != null) setStroke(px(R.dimen.hairline), getColor(stroke))
}

fun Context.pressable(d: Drawable) = RippleDrawable(ColorStateList.valueOf(getColor(R.color.border)), d, null)

fun Context.gap(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, px(h)) }

enum class Kind { PRIMARY, SECONDARY, DANGER }

/** 按鈕：PRIMARY＝accent 底白字（每個畫面最多一個）、SECONDARY＝白底細框、DANGER＝白底紅字（只用在刪除）。 */
fun Context.button(label: String, kind: Kind = Kind.SECONDARY, onClick: () -> Unit) = Button(this).apply {
    text = label
    isAllCaps = false
    stateListAnimator = null
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
    setTextColor(getColor(when (kind) {
        Kind.PRIMARY -> R.color.on_accent
        Kind.SECONDARY -> R.color.primary
        Kind.DANGER -> R.color.error
    }))
    background = pressable(
        if (kind == Kind.PRIMARY) box(R.color.accent, R.dimen.radius_md) else box(R.color.surface, R.dimen.radius_md, R.color.border)
    )
    minHeight = px(R.dimen.touch)
    minimumHeight = px(R.dimen.touch)
    setPadding(px(R.dimen.space_md), 0, px(R.dimen.space_md), 0)
    setOnClickListener { onClick() }
}

/** 輸入框：白底細框，聚焦時框線換成 accent。 */
fun Context.input(hint: String) = object : EditText(this) {
    // 不讓密碼管理器（Bitwarden 等）跳出來填或問要不要存：新版 Android 連標成「不重要」的欄位也會去問自動填入服務，
    // 只有回報「這格不能自動填入」才擋得住（0.1.2 實測 importantForAutofill=NO 擋不住）
    override fun getAutofillType() = View.AUTOFILL_TYPE_NONE
}.apply {
    id = View.generateViewId()
    this.hint = hint
    isSingleLine = true
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
    setTextColor(getColor(R.color.primary))
    setHintTextColor(getColor(R.color.secondary))
    background = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), box(R.color.surface, R.dimen.radius_md, R.color.accent))
        addState(intArrayOf(), box(R.color.surface, R.dimen.radius_md, R.color.border))
    }
    setPadding(px(R.dimen.space_md), px(R.dimen.input_pad_v), px(R.dimen.space_md), px(R.dimen.input_pad_v))
    minHeight = px(R.dimen.touch)
}

/** 單選項目（放進 RadioGroup），至少 48dp 高。 */
fun Context.radio(s: String) = RadioButton(this).apply {
    id = View.generateViewId()
    text = s
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
    setTextColor(getColor(R.color.primary))
    minHeight = px(R.dimen.touch)
}

fun Context.card() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = box(R.color.surface, R.dimen.radius_lg, R.color.border)
    val p = px(R.dimen.space_md)
    setPadding(p, p, p, p)
}

/** 清單裡的一張條碼：圖示＋名稱／類型與格式／號碼，右邊可以放一個按鈕，整列可點。 */
fun Context.cardRow(k: Card, trailing: View?, description: String, onClick: () -> Unit) = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    background = pressable(box(R.color.surface, R.dimen.radius_lg, R.color.border))
    val p = px(R.dimen.space_md)
    setPadding(p, p, p, p)
    addView(ImageView(context).apply {
        setImageBitmap(Icons.of(context, k))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }, lp(px(R.dimen.icon), px(R.dimen.icon)))
    val info = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(k.name, bold = true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        addView(text("${if (k.carrier) L.t("發票載具", "Taiwan e-invoice") else L.t("會員卡", "Membership card")} · ${Barcode.label(k.format)}", R.dimen.text_caption, R.color.secondary))
        addView(text(k.value, R.dimen.text_body_sm).mono().apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
    }
    addView(info, lp(0, WRAP_CONTENT, 1f).apply { marginStart = p; marginEnd = px(R.dimen.space_sm) })
    if (trailing != null) addView(trailing)
    contentDescription = description
    setOnClickListener { onClick() }
}

/** 還沒允許「修改系統設定」時的提示卡，按鈕打開設定頁。 */
fun Activity.permissionCard() = card().apply {
    addView(text(L.t("小工具要把螢幕調到最亮，需要允許「修改系統設定」。", "Widgets need \"Modify system settings\" to turn the screen up to full brightness.")))
    addView(gap(R.dimen.space_sm))
    addView(button(L.t("前往允許", "Allow")) { Bright.openSettings(this@permissionCard) }, lp(WRAP_CONTENT))
}
