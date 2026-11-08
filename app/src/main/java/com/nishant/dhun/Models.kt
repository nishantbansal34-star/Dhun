package com.nishant.dhun

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: Uri,
) {
    val artUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artUri)
                .build()
        )
        .build()
}

data class Playlist(val name: String, val songIds: List<Long>)

data class YtVideo(val id: String, val title: String, val channel: String, val thumb: String)

object LocalMusic {
    fun load(context: Context): List<Song> {
        val out = ArrayList<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        context.contentResolver.query(
            collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val tCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val arCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val alCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val aiCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val artist = c.getString(arCol)
                out += Song(
                    id = id,
                    title = c.getString(tCol) ?: "Unknown",
                    artist = if (artist == null || artist == "<unknown>") "Unknown artist" else artist,
                    album = c.getString(alCol) ?: "Unknown album",
                    albumId = c.getLong(aiCol),
                    duration = c.getLong(dCol),
                    uri = ContentUris.withAppendedId(collection, id),
                )
            }
        }
        return out
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
