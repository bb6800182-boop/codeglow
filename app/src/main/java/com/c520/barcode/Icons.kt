package com.c520.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlin.math.abs

/** 店家圖示：192px 正方形（畫面上是 48dp，留 4 倍解析度），用自訂圖片或名稱的第一個字。 */
object Icons {
    private const val SIZE = 192
    private val PALETTE = intArrayOf(R.color.icon_1, R.color.icon_2, R.color.icon_3, R.color.icon_4, R.color.icon_5, R.color.icon_6)

    fun of(c: Context, card: Card): Bitmap =
        (if (card.icon) BitmapFactory.decodeFile(Store.iconFile(c, card.id).path) else null) ?: letter(c, card.name)

    /** 沒選圖片：名稱第一個字（白色）＋依名稱固定的低彩度底色。 */
    fun letter(c: Context, name: String): Bitmap {
        val b = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = scaled(c, R.dimen.radius_md)
        p.color = c.getColor(PALETTE[abs(name.hashCode() % PALETTE.size)])
        cv.drawRoundRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), r, r, p)
        val t = name.trim()
        val first = if (t.isEmpty()) "?" else t.substring(0, t.offsetByCodePoints(0, 1))
        p.color = c.getColor(R.color.on_accent)
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        p.textSize = SIZE * 0.5f
        cv.drawText(first, SIZE / 2f, SIZE / 2f - (p.descent() + p.ascent()) / 2, p)
        return b
    }

    /** 使用者選的圖片：等比縮進白底圓角方塊（不裁切，寬的標誌也看得到全貌）。讀不到傳回 null。 */
    fun fromUri(c: Context, uri: Uri): Bitmap? {
        val src = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.contentResolver, uri)) { d, info, _ ->
                // 大照片先縮小再解碼，免得吃光記憶體；ImageDecoder 會照 EXIF 把照片轉正
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > SIZE * 2) d.setTargetSampleSize(longest / (SIZE * 2))
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // 要畫在一般 Canvas 上，不能用硬體點陣圖
            }
        } catch (e: Exception) {
            return null // 不是圖片、檔案壞掉、沒有讀取權限
        }
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        val r = scaled(c, R.dimen.radius_md)
        cv.save()
        cv.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()), r, r, Path.Direction.CW) })
        cv.drawColor(c.getColor(R.color.surface))
        val k = minOf(SIZE / src.width.toFloat(), SIZE / src.height.toFloat())
        val w = src.width * k
        val h = src.height * k
        cv.drawBitmap(src, null, RectF((SIZE - w) / 2, (SIZE - h) / 2, (SIZE + w) / 2, (SIZE + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        cv.restore()
        // 細框：白底的圖示放在白色小工具上才看得出邊界
        val line = scaled(c, R.dimen.hairline)
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = line
            color = c.getColor(R.color.border)
        }
        cv.drawRoundRect(line / 2, line / 2, SIZE - line / 2, SIZE - line / 2, r, r, frame)
        src.recycle()
        return out
    }

    /** 存成 PNG；先寫暫存檔再改名，寫到一半不會留下壞檔。 */
    fun save(c: Context, id: Long, b: Bitmap): Boolean {
        val f = Store.iconFile(c, id)
        val tmp = File(f.path + ".tmp")
        return try {
            f.parentFile?.mkdirs()
            val ok = tmp.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ok && tmp.renameTo(f)
        } catch (e: IOException) {
            tmp.delete()
            false
        }
    }

    /** 介面的尺寸換算到 192px 的圖上（圖示在畫面上是 R.dimen.icon 大）。 */
    private fun scaled(c: Context, dimen: Int) = c.resources.getDimension(dimen) / c.resources.getDimension(R.dimen.icon) * SIZE
}
