/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

/**
 * Controla quantas vezes o app tenta recuperar a reprodução da mesma música.
 * Antes era uma única tentativa imediata; nos dados móveis (troca de antena/IP) isso não
 * bastava e aparecia o erro 403. Agora são até [maxAttempts] tentativas por música.
 */
internal class PlaybackStreamRecoveryTracker(
    private val maxAttempts: Int = 3,
) {
    private var attemptedMediaId: String? = null
    private var attempts = 0

    /** Número da tentativa atual (1 = primeira) para a música em recuperação. */
    val currentAttempt: Int get() = attempts

    fun registerRetryAttempt(mediaId: String): Boolean {
        if (attemptedMediaId != mediaId) {
            attemptedMediaId = mediaId
            attempts = 0
        }
        if (attempts >= maxAttempts) return false
        attempts++
        return true
    }

    fun onPlaybackRecovered(mediaId: String?) {
        if (mediaId != null && attemptedMediaId == mediaId) {
            attemptedMediaId = null
            attempts = 0
        }
    }

    fun onMediaItemChanged(currentMediaId: String?) {
        if (attemptedMediaId != currentMediaId) {
            attemptedMediaId = null
            attempts = 0
        }
    }
}
