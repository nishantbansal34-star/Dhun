package com.nishant.dhun

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun DhunApp(store: Store, player: PlayerUi, sharedLink: MutableState<String?>) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var fullPlayer by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf<Song?>(null) }
    val yt = remember { YtState() }

    // Local and YouTube never play over each other
    player.onLocalPlay = { yt.pause() }
    yt.onPlay = { player.pause() }

    val audioPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    var hasPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPerm) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPerm = res[audioPerm] == true
    }
    LaunchedEffect(Unit) {
        if (!hasPerm) {
            val perms = mutableListOf(audioPerm)
            if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
            launcher.launch(perms.toTypedArray())
        }
    }
    LaunchedEffect(hasPerm) {
        if (hasPerm) {
            songs = withContext(Dispatchers.IO) { runCatching { LocalMusic.load(context) }.getOrDefault(emptyList()) }
        }
        loaded = true
    }

    // Link shared into the app -> jump to YouTube tab and play it
    LaunchedEffect(sharedLink.value) {
        sharedLink.value?.let { link ->
            YouTubeApi.idFromLink(link)?.let { id ->
                tab = 1
                yt.playLink(id)
            }
            sharedLink.value = null
        }
    }

    val songsById = remember(songs) { songs.associateBy { it.id } }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    if (player.current != null && tab != 1) {
                        MiniPlayer(player) { fullPlayer = true }
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.LibraryMusic, null) }, label = { Text("My Music") })
                        NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.SmartDisplay, null) }, label = { Text("YouTube") })
                        NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Filled.QueueMusic, null) }, label = { Text("Playlists") })
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).statusBarsPadding()) {
                when (tab) {
                    0 -> LibraryScreen(songs, loaded, hasPerm, store, player,
                        onAskPermission = { launcher.launch(arrayOf(audioPerm)) },
                        onAddToPlaylist = { addToPlaylist = it })
                    1 -> YouTubeScreen(store, yt)
                    else -> PlaylistsScreen(songsById, store, player, onAddToPlaylist = { addToPlaylist = it })
                }
            }
        }

        AnimatedVisibility(
            visible = fullPlayer && player.current != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            FullPlayer(player, store, onClose = { fullPlayer = false })
        }
    }
    BackHandler(enabled = fullPlayer) { fullPlayer = false }

    addToPlaylist?.let { song ->
        AddToPlaylistDialog(store, song, onDismiss = { addToPlaylist = null })
    }
}

@Composable
fun MiniPlayer(player: PlayerUi, onOpen: () -> Unit) {
    val meta = player.current?.mediaMetadata ?: return
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Art(meta.artworkUri, 44.dp, seed = meta.albumTitle?.toString() ?: "")
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(meta.title?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(meta.artist?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { player.togglePlay() }) {
                Icon(if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/pause")
            }
            IconButton(onClick = { player.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
        }
    }
}

@Composable
fun FullPlayer(player: PlayerUi, store: Store, onClose: () -> Unit) {
    val meta = player.current?.mediaMetadata
    val songId = player.current?.mediaId?.toLongOrNull()
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(player.controller) {
        while (true) {
            player.controller?.let { c ->
                if (!dragging) pos = c.currentPosition
                dur = c.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    val hue = ((meta?.albumTitle?.toString() ?: "").hashCode() and 0xFFFFFF) % 360
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.hsv(hue.toFloat(), 0.5f, 0.35f), MaterialTheme.colorScheme.background))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, "Close", modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f))
                Text("Now playing", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                Art(meta?.artworkUri, 1000.dp, corner = 16.dp, seed = meta?.albumTitle?.toString() ?: "")
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(meta?.title?.toString() ?: "", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(meta?.artist?.toString() ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (songId != null) {
                    val liked = store.isLiked(songId)
                    IconButton(onClick = { store.toggleLike(songId) }) {
                        Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Like",
                            tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
            Slider(
                value = if (dragging) dragValue else progress,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    player.controller?.seekTo((dragValue * dur).toLong())
                    pos = (dragValue * dur).toLong()
                    dragging = false
                },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(if (dragging) (dragValue * dur).toLong() else pos), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(formatTime(dur), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle",
                        tint = if (player.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { player.prev() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", modifier = Modifier.size(40.dp))
                }
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable { player.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/pause",
                        tint = Color.Black, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { player.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { player.cycleRepeat() }) {
                    Icon(
                        if (player.repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repeat",
                        tint = if (player.repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(store: Store, song: Song, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                LazyColumn(Modifier.height(if (store.playlists.isEmpty()) 0.dp else 220.dp)) {
                    items(store.playlists, key = { it.name }) { pl ->
                        Text(
                            "${pl.name}  (${pl.songIds.size})",
                            Modifier.fillMaxWidth().clickable { store.addToPlaylist(pl.name, song.id); onDismiss() }.padding(vertical = 12.dp)
                        )
                    }
                }
                OutlinedTextField(newName, { newName = it }, label = { Text("New playlist") }, singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (newName.isNotBlank()) {
                                store.createPlaylist(newName); store.addToPlaylist(newName.trim(), song.id); onDismiss()
                            }
                        }) { Icon(Icons.Filled.Add, "Create") }
                    })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
