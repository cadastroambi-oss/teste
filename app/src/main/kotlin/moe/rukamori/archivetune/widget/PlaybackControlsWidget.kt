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
 * Widget com o nome da música tocando e os botões: curtir, iniciar rádio e play/pausa.
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
        val compact = size.width < 300.dp || size.height < 82.dp
        val buttonSize = if (compact) 44.dp else 52.dp
        val iconSize = if (compact) 22.dp else 26.dp
        val gap = if (compact) 6.dp else 8.dp

        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(palette.surface)
                    .cornerRadius(28.dp)
                    .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = 8.dp)
                    .clickable(openArchiveTuneAction(context)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        text = state.title,
                        style =
                            TextStyle(
                                color = palette.onSurface,
                                fontSize = if (compact) 15.sp else 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.artist,
                        style =
                            TextStyle(
                                color = palette.onSurfaceVariant,
                                fontSize = if (compact) 12.sp else 13.sp,
                            ),
                        maxLines = 1,
                    )
                }

                if (state.isAvailable) {
                    Spacer(GlanceModifier.width(gap))
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

                    if (state.canStartRadio) {
                        Spacer(GlanceModifier.width(gap))
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

                    Spacer(GlanceModifier.width(gap))
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
                        cornerRadius = if (state.isPlaying) 14.dp else buttonSize / 2,
                        iconSize = iconSize,
                    )
                }
            }
        }
    }
}
