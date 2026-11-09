package moe.rukamori.archivetune.playback

import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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

    private inner class FilteringListener(
        private val delegate: Player.Listener,
    ) : Player.Listener by delegate {
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
