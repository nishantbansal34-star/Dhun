package com.nishant.dhun

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Everything Dhun remembers: likes, playlists, saved YouTube tracks, API key. Kept on the phone only. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("dhun", Context.MODE_PRIVATE)

    val liked = mutableStateListOf<Long>()
    val playlists = mutableStateListOf<Playlist>()
    val savedVideos = mutableStateListOf<YtVideo>()
    var apiKey by mutableStateOf(prefs.getString("apiKey", "") ?: "")
        private set

    init {
        runCatching {
            JSONArray(prefs.getString("liked", null) ?: "[]").let { a ->
                for (i in 0 until a.length()) liked += a.getLong(i)
            }
            JSONArray(prefs.getString("playlists", null) ?: "[]").let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val ids = o.getJSONArray("ids")
                    playlists += Playlist(o.getString("name"), List(ids.length()) { ids.getLong(it) })
                }
            }
            JSONArray(prefs.getString("videos", null) ?: "[]").let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    savedVideos += YtVideo(o.getString("id"), o.getString("title"), o.optString("channel"), o.optString("thumb"))
                }
            }
        }
    }

    private fun save() {
        val p = JSONArray()
        playlists.forEach { pl ->
            p.put(JSONObject().put("name", pl.name).put("ids", JSONArray(pl.songIds)))
        }
        val v = JSONArray()
        savedVideos.forEach {
            v.put(JSONObject().put("id", it.id).put("title", it.title).put("channel", it.channel).put("thumb", it.thumb))
        }
        prefs.edit()
            .putString("liked", JSONArray(liked.toList()).toString())
            .putString("playlists", p.toString())
            .putString("videos", v.toString())
            .putString("apiKey", apiKey)
            .apply()
    }

    fun updateApiKey(key: String) { apiKey = key.trim(); save() }

    fun isLiked(id: Long) = id in liked
    fun toggleLike(id: Long) { if (id in liked) liked.remove(id) else liked.add(0, id); save() }

    fun createPlaylist(name: String) {
        if (name.isBlank() || playlists.any { it.name == name.trim() }) return
        playlists += Playlist(name.trim(), emptyList()); save()
    }
    fun deletePlaylist(name: String) { playlists.removeAll { it.name == name }; save() }
    fun addToPlaylist(name: String, songId: Long) {
        val i = playlists.indexOfFirst { it.name == name }
        if (i < 0 || songId in playlists[i].songIds) return
        playlists[i] = playlists[i].copy(songIds = playlists[i].songIds + songId); save()
    }
    fun removeFromPlaylist(name: String, songId: Long) {
        val i = playlists.indexOfFirst { it.name == name }
        if (i < 0) return
        playlists[i] = playlists[i].copy(songIds = playlists[i].songIds - songId); save()
    }

    fun isSaved(v: YtVideo) = savedVideos.any { it.id == v.id }
    fun toggleSaved(v: YtVideo) {
        if (isSaved(v)) savedVideos.removeAll { it.id == v.id } else savedVideos.add(0, v)
        save()
    }
}
