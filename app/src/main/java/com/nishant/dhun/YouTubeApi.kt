package com.nishant.dhun

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Official YouTube Data API v3 search (needs your own free API key). */
object YouTubeApi {
    suspend fun search(query: String, key: String): List<YtVideo> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = URL(
            "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video" +
                "&videoEmbeddable=true&videoCategoryId=10&maxResults=25&q=$q&key=${URLEncoder.encode(key, "UTF-8")}"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP $code")
                throw IllegalStateException(Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY).toString())
            }
            val items = JSONObject(body).getJSONArray("items")
            List(items.length()) { i ->
                val it = items.getJSONObject(i)
                val sn = it.getJSONObject("snippet")
                val thumbs = sn.optJSONObject("thumbnails")
                val thumb = thumbs?.optJSONObject("high")?.optString("url")
                    ?: thumbs?.optJSONObject("medium")?.optString("url") ?: ""
                YtVideo(
                    id = it.getJSONObject("id").getString("videoId"),
                    title = decode(sn.getString("title")),
                    channel = decode(sn.optString("channelTitle")),
                    thumb = thumb,
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun decode(s: String) = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()

    /** Pulls the video id out of any YouTube / YT Music link. */
    fun idFromLink(text: String): String? {
        val patterns = listOf(
            Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
            Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""/(?:shorts|embed|live)/([A-Za-z0-9_-]{11})"""),
        )
        for (p in patterns) p.find(text)?.let { return it.groupValues[1] }
        val t = text.trim()
        return if (Regex("""^[A-Za-z0-9_-]{11}$""").matches(t)) t else null
    }
}
