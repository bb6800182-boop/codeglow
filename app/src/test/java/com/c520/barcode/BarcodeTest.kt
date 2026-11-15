package com.c520.barcode

import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

class BarcodeTest {

    @Test
    fun carrierFormat() {
        assertTrue(Barcode.isCarrier("/ABC1234"))
        assertTrue(Barcode.isCarrier("/A.+-9Z0"))
        assertFalse(Barcode.isCarrier("ABC12345")) // 沒有 /
        assertFalse(Barcode.isCarrier("/abc1234")) // 小寫要先轉大寫
        assertFalse(Barcode.isCarrier("/ABC123")) // 少一碼
        assertFalse(Barcode.isCarrier("/ABC12345")) // 多一碼
        assertFalse(Barcode.isCarrier("/ABC 123")) // 空白不行
    }

    @Test
    fun normalize() {
        assertEquals("/ABC1234", Barcode.normalize(" /abc1234 ", BarcodeFormat.CODE_39, carrier = true))
        assertEquals("2000123456789", Barcode.normalize(" 2000 1234 5678 9 ", BarcodeFormat.CODE_128))
        assertEquals("會員 A1", Barcode.normalize("  會員 A1 ", BarcodeFormat.QR_CODE)) // QR 保留中間的空白
        // 少打檢查碼就補上
        assertEquals("4006381333931", Barcode.normalize("400638133393", BarcodeFormat.EAN_13))
        assertEquals("96385074", Barcode.normalize("9638507", BarcodeFormat.EAN_8))
        assertEquals("036000291452", Barcode.normalize("03600029145", BarcodeFormat.UPC_A))
        // 位數本來就夠的不動（檢查碼錯了交給 encode 報錯）
        assertEquals("4006381333932", Barcode.normalize("4006381333932", BarcodeFormat.EAN_13))
    }

    @Test
    fun checkDigit() {
        assertTrue(Barcode.gtinOk("4006381333931"))
        assertTrue(Barcode.gtinOk("96385074"))
        assertTrue(Barcode.gtinOk("036000291452"))
        assertFalse(Barcode.gtinOk("4006381333932"))
        assertFalse(Barcode.gtinOk("40063813339A1"))
    }

    @Test
    fun guess() {
        assertEquals(BarcodeFormat.EAN_13, Barcode.guess("4006381333931"))
        assertEquals(BarcodeFormat.CODE_128, Barcode.guess("4006381333932")) // 檢查碼不對
        assertEquals(BarcodeFormat.CODE_128, Barcode.guess("036000291452")) // 12 位不猜 UPC-A
        assertEquals(BarcodeFormat.CODE_128, Barcode.guess("PX-0012345"))
        assertEquals(BarcodeFormat.QR_CODE, Barcode.guess("會員123"))
    }

    @Test
    fun encodeOneDimensional() {
        val m = Barcode.encode("/ABC1234", BarcodeFormat.CODE_39)
        assertEquals(1, m.height)
        // 10 個字（含頭尾 *）× 13 模組 − 1 ＝ 129，加左右靜區各 11
        assertEquals(151, m.width)
        assertFalse(m[10, 0]) // 靜區是空白
        assertTrue(m[11, 0]) // 起始字元 * 的第一條
        Barcode.encode("4006381333931", BarcodeFormat.EAN_13)
        Barcode.encode("2000123456789", BarcodeFormat.CODE_128)
        Barcode.encode("12345678", BarcodeFormat.ITF)
    }

    @Test
    fun stretchFillsWidth() {
        val m = Barcode.encode("/ABC1234", BarcodeFormat.CODE_39) // 151 模組
        val w = 1005 // Pixel 7 Pro 上拉到最寬的小工具（扣掉細框）
        val r = Barcode.stretch(m, w)
        assertEquals(w, r.size)
        // 每一段黑條、白條都在，寬度跟模組數成比例，最多差 1 像素
        val modules = runs(BooleanArray(m.width) { m[it, 0] })
        val px = runs(r)
        assertEquals(modules.size, px.size)
        for (i in modules.indices) {
            val expect = modules[i] * w.toDouble() / m.width
            assertTrue("第 $i 段 ${px[i]}px，應該約 ${expect}px", abs(px[i] - expect) <= 1.0)
        }
        assertEquals(m.width, Barcode.stretch(m, 100).size) // 比模組數還窄：用模組數，不漏線條
    }

    /** 連續同色的長度。 */
    private fun runs(a: BooleanArray): List<Int> {
        val out = mutableListOf<Int>()
        var n = 0
        for (i in a.indices) {
            n++
            if (i == a.lastIndex || a[i + 1] != a[i]) {
                out += n
                n = 0
            }
        }
        return out
    }

    @Test
    fun encodeQr() {
        val m = Barcode.encode("會員 A1", BarcodeFormat.QR_CODE)
        assertTrue(m.height > 1)
        assertEquals(m.width, m.height)
    }

    @Test
    fun encodeRejectsBadInput() {
        bad("4006381333932", BarcodeFormat.EAN_13) // 檢查碼錯
        bad("12345", BarcodeFormat.EAN_13) // 位數不對
        bad("abc", BarcodeFormat.CODE_39) // 小寫（不讓 ZXing 偷偷改延伸模式）
        bad("123", BarcodeFormat.ITF) // 奇數位
        bad("會員", BarcodeFormat.CODE_128) // 中文
        bad("", BarcodeFormat.CODE_128)
        bad("1".repeat(81), BarcodeFormat.CODE_128)
    }

    private fun bad(v: String, f: BarcodeFormat) {
        try {
            Barcode.encode(v, f)
            fail("應該要拒絕 $f「$v」")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }
}
