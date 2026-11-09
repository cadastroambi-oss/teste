package moe.rukamori.archivetune.ui.player

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.playback.MusicService

/**
 * Botão do player: quando está tocando o mix/rádio de uma playlist, adiciona a música atual
 * direto nessa playlist com um toque. Fica com ✓ quando a música já está na playlist.
 */
@Composable
internal fun AddToSourcePlaylistButton(
    mediaId: String,
    contentColor: Color,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sourcePlaylistId by playerConnection.service.queueSourcePlaylistId.collectAsState()
    val playlistId = sourcePlaylistId ?: return

    val playlistFlow = remember(playlistId) { database.playlist(playlistId) }
    val playlist by playlistFlow.collectAsState(initial = null)
    var refresh by remember { mutableIntStateOf(0) }
    val inPlaylist by produceState(false, mediaId, playlistId, playlist?.songCount, refresh) {
        value = withContext(Dispatchers.IO) {
            runCatching { database.playlistDuplicates(playlistId, listOf(mediaId)).isNotEmpty() }.getOrDefault(false)
        }
    }
    val playlistName = playlist?.playlist?.name.orEmpty()
    val description = stringResource(R.string.add_to_source_playlist, playlistName)

    Surface(
        onClick = {
            if (inPlaylist) {
                Toast.makeText(context, context.getString(R.string.already_in_source_playlist, playlistName), Toast.LENGTH_SHORT).show()
            } else scope.launch {
                val message = when (playerConnection.service.addCurrentSongToSourcePlaylist()) {
                    MusicService.AddToSourcePlaylistResult.ADDED ->
                        context.getString(R.string.added_to_source_playlist, playlistName)
                    MusicService.AddToSourcePlaylistResult.ALREADY_IN_PLAYLIST ->
                        context.getString(R.string.already_in_source_playlist, playlistName)
                    MusicService.AddToSourcePlaylistResult.FAILED ->
                        context.getString(R.string.add_to_source_playlist_failed)
                }
                refresh++
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        },
        shape = RoundedCornerShape(14.dp),
        color = if (inPlaylist) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else contentColor.copy(alpha = 0.12f),
        modifier = Modifier.height(44.dp).width(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(if (inPlaylist) R.drawable.done else R.drawable.playlist_add),
                contentDescription = description,
                tint = if (inPlaylist) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
