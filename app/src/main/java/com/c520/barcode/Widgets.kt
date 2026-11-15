package com.c520.barcode

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.Toast
import com.google.zxing.BarcodeFormat

/**
 * 單一條碼小工具：條碼撐滿整個小工具；點右下的太陽調到最亮，最亮時點條碼（或再點太陽）恢復。
 * 不用連點兩下：Nova 等桌面的「點兩下鎖螢幕」手勢會吃掉第二下。
 */
class SingleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Widgets.single(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) =
        Widgets.single(context, appWidgetManager, appWidgetId) // 大小變了，條碼照新大小重畫

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = Widgets.forget(context, appWidgetIds)
}

/** 多店家小工具：店家圖示一覽；點圖示只留那張條碼並調到最亮，點條碼恢復亮度、收回一覽。 */
class GridWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Widgets.grid(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) =
        Widgets.grid(context, appWidgetManager, appWidgetId)

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = Widgets.forget(context, appWidgetIds)
}

/**
 * 單一圖示小工具（4×1）：平常背景透明，只有一個店家圖示（靠左／置中／靠右）；
 * 點圖示換成白底條碼並調到最亮，點條碼恢復亮度、收回成圖示。
 */
class IconWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Widgets.icon(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) =
        Widgets.icon(context, appWidgetManager, appWidgetId) // 展開中改大小，條碼照新大小重畫

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = Widgets.forget(context, appWidgetIds)
}

/** 小工具上的點擊。只收自己發的 PendingIntent（manifest 不對外）。 */
class TapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        when (intent.action) {
            Widgets.TAP_SUN -> Widgets.sun(context, id)
            Widgets.TAP_ICON -> Widgets.open(context, id, intent.getLongExtra(Widgets.EXTRA_CARD, -1))
            Widgets.TAP_CODE -> Widgets.restoreAll(context)
        }
    }
}

/**
 * 多店家小工具的圖示清單：每格一個店家圖示＋名稱，點了把卡片 id 填進範本 PendingIntent。
 * 不用 Android 12 的 RemoteCollectionItems：那要一次把所有圖示塞進同一包，條碼一多會超過傳輸上限；這裡一格一格給。
 */
class GridService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Items(applicationContext)

    private class Items(private val c: Context) : RemoteViewsFactory {
        private var cards = emptyList<Card>()

        override fun onCreate() {
            cards = Store.all(c)
        }

        override fun onDataSetChanged() {
            cards = Store.all(c)
        }

        override fun onDestroy() {}

        override fun getCount() = cards.size

        override fun getViewAt(position: Int): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget_grid_item)
            val card = cards.getOrNull(position) ?: return v
            v.setImageViewBitmap(R.id.item_icon, Icons.of(c, card))
            v.setTextViewText(R.id.item_name, card.name)
            v.setContentDescription(R.id.item, "${card.name}，點一下顯示條碼")
            v.setOnClickFillInIntent(R.id.item, Intent().putExtra(Widgets.EXTRA_CARD, card.id))
            return v
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount() = 1

        override fun getItemId(position: Int) = cards.getOrNull(position)?.id ?: position.toLong()

        override fun hasStableIds() = true
    }
}

/** 三種小工具的畫面與狀態。 */
object Widgets {
    const val TAP_SUN = "com.c520.barcode.TAP_SUN"
    const val TAP_ICON = "com.c520.barcode.TAP_ICON"
    const val TAP_CODE = "com.c520.barcode.TAP_CODE"
    const val EXTRA_CARD = "card"

    // 單一圖示小工具的圖示位置，順序同 widget_icon.xml 的三格、選擇畫面的選項
    const val ALIGN_START = 0
    const val ALIGN_CENTER = 1
    const val ALIGN_END = 2
    private val ICON_SLOTS = intArrayOf(R.id.icon_start, R.id.icon_center, R.id.icon_end)

    // 小工具找不到大小時的預設值（dp），同 widget_*_info.xml 的 minWidth／minHeight
    private const val DEFAULT_W_DP = 250
    private const val DEFAULT_H_DP = 110

    private const val IMMUTABLE = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    // 狀態：card_<id> 單一條碼／單一圖示小工具顯示哪張；align_<id> 單一圖示的位置；
    // bright_<id> 單一條碼小工具調亮中；open_<id> 多店家或單一圖示小工具展開哪張
    private fun state(c: Context) = c.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun align(c: Context, id: Int) = state(c).getInt("align_$id", ALIGN_CENTER)

