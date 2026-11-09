package moe.rukamori.archivetune.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R

/** Ícone do botão de aleatório: com ✨ quando o aleatório inteligente está ligado. */
@DrawableRes
@Composable
internal fun shuffleIconRes(): Int {
    val connection = LocalPlayerConnection.current ?: return R.drawable.shuffle
    val smart by connection.smartShuffleEnabled.collectAsState()
    return if (smart) R.drawable.shuffle_smart else R.drawable.shuffle
}
