package com.c520.barcode

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat

/** 新增／編輯一張條碼：類型、名稱、號碼、格式（會員卡才有）、圖示，下面即時預覽。 */
class EditActivity : Activity() {
    companion object {
        private const val EXTRA_ID = "id"
        private const val REQ_IMAGE = 1
        private val CARRIER_NAME get() = L.t("發票載具", "E-invoice carrier")
        private const val MAX_NAME = 20
        fun intent(c: Context, id: Long): Intent = Intent(c, EditActivity::class.java).putExtra(EXTRA_ID, id)
    }

    private var cardId = 0L // 0＝新增
    private var kind = Card.MEMBER
    private var format = BarcodeFormat.CODE_128
    private var formatTouched = false // 使用者自己選過格式，就不再自動猜
    private var savedIcon: Bitmap? = null // 原本存的自訂圖示
    private var newIcon: Bitmap? = null // 剛選的圖，按儲存才寫檔
    private var iconRemoved = false

    private lateinit var kindGroup: RadioGroup
    private lateinit var carrierBtn: RadioButton
    private lateinit var memberBtn: RadioButton
    private lateinit var nameIn: EditText
    private lateinit var valueIn: EditText
    private lateinit var valueHint: TextView
    private lateinit var formatBox: View
    private lateinit var formatSpin: Spinner
    private lateinit var iconView: ImageView
    private lateinit var iconReset: Button
    private lateinit var previewBox: View
    private lateinit var previewImg: ImageView
    private lateinit var previewName: TextView
    private lateinit var previewValue: TextView
    private lateinit var error: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardId = intent.getLongExtra(EXTRA_ID, 0)
        val card = Store.get(this, cardId)
        if (cardId > 0 && card == null) {
            finish() // 已經被刪掉了
            return
        }
        setContentView(buildUi(editing = card != null))
        if (card != null) {
            kind = card.kind
            format = Barcode.formatOf(card.format) ?: BarcodeFormat.CODE_128
            formatTouched = true
            if (card.icon) savedIcon = BitmapFactory.decodeFile(Store.iconFile(this, card.id).path)
            nameIn.setText(card.name)
            valueIn.setText(card.value)
        }
        formatSpin.setSelection(indexOf(format))
        (if (kind == Card.CARRIER) carrierBtn else memberBtn).isChecked = true
        applyKind()
        listen() // 初始值都填好才開始監聽，免得一開始就觸發自動判斷
        renderIcon()
        refresh()
    }

    private fun indexOf(f: BarcodeFormat) = Barcode.CHOICES.indexOfFirst { it.first == f }.coerceAtLeast(0)

    /** 依類型切換：發票載具固定 Code 39、自動大寫、最多 8 碼；會員卡可以選格式。 */
    private fun applyKind() {
        val carrier = kind == Card.CARRIER
        formatBox.visibility = if (carrier) View.GONE else View.VISIBLE
        if (carrier) {
            // 英數鍵盤（不跳注音）、沒有選字建議；小寫由 AllCaps 轉大寫
            valueIn.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            valueIn.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(8))
            valueIn.hint = "/ABC1234"
            valueHint.text = L.t("手機條碼共 8 碼，第 1 碼是「/」", "Taiwan e-invoice mobile barcode: 8 characters, starting with \"/\"")
            if (nameIn.text.isBlank()) nameIn.setText(CARRIER_NAME)
        } else {
            // 不要選字建議、不要自動更正（號碼不是文字）
            valueIn.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            valueIn.filters = arrayOf(InputFilter.LengthFilter(500))
            valueIn.hint = L.t("卡片條碼下面的號碼", "The number under the barcode on your card")
            valueHint.text = L.t("格式會依號碼自動判斷；店家掃不到再換一種試試", "The format is picked from the number. If the store can't scan it, try another one")
            if (nameIn.text.toString() == CARRIER_NAME) nameIn.setText("")
        }
    }

    private fun listen() {
        kindGroup.setOnCheckedChangeListener { _, id ->
            kind = if (id == carrierBtn.id) Card.CARRIER else Card.MEMBER
            applyKind()
            refresh()
        }
        nameIn.addTextChangedListener(watcher {
            renderIcon()
            refresh()
        })
        valueIn.addTextChangedListener(watcher {
            if (kind == Card.MEMBER && !formatTouched) {
                val g = Barcode.guess(Barcode.normalize(valueIn.text.toString(), BarcodeFormat.CODE_128))
                if (g != format) {
                    format = g
                    formatSpin.setSelection(indexOf(g))
                }
            }
            refresh()
        })
        formatSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val f = Barcode.CHOICES[position].first
                if (f == format) return // 程式自己設的（或沒變），不算使用者選的
                format = f
                formatTouched = true
                refresh()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun watcher(after: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, n: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = after()
    }

    /** 目前的號碼（已整理）與格式。 */
    private fun current(): Pair<String, BarcodeFormat> {
        val f = if (kind == Card.CARRIER) BarcodeFormat.CODE_39 else format
        return Barcode.normalize(valueIn.text.toString(), f, kind == Card.CARRIER) to f
    }

    /** 即時預覽；號碼有問題就顯示原因。傳回錯誤訊息，null＝可以儲存。 */
    private fun refresh(): String? {
        val (v, f) = current()
        var err: String? = when {
            v.isEmpty() -> L.t("請輸入號碼", "Enter the number")
            kind == Card.CARRIER && !Barcode.isCarrier(v) -> L.t("手機條碼要 8 碼：第 1 碼是「/」，後 7 碼是數字、大寫英文或 + - .", "The mobile barcode has 8 characters: \"/\" first, then 7 digits, capital letters or + - .")
            else -> null
        }
        if (err == null) {
            try {
                val m = Barcode.encode(v, f)
                val w = resources.displayMetrics.widthPixels - px(R.dimen.space_lg) * 2 - px(R.dimen.space_md) * 2
                previewImg.setImageBitmap(Barcode.bitmap(m, w, px(R.dimen.preview_h), getColor(R.color.barcode_ink), getColor(R.color.barcode_paper)))
                previewName.text = nameIn.text.toString().trim()
                previewValue.text = v
            } catch (e: IllegalArgumentException) {
                err = e.message ?: Barcode.rule(f)
            }
        }
        previewBox.visibility = if (err == null) View.VISIBLE else View.GONE
        // 還沒打號碼時不急著報錯
        showError(if (v.isEmpty()) null else err)
        return err
    }

    private fun showError(msg: String?) {
        error.text = msg ?: ""
        error.visibility = if (msg == null) View.GONE else View.VISIBLE
    }

    private fun renderIcon() {
        val custom = newIcon ?: savedIcon.takeIf { !iconRemoved }
        iconView.setImageBitmap(custom ?: Icons.letter(this, nameIn.text.toString()))
        iconReset.visibility = if (custom != null) View.VISIBLE else View.GONE
    }

    private fun save() {
        val name = nameIn.text.toString().trim()
        val err = refresh()
        if (name.isEmpty()) {
            showError(L.t("請輸入名稱", "Enter a name"))
            nameIn.requestFocus()
            return
        }
        if (err != null) {
            showError(err)
            valueIn.requestFocus()
            return
        }
        val (v, f) = current()
        val id = if (cardId > 0) cardId else System.currentTimeMillis()
        var icon = savedIcon != null && !iconRemoved
        val picked = newIcon
        if (picked != null) {
            if (Icons.save(this, id, picked)) icon = true
            else Toast.makeText(this, L.t("圖示存不起來，先用文字圖示", "Couldn't save the icon. Using a letter icon for now"), Toast.LENGTH_LONG).show()
        }
        if (!icon) Store.iconFile(this, id).delete()
        Store.put(this, Card(id, name.take(MAX_NAME), kind, v, f.name, icon))
        Widgets.refreshAll(this)
        finish()
    }

    private fun confirmDelete() {
        val name = nameIn.text.toString().trim().ifEmpty { L.t("這張條碼", "this barcode") }
        val d = AlertDialog.Builder(this)
            .setTitle(L.t("刪除「$name」？", "Delete \"$name\"?"))
            .setMessage(L.t("小工具上的這張條碼也會一起移除。", "It will be removed from your widgets too."))
            .setNegativeButton(L.t("取消", "Cancel"), null)
            .setPositiveButton(L.t("刪除", "Delete")) { _, _ ->
                Store.delete(this, cardId)
                Widgets.refreshAll(this)
                finish()
            }
            .show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.error))
    }

    private fun pickImage() {
        // Android 13 起用系統的相片挑選器（不用任何權限）；Android 12 用檔案選擇
        val i = if (Build.VERSION.SDK_INT >= 33) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(i, REQ_IMAGE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, L.t("找不到可以選圖片的 App", "No app found to pick an image"), Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_IMAGE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val b = Icons.fromUri(this, uri)
        if (b == null) {
            Toast.makeText(this, L.t("這張圖片讀不到，換一張試試", "Can't read this image. Try another one"), Toast.LENGTH_LONG).show()
            return
        }
        newIcon = b
        iconRemoved = false
        renderIcon()
    }

    // ---------- 畫面 ----------

    /** 欄位標題；有對應的欄位就設 labelFor，讀屏軟體才念得出欄位名稱。 */
    private fun label(s: String, field: View? = null) = text(s, R.dimen.text_body_sm, R.color.secondary).apply {
        if (field != null) labelFor = field.id
        setPadding(0, 0, 0, px(R.dimen.space_xs))
    }

    private fun buildUi(editing: Boolean): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = px(R.dimen.space_lg)
            setPadding(p, p, p, p)
        }
        root.addView(text(if (editing) L.t("編輯條碼", "Edit barcode") else L.t("新增條碼", "Add barcode"), R.dimen.text_h3, bold = true).apply { isAccessibilityHeading = true })
        root.addView(gap(R.dimen.space_lg))

        carrierBtn = radio(L.t("發票載具", "Taiwan e-invoice"))
        memberBtn = radio(L.t("會員卡", "Membership card"))
        kindGroup = RadioGroup(this).apply {
            id = View.generateViewId()
            orientation = RadioGroup.HORIZONTAL
            addView(carrierBtn)
            addView(memberBtn, RadioGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = px(R.dimen.space_lg) })
        }
        root.addView(label(L.t("類型", "Type"), kindGroup))
        root.addView(kindGroup)
        root.addView(gap(R.dimen.space_md))

        nameIn = input(L.t("例如：全聯、家樂福", "e.g. the store name")).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            filters = arrayOf(InputFilter.LengthFilter(MAX_NAME))
        }
        root.addView(label(L.t("名稱", "Name"), nameIn))
        root.addView(nameIn, lp())
        root.addView(gap(R.dimen.space_md))

        valueIn = input("")
        root.addView(label(L.t("號碼", "Number"), valueIn))
        root.addView(valueIn, lp())
        valueHint = text("", R.dimen.text_caption, R.color.secondary)
        root.addView(valueHint, lp().apply { topMargin = px(R.dimen.space_xs) })
        root.addView(gap(R.dimen.space_md))

        formatSpin = Spinner(this).apply {
            id = View.generateViewId()
            adapter = ArrayAdapter(this@EditActivity, android.R.layout.simple_spinner_item, Barcode.CHOICES.map { it.second }).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            minimumHeight = px(R.dimen.touch)
        }
        formatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(L.t("格式", "Format"), formatSpin))
            addView(formatSpin, lp(WRAP_CONTENT))
            addView(gap(R.dimen.space_md))
        }
        root.addView(formatBox, lp())

        iconView = ImageView(this).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        iconReset = button(L.t("改用文字", "Use a letter")) {
            newIcon = null
            iconRemoved = true
            renderIcon()
        }
        root.addView(label(L.t("圖示", "Icon")))
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(iconView, lp(px(R.dimen.icon), px(R.dimen.icon)))
            addView(button(L.t("選擇圖片", "Choose image")) { pickImage() }, lp(WRAP_CONTENT).apply { marginStart = px(R.dimen.space_md) })
            addView(iconReset, lp(WRAP_CONTENT).apply { marginStart = px(R.dimen.space_sm) })
        })
        root.addView(gap(R.dimen.space_lg))

        // 預覽：跟小工具一樣，條碼＋「名稱（小字）　號碼」
        previewImg = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        previewName = text("", R.dimen.text_micro, R.color.secondary)
        previewValue = text("", R.dimen.text_caption).mono()
        previewBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(L.t("預覽", "Preview")))
            addView(card().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(previewImg, lp())
                addView(LinearLayout(this@EditActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    addView(previewName)
                    addView(previewValue, lp(WRAP_CONTENT).apply { marginStart = px(R.dimen.space_sm) })
                }, lp(WRAP_CONTENT).apply { topMargin = px(R.dimen.space_xs) })
            }, lp())
        }
        root.addView(previewBox, lp())

        error = text("", R.dimen.text_body_sm, R.color.error).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            visibility = View.GONE
        }
        root.addView(error, lp().apply { topMargin = px(R.dimen.space_md) })
        root.addView(gap(R.dimen.space_lg))
        root.addView(button(L.t("儲存", "Save"), Kind.PRIMARY) { save() }, lp())
        if (editing) {
            root.addView(gap(R.dimen.space_sm))
            root.addView(button(L.t("刪除", "Delete"), Kind.DANGER) { confirmDelete() }, lp())
        }
        return ScrollView(this).apply {
            isFillViewport = true
            fitsSystemWindows = true
            addView(root)
        }
    }
}
