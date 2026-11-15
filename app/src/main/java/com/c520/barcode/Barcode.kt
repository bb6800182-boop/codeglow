package com.c520.barcode

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

/** 號碼檢查與條碼編碼。除了最下面的 bitmap() 都是純邏輯，可以在電腦上跑單元測試。 */
object Barcode {
    /** 會員卡可選的格式，順序＝選單順序。發票載具固定用 Code 39。 */
    val CHOICES = listOf(
        BarcodeFormat.CODE_128 to "Code 128",
        BarcodeFormat.EAN_13 to "EAN-13",
        BarcodeFormat.EAN_8 to "EAN-8",
        BarcodeFormat.UPC_A to "UPC-A",
        BarcodeFormat.CODE_39 to "Code 39",
        BarcodeFormat.ITF to "ITF",
        BarcodeFormat.CODABAR to "Codabar",
        BarcodeFormat.QR_CODE to "QR Code",
    )

    fun formatOf(name: String): BarcodeFormat? = CHOICES.firstOrNull { it.first.name == name }?.first

    fun label(format: String) = CHOICES.firstOrNull { it.first.name == format }?.second ?: format

    private const val CODE39_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. \$/+%"

    /** 手機條碼（發票載具）：第 1 碼是 /，後 7 碼是數字、大寫英文或 + - . */
    private val CARRIER = Regex("^/[0-9A-Z+.\\-]{7}$")

    fun isCarrier(v: String) = CARRIER.matches(v)

    /**
     * 整理打進來的號碼：
     * - 一維條碼去掉所有空白（卡片上的號碼常分組印）；QR Code 只去頭尾空白。
     * - 發票載具轉大寫。
     * - EAN-13／EAN-8／UPC-A 少打最後的檢查碼就補上，畫面上的號碼才會跟條碼內容一樣。
     */
    fun normalize(raw: String, format: BarcodeFormat, carrier: Boolean = false): String {
        var v = if (format == BarcodeFormat.QR_CODE) raw.trim() else raw.filterNot { it.isWhitespace() }
        if (carrier) v = v.uppercase()
        val full = when (format) {
            BarcodeFormat.EAN_13 -> 13
            BarcodeFormat.EAN_8 -> 8
            BarcodeFormat.UPC_A -> 12
            else -> 0
        }
        if (full > 0 && v.length == full - 1 && v.all { it in '0'..'9' }) v += checkDigit(v)
        return v
    }

    /**
     * 依號碼猜格式：13 位數字且檢查碼正確 → EAN-13；有中文等非 ASCII 字 → QR Code；其他 → Code 128。
     * 12 位數不猜 UPC-A：有的收銀機會把 UPC-A 前面補 0 變 13 碼，會員號碼就對不上。
     */
    fun guess(v: String): BarcodeFormat = when {
        v.length == 13 && gtinOk(v) -> BarcodeFormat.EAN_13
        v.any { it.code !in 32..126 } -> BarcodeFormat.QR_CODE
        else -> BarcodeFormat.CODE_128
    }

    /** GTIN 檢查碼（EAN-13、EAN-8、UPC-A 通用）：資料位數從右邊起依序乘 3、1。 */
    fun checkDigit(body: String): Char {
        var sum = 0
        body.reversed().forEachIndexed { i, ch -> sum += (ch - '0') * if (i % 2 == 0) 3 else 1 }
        return '0' + (10 - sum % 10) % 10
    }

    fun gtinOk(v: String) = v.length >= 2 && v.all { it in '0'..'9' } && checkDigit(v.dropLast(1)) == v.last()

