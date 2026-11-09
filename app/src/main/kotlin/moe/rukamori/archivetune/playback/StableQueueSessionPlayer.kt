package moe.rukamori.archivetune.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import java.util.concurrent.ConcurrentHashMap

/**
 * Player entregue à sessão de mídia (Android Auto, notificação).
 *
 * O ExoPlayer avisa "a linha do tempo mudou" (TIMELINE_CHANGE_REASON_SOURCE_UPDATE) toda vez que
 * termina de carregar uma música (descobre a duração, pré-carrega a próxima), mesmo sem a fila mudar.
 * A cada aviso a sessão reenvia a fila inteira ao Android Auto, que redesenha a lista e volta ao
 * início — por isso não dava para rolar a fila no carro.
 *
 * Aqui esses avisos só passam quando a fila muda de verdade (músicas diferentes ou em outra ordem).
 * Quando só a duração da música atual muda, a sessão recebe um aviso de metadados, que atualiza a
 * barra de progresso no carro sem reenviar a fila.
 */
internal class StableQueueSessionPlayer(player: Player) : ForwardingPlayer(player) {
    private val wrappers = ConcurrentHashMap<Player.Listener, FilteringListener>()

    override fun addListener(listener: Player.Listener) {
        val wrapper = wrappers.getOrPut(listener) { FilteringListener(listener) }
        super.addListener(wrapper)
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(wrappers.remove(listener) ?: listener)
    }

    /**
     * Repassa TODOS os avisos do player para a sessão, menos os de "linha do tempo mudou" que não
     * mudam a fila. Cada aviso é repassado explicitamente: a delegação automática do Kotlin
     * ("by delegate") não repassa os métodos padrão de interfaces Java como Player.Listener — foi
     * isso que fez a música parar ao sair do app (a sessão nunca sabia que estava tocando).
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private inner class FilteringListener(
        private val delegate: Player.Listener,
    ) : Player.Listener {
        private var lastSignature: List<String>? = null
        private var lastDuration: Long = C.TIME_UNSET

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val signature = timeline.signature()
            val duration = currentDuration()
            if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE && signature == lastSignature) {
                if (duration != lastDuration) {
                    lastDuration = duration
                    delegate.onMediaMetadataChanged(mediaMetadata)
                }
                return
            }
            lastSignature = signature
            lastDuration = duration
            delegate.onTimelineChanged(timeline, reason)
        }

        override fun onEvents(player: Player, events: Player.Events) = delegate.onEvents(player, events)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = delegate.onMediaItemTransition(mediaItem, reason)
        override fun onTracksChanged(tracks: Tracks) = delegate.onTracksChanged(tracks)
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = delegate.onMediaMetadataChanged(mediaMetadata)
        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) = delegate.onPlaylistMetadataChanged(mediaMetadata)
        override fun onIsLoadingChanged(isLoading: Boolean) = delegate.onIsLoadingChanged(isLoading)
        override fun onLoadingChanged(isLoading: Boolean) = delegate.onLoadingChanged(isLoading)
        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) = delegate.onAvailableCommandsChanged(availableCommands)
        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) = delegate.onTrackSelectionParametersChanged(parameters)
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) = delegate.onPlayerStateChanged(playWhenReady, playbackState)
        override fun onPlaybackStateChanged(playbackState: Int) = delegate.onPlaybackStateChanged(playbackState)
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = delegate.onPlayWhenReadyChanged(playWhenReady, reason)
        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) = delegate.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
        override fun onIsPlayingChanged(isPlaying: Boolean) = delegate.onIsPlayingChanged(isPlaying)
        override fun onRepeatModeChanged(repeatMode: Int) = delegate.onRepeatModeChanged(repeatMode)
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = delegate.onShuffleModeEnabledChanged(shuffleModeEnabled)
        override fun onPlayerError(error: PlaybackException) = delegate.onPlayerError(error)
        override fun onPlayerErrorChanged(error: PlaybackException?) = delegate.onPlayerErrorChanged(error)
        override fun onPositionDiscontinuity(reason: Int) = delegate.onPositionDiscontinuity(reason)
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = delegate.onPositionDiscontinuity(oldPosition, newPosition, reason)
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = delegate.onPlaybackParametersChanged(playbackParameters)
        override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) = delegate.onSeekBackIncrementChanged(seekBackIncrementMs)
        override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) = delegate.onSeekForwardIncrementChanged(seekForwardIncrementMs)
        override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) = delegate.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
        override fun onAudioSessionIdChanged(audioSessionId: Int) = delegate.onAudioSessionIdChanged(audioSessionId)
        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) = delegate.onAudioAttributesChanged(audioAttributes)
        override fun onVolumeChanged(volume: Float) = delegate.onVolumeChanged(volume)
        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) = delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = delegate.onDeviceInfoChanged(deviceInfo)
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = delegate.onDeviceVolumeChanged(volume, muted)
        override fun onVideoSizeChanged(videoSize: VideoSize) = delegate.onVideoSizeChanged(videoSize)
        override fun onSurfaceSizeChanged(width: Int, height: Int) = delegate.onSurfaceSizeChanged(width, height)
        override fun onRenderedFirstFrame() = delegate.onRenderedFirstFrame()
        override fun onCues(cues: MutableList<Cue>) = delegate.onCues(cues)
        override fun onCues(cueGroup: CueGroup) = delegate.onCues(cueGroup)
        override fun onMetadata(metadata: Metadata) = delegate.onMetadata(metadata)

        override fun equals(other: Any?): Boolean =
            other is FilteringListener && other.delegate == delegate

        override fun hashCode(): Int = delegate.hashCode()
    }

    private fun currentDuration(): Long = runCatching { duration }.getOrDefault(C.TIME_UNSET)

    private fun Timeline.signature(): List<String> {
        if (isEmpty) return emptyList()
        val window = Timeline.Window()
        val ids = ArrayList<String>(windowCount + 1)
        for (i in 0 until windowCount) {
            ids += getWindow(i, window).mediaItem.mediaId
        }
        // Inclui a ordem do aleatório: mudar a ordem também é mudar a fila.
        val order = StringBuilder()
        var index = getFirstWindowIndex(true)
        var guard = 0
        while (index != C.INDEX_UNSET && guard++ < windowCount) {
            order.append(index).append(',')
            index = getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        ids += order.toString()
        return ids
    }
}