    /** 單一條碼或單一圖示小工具選好要顯示的條碼（單一圖示還有位置）。長按重新選時，如果正在調亮，先恢復。 */
    fun setCard(c: Context, id: Int, card: Long, align: Int? = null) {
        val s = state(c)
        if (s.getBoolean("bright_$id", false) || s.contains("open_$id")) restoreAll(c)
        val e = s.edit().putLong("card_$id", card)
        if (align != null) e.putInt("align_$id", align)
        e.apply()
        render(c, AppWidgetManager.getInstance(c), id)
    }

    /** 依小工具種類重畫；已經移走的小工具什麼都不做。 */
    private fun render(c: Context, m: AppWidgetManager, id: Int) {
        when (m.getAppWidgetInfo(id)?.provider?.className) {
            SingleWidget::class.java.name -> single(c, m, id)
            GridWidget::class.java.name -> grid(c, m, id)
            IconWidget::class.java.name -> icon(c, m, id)
        }
    }

    /** 條碼清單變了（新增、修改、刪除）：所有小工具重畫。 */
    fun refreshAll(c: Context) {
        val m = AppWidgetManager.getInstance(c) ?: return
        for (id in m.getAppWidgetIds(ComponentName(c, GridWidget::class.java))) grid(c, m, id)
        for (id in m.getAppWidgetIds(ComponentName(c, SingleWidget::class.java))) single(c, m, id)
        for (id in m.getAppWidgetIds(ComponentName(c, IconWidget::class.java))) icon(c, m, id)
    }

    /**
     * 單一圖示小工具。平常：透明底，只有圖示（位置照 align_<id>），點圖示展開。
     * 展開：換成單一條碼的白底版面（沒有太陽），點條碼恢復亮度、收回。還沒選或那張被刪了：「選擇條碼」，點了開選擇畫面。
     */
    fun icon(c: Context, m: AppWidgetManager, id: Int) {
        val s = state(c)
        val card = Store.get(c, s.getLong("card_$id", -1))
        if (s.contains("open_$id")) {
            if (card == null) {
                restoreAll(c) // 展開中的那張被刪了：恢復亮度並收回（restoreAll 會重畫這個小工具）
                return
            }
            val v = RemoteViews(c.packageName, R.layout.widget_single)
            fill(c, m, id, v, card, sun = false, "點一下恢復亮度，收回成圖示")
            v.setOnClickPendingIntent(R.id.root, tap(c, TAP_CODE, id))
            m.updateAppWidget(id, v)
            return
        }
        // 同 single()：桌面會把新畫面套在舊的上面，三格和提示字的可見度都要明講
        val v = RemoteViews(c.packageName, R.layout.widget_icon)
        val slot = ICON_SLOTS[align(c, id).coerceIn(ICON_SLOTS.indices)]
        for (i in ICON_SLOTS) v.setViewVisibility(i, if (card != null && i == slot) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.icon_empty, if (card == null) View.VISIBLE else View.GONE)
        if (card == null) {
            v.setOnClickPendingIntent(R.id.icon_empty, PendingIntent.getActivity(c, id, PickActivity.intent(c, id), IMMUTABLE))
        } else {
            v.setImageViewBitmap(slot, Icons.of(c, card))
            v.setContentDescription(slot, "${card.name}，點一下顯示條碼並調到最亮")
            v.setOnClickPendingIntent(slot, tap(c, TAP_ICON, id, card = card.id))
        }
        m.updateAppWidget(id, v)
    }