    /** 每種格式能用什麼字；號碼不合時顯示給使用者。 */
    fun rule(format: BarcodeFormat): String = when (format) {
        BarcodeFormat.EAN_13 -> L.t("EAN-13 要 12 或 13 位數字（最後一位是檢查碼）", "EAN-13 needs 12 or 13 digits (the last one is a check digit)")
        BarcodeFormat.EAN_8 -> L.t("EAN-8 要 7 或 8 位數字（最後一位是檢查碼）", "EAN-8 needs 7 or 8 digits (the last one is a check digit)")
        BarcodeFormat.UPC_A -> L.t("UPC-A 要 11 或 12 位數字（最後一位是檢查碼）", "UPC-A needs 11 or 12 digits (the last one is a check digit)")
        BarcodeFormat.ITF -> L.t("ITF 只能用數字，而且要偶數位", "ITF takes digits only, and an even number of them")
        BarcodeFormat.CODE_39 -> L.t("Code 39 只能用數字、大寫英文和 - . \$ / + %", "Code 39 takes digits, capital letters and - . \$ / + % only")
        BarcodeFormat.CODABAR -> L.t("Codabar 只能用數字和 - \$ : / . +（頭尾可以加 A～D）", "Codabar takes digits and - \$ : / . + only (A–D allowed at both ends)")
        BarcodeFormat.CODE_128 -> L.t("Code 128 只能用英數字和半形符號，不能有中文", "Code 128 takes letters, digits and ASCII symbols only")
        else -> L.t("內容太長，放不進 QR Code", "Too long to fit in a QR Code")
    }

    /**
     * 條碼的最小矩陣（含左右靜區）。號碼不合格式就丟 IllegalArgumentException，訊息可以直接給使用者看。
     * 一維條碼的矩陣高度是 1，畫的時候再拉高。
     */
    fun encode(v: String, format: BarcodeFormat): BitMatrix {
        require(v.isNotEmpty()) { L.t("請輸入號碼", "Enter the number") }
        require(v.length <= if (format == BarcodeFormat.QR_CODE) 500 else 80) { L.t("號碼太長", "The number is too long") }
        // Code 39 遇到小寫或其他字，ZXing 會自動改成延伸模式，一般收銀掃描器讀不懂，所以先擋下來
        require(format != BarcodeFormat.CODE_39 || v.all { it in CODE39_CHARS }) { rule(format) }
        val hints = mutableMapOf<EncodeHintType, Any>()
        // 一維條碼左右各留 11 個模組的空白（靜區；EAN 左邊至少要 11）。ZXing 的 MARGIN 是左右加起來，預設只有 10
        if (format != BarcodeFormat.QR_CODE) hints[EncodeHintType.MARGIN] = 22
        // 只有非 ASCII 才指定 UTF-8：指定了 QR Code 會多一段 ECI 標記，少數掃描器不認得
        if (v.any { it.code > 126 }) hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        return try {
            MultiFormatWriter().encode(v, format, 0, 0, hints)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(rule(format), e)
        } catch (e: WriterException) {
            throw IllegalArgumentException(rule(format), e)
        }
    }

    /**
     * 一維條碼橫向拉成剛好 w 像素（true＝黑條）：每個模組佔 ⌊w／模組數⌋ 或多 1 像素，
     * 粗細比例不變，掃描器照樣讀；w 比模組數還小就用模組數，免得漏掉線條。
     */
    fun stretch(m: BitMatrix, w: Int): BooleanArray {
        val n = maxOf(w, m.width)
        return BooleanArray(n) { x -> m[x * m.width / n, 0] }
    }

    /**
     * 畫成點陣圖。
     * - 一維條碼：寬度撐滿 w（見 stretch），高度 h。
     * - QR Code：每個模組用整數倍像素放大，取放得進 w×h 的最大正方形，模組才方正銳利。
     */
    fun bitmap(m: BitMatrix, w: Int, h: Int, ink: Int, paper: Int): Bitmap {
        val px: IntArray
        val bw: Int
        val bh: Int
        if (m.height > 1) {
            val s = maxOf(1, minOf(w / m.width, h / m.height))
            bw = m.width * s
            bh = m.height * s
            px = IntArray(bw * bh) { i -> if (m[i % bw / s, i / bw / s]) ink else paper }
        } else {
            val row = stretch(m, w).let { r -> IntArray(r.size) { x -> if (r[x]) ink else paper } }
            bw = row.size
            bh = maxOf(1, h)
            px = IntArray(bw * bh)
            for (y in 0 until bh) System.arraycopy(row, 0, px, y * bw, bw)
        }
        // 黑白不需要透明度，RGB_565 只佔一半記憶體（小工具傳圖片有大小上限）
        return Bitmap.createBitmap(px, bw, bh, Bitmap.Config.RGB_565)
    }
}
