package com.lot.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ---------------- (de)serializing creations. everything coming back is clamped, since server data is untrusted ----------------

fun CustomMode.toJson(): JSONObject = JSONObject()
    .put("id", id).put("n", name).put("t", template).put("col", color).put("h", host).put("cp", catchphrase).put("p", price)
    .put("pool", pool).put("picks", picks).put("draws", draws).put("seg", segments).put("flips", flips)
    .put("dice", dice).put("cups", cups).put("swaps", swaps).put("runners", runners).put("rt", runnerType)
    .put("boxes", boxes).put("bombs", bombs).put("by", author)

fun modeFromJson(m: JSONObject): CustomMode {
    val pool = m.optInt("pool", 20).coerceIn(10, 50)
    val picks = m.optInt("picks", 3).coerceIn(1, 6)
    val boxes = m.optInt("boxes", 9).coerceIn(4, 12)
    return CustomMode(
        id = m.optLong("id"),
        name = m.optString("n").take(40).ifBlank { "untitled" },
        template = m.optString("t", "draw").let { if (it in ModeTemplates) it else "draw" },
        color = m.optLong("col", 0xFFFF5C8A),
        host = m.optString("h", "host").let { if (it in listOf("host", "granny", "cashier", "commissioner", "raccoon", "pigeon", "you")) it else "host" },
        catchphrase = m.optString("cp").take(60).ifBlank { "welcome to my game!" },
        price = m.optInt("p", 10).coerceIn(1, 1000),
        pool = pool,
        picks = picks,
        draws = m.optInt("draws", 5).coerceIn(picks, pool - 1).coerceAtMost(10),
        segments = (m.optInt("seg", 8).coerceIn(4, 12) / 2) * 2,
        flips = m.optInt("flips", 3).coerceIn(1, 8),
        dice = m.optInt("dice", 2).coerceIn(1, 3),
        cups = m.optInt("cups", 3).coerceIn(3, 6),
        swaps = m.optInt("swaps", 6).coerceIn(3, 20),
        runners = m.optInt("runners", 4).coerceIn(3, 6),
        runnerType = m.optString("rt", "pigeon").let { if (it in RunnerTypes) it else "pigeon" },
        boxes = boxes,
        bombs = m.optInt("bombs", 2).coerceIn(1, boxes - 2),
        author = m.optString("by").take(24),
    )
}

fun CustomTicket.toJson(): JSONObject = JSONObject()
    .put("id", id).put("n", name).put("p", price).put("c", chance.toDouble())
    .put("s", JSONArray(syms.map { it.name })).put("pa", paper).put("ac", accent)

fun ticketFromJson(t: JSONObject): CustomTicket? {
    val syms = t.optJSONArray("s")?.let { sa -> (0 until sa.length()).mapNotNull { j -> Sym.entries.firstOrNull { it.name == sa.optString(j) } } }
        ?: return null
    if (syms.size != 5 || syms.distinct().size != 5) return null
    return CustomTicket(
        t.optLong("id"), t.optString("n").take(40).ifBlank { "untitled" }, t.optInt("p", 10).coerceIn(1, 1000),
        t.optDouble("c", 0.3).toFloat().coerceIn(0.1f, 0.5f), syms, t.optLong("pa", 0xFFFFE08A), t.optLong("ac", 0xFFFF5C8A),
    )
}

// ---------------- lottery locker: shared creations on the lot server ----------------

private const val API = "https://wmdqjnelwpsdqzfgpiyq.supabase.co/rest/v1"
// publishable key: safe to ship in the app, the database only allows reading and the locker_* functions
private const val KEY = "sb_publishable_WloaMEIHG6QLWeyMN4jTSg_jMq0iTt5"

data class LockerItem(
    val id: String,
    val kind: String,
    val name: String,
    val author: String,
    val data: JSONObject,
    val downloads: Int,
    val likes: Int,
    val createdAt: String,
)

class LockerException(msg: String) : Exception(msg)

object Locker {
    private suspend fun call(method: String, path: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val c = URL("$API/$path").openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("apikey", KEY)
            c.setRequestProperty("Authorization", "Bearer $KEY")
            c.setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                c.doOutput = true
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                throw LockerException(msg?.takeIf { it.isNotBlank() } ?: "server said $code")
            }
            text
        } catch (e: LockerException) {
            throw e
        } catch (e: Exception) {
            throw LockerException("can't reach the locker. check your internet.")
        } finally {
            c.disconnect()
        }
    }

    suspend fun browse(kind: String?, sort: String, search: String = ""): List<LockerItem> {
        val q = StringBuilder("locker_items?select=id,kind,name,author,data,downloads,likes,created_at&limit=60")
        if (kind != null) q.append("&kind=eq.").append(kind)
        if (search.isNotBlank()) q.append("&name=ilike.*").append(java.net.URLEncoder.encode(search.trim().replace("*", ""), "UTF-8")).append("*")
        q.append("&order=").append(if (sort == "popular") "likes.desc,downloads.desc" else "created_at.desc")
        val arr = JSONArray(call("GET", q.toString()))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LockerItem(
                o.optString("id"), o.optString("kind"), o.optString("name").take(40), o.optString("author").take(24),
                o.optJSONObject("data") ?: JSONObject(), o.optInt("downloads"), o.optInt("likes"), o.optString("created_at"),
            )
        }
    }

    suspend fun publish(kind: String, name: String, author: String, data: JSONObject, secret: String): String {
        val body = JSONObject().put("p_kind", kind).put("p_name", name.take(40)).put("p_author", author.take(24))
            .put("p_data", data).put("p_secret", secret)
        return call("POST", "rpc/locker_publish", body.toString()).trim('"', ' ', '\n')
    }

    suspend fun mine(secret: String): Set<String> {
        val arr = JSONArray(call("POST", "rpc/locker_mine", JSONObject().put("p_secret", secret).toString()))
        return (0 until arr.length()).map { arr.optString(it) }.toSet()
    }

    suspend fun delete(id: String, secret: String): Boolean =
        call("POST", "rpc/locker_delete", JSONObject().put("p_id", id).put("p_secret", secret).toString()).trim() == "true"

    suspend fun bump(id: String, what: String) {
        runCatching { call("POST", "rpc/locker_bump", JSONObject().put("p_id", id).put("p_what", what).toString()) }
    }
}
