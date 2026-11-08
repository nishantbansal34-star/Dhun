package com.nishant.dhun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController

/** Compose-friendly mirror of the background player's state. */
class PlayerUi {
    var controller by mutableStateOf<MediaController?>(null)
        private set
    var current by mutableStateOf<MediaItem?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    /** Called whenever local playback starts, so YouTube can pause. */
    var onLocalPlay: () -> Unit = {}

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    private fun sync(p: Player) {
        current = p.currentMediaItem
        isPlaying = p.isPlaying
        shuffle = p.shuffleModeEnabled
        repeat = p.repeatMode
    }

    fun attach(c: MediaController) {
        controller = c
        c.addListener(listener)
        sync(c)
    }

    fun detach() {
        controller?.removeListener(listener)
        controller = null
    }

    fun play(songs: List<Song>, start: Int, shuffled: Boolean = false) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val startAt = if (shuffled) songs.indices.random() else start.coerceIn(songs.indices)
        c.setMediaItems(songs.map { it.toMediaItem() }, startAt, 0L)
        c.shuffleModeEnabled = shuffled
        c.prepare()
        c.play()
        onLocalPlay()
    }

    fun addNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) { play(listOf(song), 0); return }
        c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else { c.play(); onLocalPlay() }
    }

    fun pause() { controller?.pause() }
    fun next() { controller?.seekToNext() }
    fun prev() { controller?.seekToPrevious() }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
}