    fun single(c: Context, m: AppWidgetManager, id: Int) {
        val s = state(c)
        val card = Store.get(c, s.getLong("card_$id", -1))
        val bright = s.getBoolean("bright_$id", false)
        if (card == null && bright) {
            restoreAll(c) // 調亮中的那張被刪了：恢復亮度（restoreAll 會重畫這個小工具）
            return
        }
        // 桌面會把新的 RemoteViews 套在舊畫面上（同一個版面就不重建），所以每種狀態都要明講，不能靠版面預設值
        val v = RemoteViews(c.packageName, R.layout.widget_single)
        if (card == null) {
            // 還沒選、或那張被刪了：顯示提示字，點了開選擇畫面
            v.setViewVisibility(R.id.code, View.GONE)
            v.setViewVisibility(R.id.empty, View.VISIBLE)
            v.setContentDescription(R.id.root, null)
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, id, PickActivity.intent(c, id), IMMUTABLE))
        } else {
            fill(c, m, id, v, card, sun = true, if (bright) "已調到最亮，點條碼恢復亮度" else "點右下的太陽調到最亮")
            v.setImageViewResource(R.id.code_sun, if (bright) R.drawable.ic_sun_on else R.drawable.ic_sun)
            v.setContentDescription(R.id.code_sun, if (bright) "恢復亮度" else "調到最亮")
            v.setOnClickPendingIntent(R.id.code_sun, tap(c, TAP_SUN, id))
            // 平常點條碼不做事（免得誤觸調亮）；調亮中點條碼恢復
            v.setOnClickPendingIntent(R.id.root, if (bright) tap(c, TAP_CODE, id) else null)
        }
        m.updateAppWidget(id, v)
    }

    fun grid(c: Context, m: AppWidgetManager, id: Int) {
        val open = state(c).getLong("open_$id", -1)
        val card = Store.get(c, open)
        if (open > 0 && card == null) {
            restoreAll(c) // 打開的那張被刪了：恢復亮度並收回（restoreAll 會重畫這個小工具）
            return
        }
        if (card != null) {
            // 打開一張：換成單一條碼的版面，其他圖示都不顯示；點條碼恢復亮度、收回
            val v = RemoteViews(c.packageName, R.layout.widget_single)
            fill(c, m, id, v, card, sun = false, "點一下恢復亮度，回到店家一覽")
            v.setOnClickPendingIntent(R.id.root, tap(c, TAP_CODE, id))
            m.updateAppWidget(id, v)
            return
        }
        val v = RemoteViews(c.packageName, R.layout.widget_grid)
        @Suppress("DEPRECATION") // 原因見 GridService 的說明
        v.setRemoteAdapter(R.id.grid, Intent(c, GridService::class.java))
        v.setEmptyView(R.id.grid, R.id.grid_empty)
        v.setPendingIntentTemplate(R.id.grid, tap(c, TAP_ICON, id, mutable = true))
        v.setOnClickPendingIntent(R.id.grid_empty, PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), IMMUTABLE))
        m.updateAppWidget(id, v)
        @Suppress("DEPRECATION") // 跟 setRemoteAdapter 同一組，原因見 GridService 的說明
        m.notifyAppWidgetViewDataChanged(id, R.id.grid) // 清單可能在打開期間改過，收回時一律重讀
    }

    /** 單一條碼小工具的太陽被點：調到最亮；已經最亮就恢復。 */
    fun sun(c: Context, id: Int) {
        val s = state(c)
        if (s.getBoolean("bright_$id", false)) {
            restoreAll(c)
            return
        }
        if (Store.get(c, s.getLong("card_$id", -1)) == null) return // 那張剛被刪掉，畫面還沒換
        if (!boost(c)) return
        s.edit().putBoolean("bright_$id", true).apply()
        single(c, AppWidgetManager.getInstance(c), id) // 太陽換成實心、點條碼改成恢復
    }

    /** 多店家小工具點了某個店家、或單一圖示小工具點了圖示：只顯示那張條碼，調到最亮。 */
    fun open(c: Context, id: Int, card: Long) {
        if (Store.get(c, card) == null) return
        state(c).edit().putLong("open_$id", card).apply()
        render(c, AppWidgetManager.getInstance(c), id)
        boost(c)
    }

    /** 恢復原本的亮度，所有小工具回到平常的樣子。 */
    fun restoreAll(c: Context) {
        Bright.restore(c)
        val s = state(c)
        val keys = s.all.keys.filter { it.startsWith("bright_") || it.startsWith("open_") }
        val e = s.edit()
        for (k in keys) e.remove(k)
        e.apply()
        val m = AppWidgetManager.getInstance(c)
        for (k in keys) k.substringAfter('_').toIntOrNull()?.let { render(c, m, it) }
    }

    /** 小工具被移走：清掉它的狀態；如果是它調亮的，先恢復亮度，免得螢幕一直維持最亮。 */
    fun forget(c: Context, ids: IntArray) {
        val s = state(c)
        if (ids.any { s.getBoolean("bright_$it", false) || s.contains("open_$it") }) restoreAll(c)
        val e = s.edit()
        for (id in ids) for (k in listOf("card_", "align_", "bright_", "open_")) e.remove("$k$id")
        e.apply()
    }

    private fun boost(c: Context): Boolean {
        if (Bright.boost(c)) return true
        Toast.makeText(c, R.string.need_permission, Toast.LENGTH_LONG).show()
        return false
    }

    /** 條碼撐滿小工具＋下面一行「名稱（小字）　號碼」；sun＝下面那行右邊有太陽鈕。 */
    private fun fill(c: Context, m: AppWidgetManager, id: Int, v: RemoteViews, card: Card, sun: Boolean, hint: String) {
        v.setViewVisibility(R.id.code, View.VISIBLE)
        v.setViewVisibility(R.id.empty, View.GONE)
        v.setViewVisibility(R.id.code_sun, if (sun) View.VISIBLE else View.GONE)
        v.setTextViewText(R.id.code_name, card.name)
        v.setTextViewText(R.id.code_value, card.value)
        val (w, h) = codeArea(c, m, id, sun)
        var img = View.NO_ID
        try {
            val code = Barcode.encode(card.value, Barcode.formatOf(card.format) ?: BarcodeFormat.CODE_128)
            val qr = code.height > 1
            img = if (qr) R.id.code_qr else R.id.code_img
            // 一維條碼每一列都一樣：只畫一列，由 fitXY 拉滿小工具（圖小、傳得快，桌面回報的高度不準也沒關係）
            v.setImageViewBitmap(img, Barcode.bitmap(code, w, if (qr) h else 1, c.getColor(R.color.barcode_ink), c.getColor(R.color.barcode_paper)))
        } catch (e: IllegalArgumentException) {
            // 存檔時檢查過，照理不會到這裡；真的發生就只顯示號碼（兩張圖都藏起來，免得留著上一張）
        }
        v.setViewVisibility(R.id.code_img, if (img == R.id.code_img) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.code_qr, if (img == R.id.code_qr) View.VISIBLE else View.GONE)
        v.setContentDescription(R.id.root, "${card.name} 條碼，號碼 ${card.value}。$hint")
    }

    /**
     * 條碼可用的大小（px）：直向用最小寬×最大高、橫向反過來（AppWidgetManager 的慣例），扣掉細框、上方留白和下面那行。
     * 桌面回報的大小不一定準，差一點由 fitXY／fitCenter 補；這裡算得越接近，線條越銳利。
     */
    private fun codeArea(c: Context, m: AppWidgetManager, id: Int, sun: Boolean): Pair<Int, Int> {
        val o = m.getAppWidgetOptions(id)
        val land = c.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val wDp = o.getInt(if (land) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val hDp = o.getInt(if (land) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val r = c.resources
        val d = r.displayMetrics.density
        val edge = r.getDimensionPixelSize(R.dimen.hairline) * 2
        // 下面那行：一行字＋上下間距，有太陽鈕就至少是它的高度（字的大小已含系統字型縮放）
        val text = r.getDimension(R.dimen.text_caption) * 1.4f + r.getDimension(R.dimen.space_xs) * 2
        val row = if (sun) maxOf(text, r.getDimension(R.dimen.touch_sm)) else text
        val w = (if (wDp > 0) wDp else DEFAULT_W_DP) * d - edge
        val h = (if (hDp > 0) hDp else DEFAULT_H_DP) * d - edge - r.getDimension(R.dimen.space_sm) - row
        return w.toInt().coerceAtLeast(1) to h.toInt().coerceAtLeast(1)
    }

    /** 小工具點擊用的 PendingIntent，明確指定送給 TapReceiver；card 是要展開哪張（多店家由清單每格填入）。 */
    private fun tap(c: Context, action: String, id: Int, mutable: Boolean = false, card: Long? = null): PendingIntent {
        val i = Intent(c, TapReceiver::class.java)
            .setAction(action)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND) // 使用者點的，走前景佇列反應比較快
        if (card != null) i.putExtra(EXTRA_CARD, card)
        // 圖示清單的範本要可變，系統才能把每格的卡片 id 填進去；元件已明確指定，不會被改送到別處
        val f = PendingIntent.FLAG_UPDATE_CURRENT or if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(c, id, i, f)
    }
}
