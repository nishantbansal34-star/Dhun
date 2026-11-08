package com.nishant.dhun

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class Group(val name: String, val sub: String, val art: Any?, val songs: List<Song>)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    loaded: Boolean,
    hasPerm: Boolean,
    store: Store,
    player: PlayerUi,
    onAskPermission: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 songs, 1 albums, 2 artists
    var openGroup by remember { mutableStateOf<String?>(null) }
    val currentId = player.current?.mediaId

    if (!hasPerm) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Let Dhun see your music", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(6.dp))
            Text("Dhun plays the songs saved on your phone. Nothing leaves your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(10.dp))
            Button(onClick = onAskPermission) { Text("Allow access") }
        }
        return
    }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }
    val groups = remember(filtered, mode) {
        when (mode) {
            1 -> filtered.groupBy { it.album }.map { (k, v) -> Group(k, v.first().artist, v.first().artUri, v) }.sortedBy { it.name.lowercase() }
            2 -> filtered.groupBy { it.artist }.map { (k, v) -> Group(k, "${v.size} songs", v.first().artUri, v) }.sortedBy { it.name.lowercase() }
            else -> emptyList()
        }
    }

    val group = openGroup?.let { name -> groups.firstOrNull { it.name == name } }
    BackHandler(enabled = group != null) { openGroup = null }

    val list = group?.songs ?: filtered

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            if (group == null) {
                SectionHeader("My Music")
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search songs, artists, albums") },
                    singleLine = true, shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Songs", "Albums", "Artists").forEachIndexed { i, label ->
                        FilterChip(selected = mode == i, onClick = { mode = i; openGroup = null }, label = { Text(label) })
                    }
                }
            } else {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { openGroup = null }) { Icon(Icons.Filled.ArrowBack, "Back") }
                    Art(group.art, 64.dp, seed = group.name)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(group.sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (mode == 0 || group != null) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { player.play(list, 0) }, enabled = list.isNotEmpty()) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play")
                    }
                    FilledTonalButton(onClick = { player.play(list, 0, shuffled = true) }, enabled = list.isNotEmpty()) {
                        Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(4.dp)); Text("Shuffle")
                    }
                }
            }
        }

        if (mode != 0 && group == null) {
            items(groups, key = { it.name }) { g ->
                Row(
                    Modifier.fillMaxWidth().clickable { openGroup = g.name }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Art(g.art, 56.dp, seed = g.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(g.sub, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            if (loaded && list.isEmpty()) {
                item {
                    Text(
                        if (songs.isEmpty()) "No music files found on this phone yet. Copy MP3/FLAC/M4A files into your Music folder and reopen Dhun."
                        else "Nothing matches \"$query\".",
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
                )
            }
        }
    }
}
