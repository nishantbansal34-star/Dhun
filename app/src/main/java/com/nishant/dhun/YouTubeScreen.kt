package com.nishant.dhun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

/** YouTube side: search results + a play queue, survives tab switches. */
class YtState {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<YtVideo>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var queue by mutableStateOf<List<YtVideo>>(emptyList())
        private set
    var index by mutableIntStateOf(-1)
        private set
    val current: YtVideo? get() = queue.getOrNull(index)

    var player: YouTubePlayer? = null
    private var pendingId: String? = null
    var onPlay: () -> Unit = {}

    fun attach(p: YouTubePlayer) {
        player = p
        val id = pendingId
        if (id != null) p.loadVideo(id, 0f) else current?.let { p.cueVideo(it.id, 0f) }
        pendingId = null
    }

    fun playFrom(list: List<YtVideo>, i: Int) { queue = list; index = i; load(list[i].id) }
    fun playLink(id: String) {
        queue = listOf(YtVideo(id, "Shared video", "", "https://i.ytimg.com/vi/$id/hqdefault.jpg"))
        index = 0; load(id)
    }
    fun next() { if (index + 1 < queue.size) { index++; load(queue[index].id) } }
    fun prev() { if (index > 0) { index--; load(queue[index].id) } }
    fun pause() { player?.pause() }

    private fun load(id: String) {
        error = null
        val p = player
        if (p == null) pendingId = id else p.loadVideo(id, 0f)
    }
}

@Composable
fun YouTubeScreen(store: Store, yt: YtState) {
    val scope = rememberCoroutineScope()
    var showSaved by remember { mutableStateOf(false) }
    var editKey by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }

    fun search() {
        val q = yt.query.trim()
        if (q.isEmpty() || store.apiKey.isBlank()) return
        showSaved = false
        scope.launch {
            yt.loading = true; yt.error = null
            try { yt.results = YouTubeApi.search(q, store.apiKey) }
            catch (e: Exception) { yt.error = "Search failed: ${e.message}" }
            yt.loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        val cur = yt.current
        if (cur != null) {
            YtPlayerView(yt)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cur.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (cur.channel.isNotEmpty()) Text(cur.channel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { yt.prev() }, enabled = yt.index > 0) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                IconButton(onClick = { yt.next() }, enabled = yt.index + 1 < yt.queue.size) { Icon(Icons.Filled.SkipNext, "Next") }
                IconButton(onClick = { store.toggleSaved(cur) }) {
                    Icon(if (store.isSaved(cur)) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Save",
                        tint = if (store.isSaved(cur)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("YouTube", Modifier.weight(1f))
                IconButton(onClick = { editKey = true }, modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Filled.Key, "API key") }
            }
        }
        yt.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (store.apiKey.isBlank()) {
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Turn on YouTube search", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Search needs a free YouTube API key from Google Cloud Console (APIs & Services → enable \"YouTube Data API v3\" → Credentials → Create API key). " +
                                    "Until then you can still paste or share any YouTube / YT Music link.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { editKey = true }) { Text("Enter API key") }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = yt.query, onValueChange = { yt.query = it },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        placeholder = { Text("Search songs on YouTube") },
                        singleLine = true, shape = RoundedCornerShape(28.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = link, onValueChange = { link = it },
                    leadingIcon = { Icon(Icons.Filled.Link, null) },
                    placeholder = { Text("Paste a YouTube link") },
                    singleLine = true, shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        val id = YouTubeApi.idFromLink(link)
                        if (id != null) { yt.playLink(id); link = "" } else yt.error = "That doesn't look like a YouTube link."
                    }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showSaved, onClick = { showSaved = false }, label = { Text("Results") })
                    FilterChip(selected = showSaved, onClick = { showSaved = true }, label = { Text("Saved (${store.savedVideos.size})") })
                }
                if (yt.loading) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
            val list = if (showSaved) store.savedVideos.toList() else yt.results
            items(list.size) { i ->
                val v = list[i]
                Row(
                    Modifier.fillMaxWidth().clickable { yt.playFrom(list, i) }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = v.thumb, contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            color = if (yt.current?.id == v.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(v.channel, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { store.toggleSaved(v) }) {
                        Icon(if (store.isSaved(v)) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Save")
                    }
                }
            }
        }
    }

    if (editKey) {
        var key by remember { mutableStateOf(store.apiKey) }
        AlertDialog(
            onDismissRequest = { editKey = false },
            title = { Text("YouTube API key") },
            text = {
                Column {
                    Text("Stored only on this phone.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(key, { key = it }, singleLine = true, label = { Text("AIza…") })
                }
            },
            confirmButton = { TextButton(onClick = { store.updateApiKey(key); editKey = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editKey = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun YtPlayerView(yt: YtState) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var view by remember { mutableStateOf<YouTubePlayerView?>(null) }
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                // Pauses when the app goes to the background, as YouTube's rules require
                lifecycleOwner.lifecycle.addObserver(this)
                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) { yt.attach(youTubePlayer) }
                    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                        when (state) {
                            PlayerConstants.PlayerState.PLAYING -> yt.onPlay()
                            PlayerConstants.PlayerState.ENDED -> yt.next()
                            else -> {}
                        }
                    }
                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        val hasNext = yt.index + 1 < yt.queue.size
                        yt.next()
                        yt.error = "That video's owner blocks outside players" + if (hasNext) " — skipped to the next one." else "."
                    }
                }, IFramePlayerOptions.Builder().controls(1).build())
                view = this
            }
        }
    )
    DisposableEffect(Unit) {
        onDispose {
            view?.let {
                lifecycleOwner.lifecycle.removeObserver(it)
                it.release()
            }
            yt.player = null
        }
    }
}
