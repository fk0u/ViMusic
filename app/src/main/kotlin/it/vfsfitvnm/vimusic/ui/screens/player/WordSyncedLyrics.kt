package it.vfsfitvnm.vimusic.ui.screens.player

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.surface
import it.vfsfitvnm.providers.lyricsplus.LyricsPlusSyncManager
import it.vfsfitvnm.vimusic.ui.modifiers.verticalFadingEdge
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.medium
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSyncedLyrics(
    manager: LyricsPlusSyncManager,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val (colorPalette, typography) = LocalAppearance.current
    val baseStyle = typography.xs.center.medium
    val density = LocalDensity.current

    val textPaint = remember {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = with(density) { baseStyle.fontSize.toPx() }
            this.typeface = Typeface.create(Typeface.DEFAULT_BOLD, baseStyle.fontWeight?.weight ?: FontWeight.Medium.weight, false)
        }
    }

    val lazyListState = rememberLazyListState()
    val lyrics = manager.getLyrics()

    val currentLineIndex by manager.currentLineIndex.collectAsState()
    val currentPosition by manager.currentPosition.collectAsState()
    val previousLineIndex = remember { mutableIntStateOf(-2) }

    LaunchedEffect(isVisible, currentLineIndex) {
        if (isVisible && currentLineIndex != previousLineIndex.intValue) {
            previousLineIndex.intValue = currentLineIndex
            val targetIndex = if (currentLineIndex == -1) 0 else currentLineIndex
            val viewHeight = lazyListState.layoutInfo.viewportSize.height
            val centerOffset = viewHeight / 2

            lazyListState.animateScrollToItem(index = targetIndex + 1, scrollOffset = -centerOffset)
        }
    }
    
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        tonalElevation = 2.dp
    ) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = false,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .verticalFadingEdge()
                .fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(lazyListState.layoutInfo.viewportSize.height.dp / 2))
            }

            itemsIndexed(lyrics.toImmutableList()) { lineIndex, line ->
                val isActiveLine = lineIndex == currentLineIndex

                ElevatedCard(
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isActiveLine) colorPalette.surface else colorPalette.background0.copy(alpha = 0.8f),
                        contentColor = colorPalette.text
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isActiveLine) 6.dp else 1.dp
                    )
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        line.words.forEach { word ->
                            val wordStyle: TextStyle = if (isActiveLine) {
                                val timeProgress = when {
                                    currentPosition < word.startTimeMs -> 0f
                                    currentPosition >= (word.startTimeMs + word.durationMs) -> 1f
                                    else -> if (word.durationMs > 0) {
                                        ((currentPosition - word.startTimeMs) / word.durationMs.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                }

                                val wipeWidthDp = 16.dp
                                val wipeWidthPx = with(density) { wipeWidthDp.toPx() }
                                val wordWidthPx = textPaint.measureText(word.text)
                                val wipeFraction = if (wordWidthPx > 0) wipeWidthPx / wordWidthPx else 0f

                                val totalTravel = 1f + wipeFraction
                                val wipeCenter = (timeProgress * totalTravel) - (wipeFraction / 2)

                                val transitionStart = wipeCenter - (wipeFraction / 2)
                                val transitionEnd = wipeCenter + (wipeFraction / 2)

                                val activeColor = Color.White
                                val upcomingColor = Color.White.copy(alpha = 0.6f)

                                val textBrush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        transitionStart to activeColor,
                                        transitionEnd to upcomingColor
                                    )
                                )
                                baseStyle.merge(TextStyle(brush = textBrush))
                            } else {
                                val animatedColor by animateColorAsState(
                                    targetValue = colorPalette.textDisabled,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "inactiveLineColor"
                                )
                                baseStyle.copy(color = animatedColor)
                            }

                            BasicText(
                                text = word.text,
                                style = wordStyle
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(lazyListState.layoutInfo.viewportSize.height.dp / 2))
            }
        }
    }
}
