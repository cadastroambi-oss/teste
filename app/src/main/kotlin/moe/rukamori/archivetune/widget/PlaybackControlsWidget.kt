/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import moe.rukamori.archivetune.R

/**
 * Widget 4x1: nome da música e os botões curtir, voltar, play/pausa, avançar e rádio.
 */
class PlaybackControlsWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            PlaybackControlsContent(context)
        }
    }
}

@Composable
private fun PlaybackControlsContent(context: Context) {
    val prefs = currentState<Preferences>()
    val state = prefs.toWidgetPlaybackState(context)

    GlanceTheme(
        colors =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GlanceTheme.colors
            } else {
                ArchiveTuneWidgetColors.providers
            },
    ) {
        val palette = rememberWidgetPalette(state.dominantColor)
        val size = LocalSize.current
        val compact = size.height < 90.dp
        val buttonSize = if (compact) 36.dp else 44.dp
        val iconSize = if (compact) 20.dp else 24.dp

        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(palette.surface)
                    .cornerRadius(24.dp)
                    .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                // Nome da música (toque abre o app)
                Text(
                    text =
                        if (state.isAvailable && state.artist.isNotBlank()) {
                            "${state.title} • ${state.artist}"
                        } else {
                            state.title
                        },
                    style =
                        TextStyle(
                            color = palette.onSurface,
                            fontSize = if (compact) 13.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    maxLines = 1,
                    modifier =
                        GlanceModifier
                            .fillMaxWidth()
                            .clickable(openArchiveTuneAction(context)),
                )
                Spacer(GlanceModifier.height(if (compact) 2.dp else 6.dp))

                // Curtir · Voltar · Play/pausa · Avançar · Rádio
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlSlot {
                        WidgetControlButton(
                            modifier = GlanceModifier.size(buttonSize),
                            action = toggleLikeAction(),
                            icon = if (state.isLiked) R.drawable.favorite else R.drawable.favorite_border,
                            contentDescription =
                                context.getString(
                                    if (state.isLiked) R.string.action_remove_like else R.string.action_like,
                                ),
                            backgroundColor = palette.secondaryContainer,
                            contentColor = palette.onSecondaryContainer,
                            cornerRadius = buttonSize / 2,
                            iconSize = iconSize,
                        )
                    }
                    ControlSlot {
                        WidgetControlButton(
                            modifier = GlanceModifier.size(buttonSize),
                            action = skipPreviousAction(),
                            icon = R.drawable.skip_previous,
                            contentDescription = context.getString(R.string.widget_previous),
                            backgroundColor = palette.secondaryContainer,
                            contentColor = palette.onSecondaryContainer,
                            cornerRadius = buttonSize / 2,
                            iconSize = iconSize,
                        )
                    }
                    ControlSlot {
                        WidgetControlButton(
                            modifier = GlanceModifier.size(buttonSize),
                            action = playPauseAction(),
                            icon = if (state.isPlaying) R.drawable.pause else R.drawable.play,
                            contentDescription =
                                context.getString(
                                    if (state.isPlaying) R.string.widget_pause else R.string.play,
                                ),
                            backgroundColor = palette.primaryContainer,
                            contentColor = palette.onPrimaryContainer,
                            cornerRadius = if (state.isPlaying) 12.dp else buttonSize / 2,
                            iconSize = iconSize,
                        )
                    }
                    ControlSlot {
                        WidgetControlButton(
                            modifier = GlanceModifier.size(buttonSize),
                            action = skipNextAction(),
                            icon = R.drawable.skip_next,
                            contentDescription = context.getString(R.string.next),
                            backgroundColor = palette.secondaryContainer,
                            contentColor = palette.onSecondaryContainer,
                            cornerRadius = buttonSize / 2,
                            iconSize = iconSize,
                        )
                    }
                    ControlSlot {
                        WidgetControlButton(
                            modifier = GlanceModifier.size(buttonSize),
                            action = startRadioAction(),
                            icon = R.drawable.radio,
                            contentDescription = context.getString(R.string.start_radio),
                            backgroundColor = palette.secondaryContainer,
                            contentColor = palette.onSecondaryContainer,
                            cornerRadius = buttonSize / 2,
                            iconSize = iconSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ControlSlot(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier.defaultWeight(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
