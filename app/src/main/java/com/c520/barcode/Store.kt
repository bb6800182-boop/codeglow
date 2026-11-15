package com.c520.barcode

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 一張條碼。kind 是發票載具或會員卡；format 是 ZXing 的 BarcodeFormat 名稱；icon＝有沒有自訂圖示。 */
data class Card(
    val id: Long,
    val name: String,
    val kind: String,
    val value: String,
    val format: String,
    val icon: Boolean,
) {
    val carrier get() = kind == CARRIER

    companion object {
        const val CARRIER = "carrier"
        const val MEMBER = "member"
    }
}

/** 條碼清單存在 SharedPreferences（一個 JSON 陣列，順序＝App 清單與小工具的順序）；自訂圖示存在 files/icons/<id>.png。 */
object Store {
    private const val KEY = "cards"

    private fun prefs(c: Context) = c.getSharedPreferences("cards", Context.MODE_PRIVATE)

    fun all(c: Context): List<Card> {
        val arr = try {
            JSONArray(prefs(c).getString(KEY, null) ?: "[]")
        } catch (e: org.json.JSONException) {
            JSONArray()
        }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Card(
                id = o.optLong("id"),
                name = o.optString("name"),
                kind = o.optString("kind", Card.MEMBER),
                value = o.optString("value"),
                format = o.optString("format"),
                icon = o.optBoolean("icon"),
            ).takeIf { it.id > 0 && it.value.isNotEmpty() && it.format.isNotEmpty() }
        }
    }

    fun get(c: Context, id: Long): Card? = if (id <= 0) null else all(c).firstOrNull { it.id == id }

    /** 新增或更新：同一個 id 原位取代，新的加在最後。 */
    fun put(c: Context, card: Card) {
        val list = all(c).toMutableList()
        val i = list.indexOfFirst { it.id == card.id }
        if (i >= 0) list[i] = card else list += card
        write(c, list)
    }

    fun delete(c: Context, id: Long) {
        write(c, all(c).filterNot { it.id == id })
        iconFile(c, id).delete()
    }

    fun iconFile(c: Context, id: Long) = File(File(c.filesDir, "icons"), "$id.png")

    private fun write(c: Context, list: List<Card>) {
        val arr = JSONArray()
        for (k in list) {
            arr.put(
                JSONObject().put("id", k.id).put("name", k.name).put("kind", k.kind)
                    .put("value", k.value).put("format", k.format).put("icon", k.icon)
            )
        }
        // apply 在記憶體裡立刻生效，小工具（同一個程序）接著就讀得到
        prefs(c).edit().putString(KEY, arr.toString()).apply()
    }
}
