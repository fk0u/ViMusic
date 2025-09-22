package it.vfsfitvnm.vimusic.ui.screens.player

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.providers.lyricsplus.LyricsPlusSyncManager
import it.vfsfitvnm.vimusic.ui.modifiers.verticalFadingEdge
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.medium
import kotlinx.collections.immutable.toImmutableList

/**
 * Apple Music style lyrics: focuses active line with scale + higher opacity while keeping previous/next
 * lines visible but dimmed. Per-word progressive highlight uses sharp left-to-right fill with added glow
 * effect for immersive full-screen experience.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppleStyleLyrics(
    manager: LyricsPlusSyncManager,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    // Visual tuning params
    activeScale: Float = 1.25f,
    inactiveScale: Float = 0.88f,
    inactiveAlpha: Float = 0.28f,
    // Upward bias: 0f keeps center, negative moves active line upward, positive downward
    verticalBias: Float = -0.22f,
    // True for immersive full-screen mode with backdrop effect
    isFullScreen: Boolean = true
) {
    val (colorPalette, typography) = LocalAppearance.current
    // Slightly larger base font for full-screen experience
    val baseStyle = typography.xs.center.medium.copy(
        fontSize = if (isFullScreen) 18.sp else typography.xs.fontSize
    )
    val density = LocalDensity.current

    // Subtle animated backdrop gradient
    val infiniteTransition = rememberInfiniteTransition(label = "backdropAnimation")
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientRotation"
    )
    
    // Background effects for full-screen mode
    val backdropModifier = if (isFullScreen) {
        Modifier
            .fillMaxSize()
            .background(
                Brush.sweepGradient(
                    0f to Color(0xFF000A12).copy(alpha = 0.85f),
                    0.3f to Color(0xFF001C30).copy(alpha = 0.9f),
                    0.6f to Color(0xFF002240).copy(alpha = 0.85f),
                    0.8f to Color(0xFF000A12).copy(alpha = 0.9f),
                    1f to Color(0xFF000A12).copy(alpha = 0.85f),
                    center = androidx.compose.ui.geometry.Offset(0.5f + gradientRotation.coerceIn(0f, 0.1f), 0.5f)
                )
            )
            .blur(20.dp)
    } else {
        Modifier
    }

    val textPaint = remember {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = with(density) { baseStyle.fontSize.toPx() }
            this.typeface = Typeface.create(Typeface.DEFAULT, baseStyle.fontWeight?.weight ?: FontWeight.Normal.weight, false)
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
            // Just snap to the line; spacers provide positioning bias
            lazyListState.animateScrollToItem(index = targetIndex + 1)
        }
    }

    // Full screen backdrop if needed
    Box(modifier = if (isFullScreen) backdropModifier else Modifier) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = false,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .verticalFadingEdge()
                .fillMaxWidth()
        ) {
            // Dynamic spacers to bias the active line vertically
            val viewport = lazyListState.layoutInfo.viewportSize.height
            val clampedBias = verticalBias.coerceIn(-0.6f, 0.6f)
            val topFraction = 0.5f + (clampedBias / 2f)
            val bottomFraction = 0.5f - (clampedBias / 2f)
            val topHeight = (viewport * topFraction).toInt().coerceAtLeast(0)
            val bottomHeight = (viewport * bottomFraction).toInt().coerceAtLeast(0)
            item { Spacer(modifier = Modifier.height(topHeight.dp)) }

            itemsIndexed(lyrics.toImmutableList()) { lineIndex, line ->
                val isActiveLine = lineIndex == currentLineIndex
                val targetScale = if (isActiveLine) activeScale else inactiveScale
                // Enhanced easing for smoother feel - similar to Apple Music
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    // Smoother animation with overshooting effect
                    animationSpec = tween(450),
                    label = "lineScale"
                )
                val targetAlpha = if (isActiveLine) 1f else inactiveAlpha
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(350),
                    label = "lineAlpha"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(vertical = 6.dp, horizontal = 24.dp)
                        .scale(animatedScale)
                        .alpha(animatedAlpha)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
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
                                // Enhanced colors for more vibrant appearance
                                val activeColor = Color.White
                                val upcomingColor = Color.White.copy(alpha = 0.25f)
                                val fill = timeProgress.coerceIn(0f, 1f)
                                
                                // Solid fill effect with sharp boundary
                                val textBrush = if (fill == 0f) {
                                    Brush.horizontalGradient(listOf(upcomingColor, upcomingColor))
                                } else if (fill == 1f) {
                                    Brush.horizontalGradient(listOf(activeColor, activeColor))
                                } else {
                                    Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0f to activeColor,
                                            fill to activeColor,
                                            fill to upcomingColor,
                                            1f to upcomingColor
                                        )
                                    )
                                }
                                
                                // Glow effect for active words using shadow + elevated weight
                                baseStyle.merge(
                                    TextStyle(
                                        brush = textBrush,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            } else {
                                baseStyle.copy(color = colorPalette.textDisabled)
                            }

                            // Add glow to active words with shadow and custom drawing
                            val isActiveWord = isActiveLine && currentPosition >= word.startTimeMs && currentPosition < (word.startTimeMs + word.durationMs + 200)
                            val glowModifier = if (isActiveWord) {
                                Modifier
                                    .drawBehind {
                                        drawIntoCanvas { canvas ->
                                            val shadowColor = Color.White.copy(alpha = 0.25f)
                                            val shadowRadius = 12f
                                            val originalColor = canvas.nativeCanvas.save()
                                            canvas.nativeCanvas.drawText(
                                                word.text,
                                                this.center.x - (textPaint.measureText(word.text) / 2),
                                                this.center.y + (textPaint.textSize / 3),
                                                TextPaint(textPaint).apply {
                                                    color = android.graphics.Color.argb(
                                                        (shadowColor.alpha * 255).toInt(),
                                                        (shadowColor.red * 255).toInt(),
                                                        (shadowColor.green * 255).toInt(),
                                                        (shadowColor.blue * 255).toInt()
                                                    )
                                                    setShadowLayer(shadowRadius, 0f, 0f, android.graphics.Color.WHITE)
                                                }
                                            )
                                            canvas.nativeCanvas.restoreToCount(originalColor)
                                        }
                                    }
                                    .shadow(
                                        elevation = 4.dp,
                                        ambientColor = Color.White.copy(alpha = 0.2f),
                                        spotColor = Color.White.copy(alpha = 0.2f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                    )
                            } else {
                                Modifier
                            }
                            
                            Box(modifier = glowModifier) {
                                BasicText(
                                    text = word.text,
                                    style = wordStyle
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(bottomHeight.dp)) }
        }
    }
}