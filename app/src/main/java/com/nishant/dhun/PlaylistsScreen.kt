package com.nishant.dhun

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val LIKED = "\u0000liked"

@Composable
fun PlaylistsScreen(
    songsById: Map<Long, Song>,
    store: Store,
    player: PlayerUi,
    onAddToPlaylist: (Song) -> Unit,
) {
    var open by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = open != null) { open = null }

    val openName = open
    if (openName != null) {
        val isLiked = openName == LIKED
        val ids = if (isLiked) store.liked.toList() else store.playlists.firstOrNull { it.name == openName }?.songIds
        if (ids == null) { open = null; return }
        val list = ids.mapNotNull { songsById[it] }
        val currentId = player.current?.mediaId
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { open = null }) { Icon(Icons.Filled.ArrowBack, "Back") }
                    Column(Modifier.weight(1f)) {
                        Text(if (isLiked) "Liked songs" else openName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${list.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!isLiked) IconButton(onClick = { confirmDelete = openName }) { Icon(Icons.Filled.Delete, "Delete playlist") }
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { player.play(list, 0) }, enabled = list.isNotEmpty()) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play")
                    }
                    FilledTonalButton(onClick = { player.play(list, 0, shuffled = true) }, enabled = list.isNotEmpty()) {
                        Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(4.dp)); Text("Shuffle")
                    }
                }
                if (list.isEmpty()) {
                    Text(
                        if (isLiked) "Tap ♥ on any song to add it here." else "Add songs from My Music using the ⋮ menu.",
                        Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(list.size, key = { list[it].id }) { i ->
                val s = list[i]
                SongRow(
                    song = s, store = store, playing = s.id.toString() == currentId,
                    onClick = { player.play(list, i) },
                    onPlayNext = { player.addNext(s) },
                    onAddToPlaylist = { onAddToPlaylist(s) },
                    onRemove = if (isLiked) null else ({ store.removeFromPlaylist(openName, s.id) }),
                )
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Playlists", Modifier.weight(1f))
                    IconButton(onClick = { creating = true }, modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Filled.Add, "New playlist") }
                }
                PlaylistTile("Liked songs", "${store.liked.size} songs", Icons.Filled.Favorite,
                    listOf(Color(0xFFFF7A45), Color(0xFF8A2BE2))) { open = LIKED }
            }
            items(store.playlists, key = { it.name }) { pl ->
                PlaylistTile(pl.name, "${pl.songIds.size} songs", Icons.Filled.QueueMusic,
                    listOf(Color(0xFF2E3A59), Color(0xFF1C1C20))) { open = pl.name }
            }
            if (store.playlists.isEmpty()) {
                item {
                    Text("Tap + to make your first playlist.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New playlist") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }) },
            confirmButton = { TextButton(onClick = { store.createPlaylist(name); creating = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
        )
    }
    confirmDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"$name\"?") },
            text = { Text("The songs stay on your phone; only the playlist is removed.") },
            confirmButton = { TextButton(onClick = { store.deletePlaylist(name); confirmDelete = null; open = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PlaylistTile(title: String, sub: String, icon: ImageVector, colors: List<Color>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
