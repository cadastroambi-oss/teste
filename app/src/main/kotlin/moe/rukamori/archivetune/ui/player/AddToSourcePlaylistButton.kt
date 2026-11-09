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
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.ui.menu.AddToPlaylistDialog
import moe.rukamori.archivetune.playback.MusicService

/**
 * Botão do player (no lugar do compartilhar):
 * - tocando uma playlist (normal, aleatório, inteligente) ou o mix dela: um toque adiciona a música
 *   atual a essa playlist; fica com ✓ quando ela já está lá;
 * - tocando outra coisa: abre a lista de playlists para escolher.
 */
@Composable
internal fun AddToSourcePlaylistButton(
    mediaMetadata: MediaMetadata,
    contentColor: Color,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaId = mediaMetadata.id
    val sourcePlaylistId by playerConnection.service.queueSourcePlaylistId.collectAsState()
    val originPlaylistId by playerConnection.service.queueOriginPlaylistId.collectAsState()
    val playlistId = sourcePlaylistId ?: originPlaylistId

    val playlistFlow = remember(playlistId) { playlistId?.let { database.playlist(it) } ?: flowOf(null) }
    val playlist by playlistFlow.collectAsState(initial = null)
    var refresh by remember { mutableIntStateOf(0) }
    val inPlaylist by produceState(false, mediaId, playlistId, playlist?.songCount, refresh) {
        value = if (playlistId == null) {
            false
        } else {
            withContext(Dispatchers.IO) {
                runCatching { database.playlistDuplicates(playlistId, listOf(mediaId)).isNotEmpty() }.getOrDefault(false)
            }
        }
    }
    val playlistName = playlist?.playlist?.name.orEmpty()
    var showChooser by remember { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChooser,
        onGetSong = {
            database.withTransaction { insert(mediaMetadata) }
            listOf(mediaMetadata.id)
        },
        onDismiss = { showChooser = false },
        onAddComplete = { _, playlistNames ->
            val message =
                if (playlistNames.size == 1) {
                    context.getString(R.string.added_to_playlist, playlistNames.first())
                } else {
                    context.getString(R.string.added_to_n_playlists, playlistNames.size)
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    Surface(
        onClick = {
            when {
                playlistId == null -> showChooser = true
                inPlaylist ->
                    Toast.makeText(context, context.getString(R.string.already_in_source_playlist, playlistName), Toast.LENGTH_SHORT).show()
                else -> scope.launch {
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
            }
        },
        shape = RoundedCornerShape(14.dp),
        color = if (inPlaylist) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else contentColor.copy(alpha = 0.12f),
        modifier = Modifier.height(44.dp).width(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(if (inPlaylist) R.drawable.done else R.drawable.playlist_add),
                contentDescription =
                    if (playlistId == null) {
                        stringResource(R.string.add_to_playlist)
                    } else {
                        stringResource(R.string.add_to_source_playlist, playlistName)
                    },
                tint = if (inPlaylist) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
