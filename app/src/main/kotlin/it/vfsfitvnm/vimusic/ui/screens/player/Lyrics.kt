package it.vfsfitvnm.vimusic.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import com.valentinilk.shimmer.shimmer
import it.vfsfitvnm.core.ui.LocalAppearance
import it.vfsfitvnm.core.ui.onOverlay
import it.vfsfitvnm.core.ui.onOverlayShimmer
import it.vfsfitvnm.core.ui.overlay
import it.vfsfitvnm.core.ui.utils.dp
import it.vfsfitvnm.providers.innertube.Innertube
import it.vfsfitvnm.providers.innertube.models.bodies.NextBody
import it.vfsfitvnm.providers.innertube.requests.lyrics
import it.vfsfitvnm.providers.kugou.KuGou
import it.vfsfitvnm.providers.lrclib.LrcLib
import it.vfsfitvnm.providers.lrclib.LrcParser
import it.vfsfitvnm.providers.lrclib.models.Track
import it.vfsfitvnm.providers.lrclib.toLrcFile
import it.vfsfitvnm.providers.lyricsplus.LyricsPlus
import it.vfsfitvnm.providers.lyricsplus.LyricsPlusSyncManager
import it.vfsfitvnm.providers.lyricsplus.models.LyricLine
import it.vfsfitvnm.vimusic.BuildConfig
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.Lyrics
import it.vfsfitvnm.vimusic.preferences.PlayerPreferences
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.LOCAL_KEY_PREFIX
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.themed.Menu
import it.vfsfitvnm.vimusic.ui.components.themed.MenuEntry
import it.vfsfitvnm.vimusic.ui.components.themed.TextPlaceholder
import it.vfsfitvnm.vimusic.ui.components.themed.TextFieldDialog
import it.vfsfitvnm.vimusic.ui.components.themed.DefaultDialog
import it.vfsfitvnm.vimusic.ui.components.themed.ValueSelectorDialogBody
import it.vfsfitvnm.vimusic.ui.modifiers.verticalFadingEdge
import it.vfsfitvnm.vimusic.utils.LyricsCacheManager
import it.vfsfitvnm.vimusic.utils.SynchronizedLyrics
import it.vfsfitvnm.vimusic.utils.SynchronizedLyricsState
import it.vfsfitvnm.vimusic.utils.center
import it.vfsfitvnm.vimusic.utils.color
import it.vfsfitvnm.vimusic.utils.isInPip
import it.vfsfitvnm.vimusic.utils.medium
import it.vfsfitvnm.vimusic.utils.semiBold
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// Update delay in milliseconds for lyrics synchronization
private val UPDATE_DELAY = 50.milliseconds

// Cache for word-synced lyrics managers
private val wordSyncCache = mutableMapOf<String, LyricsPlusSyncManager>()

/**
 * Check if the lyrics text is in word-level JSON format
 * @param text The lyrics text to check
 * @return True if the text is in word-level JSON format, false otherwise
 */
private fun isWordLevelJson(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    
    return try {
        // Try to parse as JSON and check if it has the expected structure
        val json = Json.parseToJsonElement(text).jsonObject
        json.keys.any { it == "lyrics" || it == "lines" } && (
            json["lyrics"]?.jsonArray != null || json["lines"]?.jsonArray != null
        )
    } catch (e: Exception) {
        false
    }
}

/**
 * Check if the lyrics text is in LRC format
 * @param text The lyrics text to check
 * @return True if the text is in LRC format, false otherwise
 */
private fun isLrcFormat(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    
    // Check if the text contains time tags like [mm:ss.xx]
    val lrcTimeTagRegex = "\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]".toRegex()
    return lrcTimeTagRegex.find(text) != null
}

/**
 * Parse word-level lyrics from JSON string
 * @param jsonString The JSON string containing word-level lyrics
 * @return A list of LyricLine objects, or null if parsing fails
 */
private fun parseWordLevelLyrics(jsonString: String): List<LyricLine>? {
    return try {
        Json.decodeFromString<List<LyricLine>>(jsonString)
    } catch (e: Exception) {
        try {
            // Try alternate format
            val json = Json.parseToJsonElement(jsonString).jsonObject
            if (json.containsKey("lyrics")) {
                Json.decodeFromString<List<LyricLine>>(json["lyrics"].toString())
            } else if (json.containsKey("lines")) {
                Json.decodeFromString<List<LyricLine>>(json["lines"].toString())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Get cached word-level lyrics from the device
 * @param context Android context
 * @param mediaId The media ID to retrieve lyrics for
 * @return A list of LyricLine objects if found in cache, null otherwise
 */
private fun getCachedWordLevelLyrics(context: android.content.Context, mediaId: String): List<LyricLine>? {
    val cachedJson = LyricsCacheManager.get(context, mediaId) ?: return null
    return parseWordLevelLyrics(cachedJson)
}

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuLaunch: () -> Unit = { },
    onOpenDialog: (() -> Unit)? = null,
    shouldShowSynchronizedLyrics: Boolean = PlayerPreferences.isShowingSynchronizedLyrics,
    setShouldShowSynchronizedLyrics: (Boolean) -> Unit = {
        PlayerPreferences.isShowingSynchronizedLyrics = it
    },
    shouldKeepScreenAwake: Boolean = PlayerPreferences.lyricsKeepScreenAwake,
    shouldUpdateLyrics: Boolean = true,
    showControls: Boolean = true
) = AnimatedVisibility(
    visible = isDisplayed,
    enter = fadeIn(),
    exit = fadeOut()
) {
    val currentEnsureSongInserted by rememberUpdatedState(ensureSongInserted)
    val currentMediaMetadataProvider by rememberUpdatedState(mediaMetadataProvider)
    val currentDurationProvider by rememberUpdatedState(durationProvider)

    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val density = LocalDensity.current
    val view = LocalView.current

    val pip = isInPip()

    var lyrics by remember { mutableStateOf<Lyrics?>(null) }

    // Priority: Word-level > Line-level > Unsync
    val showSynchronizedLyrics = remember(shouldShowSynchronizedLyrics, lyrics) {
        shouldShowSynchronizedLyrics && (
            isWordLevelJson(lyrics?.synced) ||
                isWordLevelJson(lyrics?.fixed) ||
                (!lyrics?.synced.isNullOrBlank() && !isWordLevelJson(lyrics?.synced))
            )
    }

    var editing by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var picking by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var error by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }

    // Get text with proper priority: word-level first, then line-level, then fixed
    val text = remember(lyrics, showSynchronizedLyrics) {
        when {
            showSynchronizedLyrics -> {
                // Priority 1: Word-level JSON in synced
                val syncedText = lyrics?.synced
                if (isWordLevelJson(syncedText)) {
                    syncedText
                } else {
                    // Priority 2: Word-level JSON in fixed
                    val fixedText = lyrics?.fixed
                    if (isWordLevelJson(fixedText)) {
                        fixedText
                    } else {
                        // Priority 3: Line-level LRC in synced
                        if (!syncedText.isNullOrBlank()) syncedText else null
                    }
                }
            }
            else -> {
                // For unsynchronized mode, prefer non-JSON fixed text
                val fixedText = lyrics?.fixed
                if (!isWordLevelJson(fixedText) && !fixedText.isNullOrBlank()) {
                    fixedText
                } else {
                    // Fallback to synced if it's not JSON
                    val syncedText = lyrics?.synced
                    if (!isWordLevelJson(syncedText)) syncedText else null
                }
            }
        }
    }

    var invalidLrc by remember(text) { mutableStateOf(false) }

    // This Saver correctly handles the MutableState object, fixing the type mismatch.
    val lyricsPlusSyncManagerStateSaver = remember(binder) {
        Saver<MutableState<LyricsPlusSyncManager?>, String>(
            save = { state ->
                // Get the value from the state and serialize it to a JSON string
                state.value?.let { manager -> Json.encodeToString(manager.getLyrics()) } ?: ""
            },
            restore = { jsonString ->
                // Deserialize the string to get the manager object
                val restoredManager = if (jsonString.isNotBlank()) {
                    val lyricsList = Json.decodeFromString<List<LyricLine>>(jsonString)
                    LyricsPlusSyncManager(
                        lyrics = lyricsList,
                        positionProvider = { binder?.player?.currentPosition ?: 0L }
                    )
                } else {
                    null
                }
                // Restore it back into a new MutableState
                mutableStateOf(restoredManager)
            }
        )
    }

    var wordSyncedManager by rememberSaveable(
        mediaId,
        shouldShowSynchronizedLyrics,
        saver = lyricsPlusSyncManagerStateSaver // Use the state-aware saver
    ) {
        mutableStateOf(null)
    }

    var wordSyncedAvailable by rememberSaveable(mediaId, shouldShowSynchronizedLyrics) {
        mutableStateOf(false)
    }

    DisposableEffect(shouldKeepScreenAwake) {
        view.keepScreenOn = shouldKeepScreenAwake

        onDispose {
            view.keepScreenOn = false
        }
    }

    // Only trigger on mediaId change, not on player state changes
    LaunchedEffect(mediaId, shouldUpdateLyrics) {
        runCatching {
            withContext(Dispatchers.IO) {
                // Reset state
                wordSyncedManager = null
                wordSyncedAvailable = false

                // Check cached word-level lyrics first
                val cachedWordLyrics = getCachedWordLevelLyrics(context, mediaId)
                if (cachedWordLyrics != null) {
                    wordSyncedAvailable = true
                    val manager = LyricsPlusSyncManager(
                        lyrics = cachedWordLyrics,
                        positionProvider = { binder?.player?.currentPosition ?: 0L }
                    )
                    wordSyncCache[mediaId] = manager
                    wordSyncedManager = manager

                    // Save to database if not exists
                    val jsonString = Json.encodeToString(cachedWordLyrics)
                    Database.instance.lyrics(mediaId).distinctUntilChanged().cancellable().collect { currentLyrics ->
                        if (currentLyrics?.synced != jsonString) {
                            val newLyrics = Lyrics(
                                songId = mediaId,
                                fixed = currentLyrics?.fixed,
                                synced = jsonString
                            )
                            ensureActive()
                            transaction {
                                runCatching {
                                    currentEnsureSongInserted()
                                    Database.instance.upsert(newLyrics)
                                }
                            }
                            lyrics = newLyrics
                        } else {
                            lyrics = currentLyrics
                        }
                        return@collect
                    }
                } else {
                    // Check database
                    Database.instance
                        .lyrics(mediaId)
                        .distinctUntilChanged()
                        .cancellable()
                        .collect { currentLyrics ->

                            // Check if we have word-level in database
                            val wordLevelText = when {
                                isWordLevelJson(currentLyrics?.synced) -> currentLyrics?.synced
                                isWordLevelJson(currentLyrics?.fixed) -> currentLyrics?.fixed
                                else -> null
                            }

                            if (wordLevelText != null) {
                                val parsedWordLyrics = parseWordLevelLyrics(wordLevelText)
                                if (parsedWordLyrics != null) {
                                    wordSyncedAvailable = true
                                    val manager = LyricsPlusSyncManager(
                                        lyrics = parsedWordLyrics,
                                        positionProvider = { binder?.player?.currentPosition ?: 0L }
                                    )
                                    wordSyncCache[mediaId] = manager
                                    wordSyncedManager = manager
                                    lyrics = currentLyrics
                                    return@collect
                                }
                            }

                            // Check if we have sufficient lyrics and don't need to fetch
                            val hasFixedLyrics = !currentLyrics?.fixed.isNullOrBlank()
                            val hasSyncedLyrics = !currentLyrics?.synced.isNullOrBlank()

                            if (!shouldUpdateLyrics || (hasFixedLyrics && hasSyncedLyrics)) {
                                lyrics = currentLyrics
                            } else {
                                // Need to fetch lyrics
                                val mediaMetadata = currentMediaMetadataProvider()
                                var duration = withContext(Dispatchers.Main) {
                                    currentDurationProvider()
                                }
                                while (duration == C.TIME_UNSET) {
                                    delay(100)
                                    duration = withContext(Dispatchers.Main) {
                                        currentDurationProvider()
                                    }
                                }
                                val album = mediaMetadata.albumTitle?.toString()
                                val artist = mediaMetadata.artist?.toString().orEmpty()
                                val title = mediaMetadata.title?.toString().orEmpty().let {
                                    if (mediaId.startsWith(LOCAL_KEY_PREFIX)) {
                                        it.substringBeforeLast('.').trim()
                                    } else it
                                }

                                lyrics = null
                                error = false

                                // Try word-level first
                                val wordLevelLyrics = try {
                                    LyricsPlus.fetchLyrics(
                                        baseUrl = BuildConfig.LYRICS_API_BASE,
                                        title = title,
                                        artist = artist,
                                        album = album
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }

                                val hasActualWords = wordLevelLyrics?.any { it.words.isNotEmpty() } == true

                                if (hasActualWords) {
                                    // Got word-level lyrics
                                    wordSyncedAvailable = true
                                    val nonNullWordLevelLyrics = wordLevelLyrics

                                    val jsonString = try {
                                        Json.encodeToString(nonNullWordLevelLyrics)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }

                                    if (jsonString != null) {
                                        // Cache the word-level lyrics
                                        LyricsCacheManager.save(context, mediaId, jsonString)

                                        // Save word-level to synced field
                                        val newLyrics = Lyrics(
                                            songId = mediaId,
                                            fixed = currentLyrics?.fixed,
                                            synced = jsonString
                                        )

                                        ensureActive()
                                        transaction {
                                            runCatching {
                                                currentEnsureSongInserted()
                                                Database.instance.upsert(newLyrics)
                                            }
                                        }

                                        val manager = LyricsPlusSyncManager(
                                            lyrics = nonNullWordLevelLyrics,
                                            positionProvider = { binder?.player?.currentPosition ?: 0L }
                                        )
                                        wordSyncCache[mediaId] = manager
                                        wordSyncedManager = manager
                                    }
                                } else {
                                    // Fallback to line-level lyrics
                                    wordSyncedAvailable = false

                                    val fixed = currentLyrics?.fixed ?: try {
                                        Innertube.lyrics(NextBody(videoId = mediaId))?.getOrNull()
                                            ?: LrcLib.bestLyrics(
                                                artist = artist,
                                                title = title,
                                                duration = duration.milliseconds,
                                                album = album,
                                                synced = false
                                            )?.map { it?.text }?.getOrNull()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }

                                    val synced = currentLyrics?.synced ?: try {
                                        LrcLib.bestLyrics(
                                            artist = artist,
                                            title = title,
                                            duration = duration.milliseconds,
                                            album = album
                                        )?.map { it?.text }?.getOrNull()
                                            ?: LrcLib.bestLyrics(
                                                artist = artist,
                                                title = title.split("(")[0].trim(),
                                                duration = duration.milliseconds,
                                                album = album
                                            )?.map { it?.text }?.getOrNull()
                                            ?: KuGou.lyrics(
                                                artist = artist,
                                                title = title,
                                                duration = duration / 1000
                                            )?.map { it?.value }?.getOrNull()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }

                                    val newLyrics = Lyrics(
                                        songId = mediaId,
                                        fixed = fixed.orEmpty(),
                                        synced = synced.orEmpty()
                                    )

                                    ensureActive()
                                    transaction {
                                        runCatching {
                                            currentEnsureSongInserted()
                                            Database.instance.upsert(newLyrics)
                                        }
                                    }
                                }
                            }

                            // Update error state based on priority
                            error = when {
                                shouldShowSynchronizedLyrics -> {
                                    !wordSyncedAvailable &&
                                        !isWordLevelJson(lyrics?.synced) &&
                                        !isWordLevelJson(lyrics?.fixed) &&
                                        lyrics?.synced.isNullOrBlank()
                                }
                                else -> {
                                    val fixedText = lyrics?.fixed
                                    val syncedText = lyrics?.synced
                                    (isWordLevelJson(fixedText) || fixedText.isNullOrBlank()) &&
                                        (isWordLevelJson(syncedText) || syncedText.isNullOrBlank())
                                }
                            }
                        }
                }
            }
        }.exceptionOrNull()?.let {
            if (it is CancellationException) throw it
            else it.printStackTrace()
        }
    }

    if (editing) TextFieldDialog(
        hintText = stringResource(R.string.enter_lyrics),
        initialTextInput = (if (shouldShowSynchronizedLyrics) lyrics?.synced else lyrics?.fixed)
            .orEmpty(),
        singleLine = false,
        maxLines = 10,
        isTextInputValid = { true },
        onDismiss = { editing = false },
        onAccept = {
            transaction {
                runCatching {
                    currentEnsureSongInserted()

                    Database.instance.upsert(
                        if (shouldShowSynchronizedLyrics) Lyrics(
                            songId = mediaId,
                            fixed = lyrics?.fixed,
                            synced = it
                        ) else Lyrics(
                            songId = mediaId,
                            fixed = it,
                            synced = lyrics?.synced
                        )
                    )
                }
            }
        }
    )

    if (picking && shouldShowSynchronizedLyrics) {
        var query by rememberSaveable {
            mutableStateOf(
                currentMediaMetadataProvider().title?.toString().orEmpty().let {
                    if (mediaId.startsWith(LOCAL_KEY_PREFIX)) it
                        .substringBeforeLast('.')
                        .trim()
                    else it
                }
            )
        }

        LrcLibSearchDialog(
            query = query,
            setQuery = { query = it },
            onDismiss = { picking = false },
            onPick = {
                runCatching {
                    transaction {
                        Database.instance.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = lyrics?.fixed,
                                synced = it.syncedLyrics
                            )
                        )
                    }
                }
            }
        )
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .fillMaxSize()
            .background(colorPalette.overlay)
    ) {
        val animatedHeight by animateDpAsState(
            targetValue = this.maxHeight,
            label = ""
        )

        AnimatedVisibility(
            visible = error,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(
                    if (shouldShowSynchronizedLyrics) R.string.synchronized_lyrics_not_available
                    else R.string.lyrics_not_available
                ),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                maxLines = if (pip) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(
            visible = !text.isNullOrBlank() && !error && invalidLrc && shouldShowSynchronizedLyrics && !wordSyncedAvailable,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(R.string.invalid_synchronized_lyrics),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                maxLines = if (pip) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }

        val lyricsState = rememberSaveable(text) {
            val syncedText = text?.takeIf { it.isNotBlank() }
            val isJson = isWordLevelJson(syncedText)

            if (isJson) {
                // If it's word-level JSON, provide default state
                SynchronizedLyricsState(sentences = null, offset = 0L)
            } else {
                val file = syncedText?.let { LrcParser.parse(it)?.toLrcFile() }
                SynchronizedLyricsState(
                    sentences = file?.lines,
                    offset = file?.offset?.toDouble(DurationUnit.MILLISECONDS)?.toLong() ?: 0L
                )
            }
        }

        val synchronizedLyrics = remember(lyricsState) {
            invalidLrc = lyricsState.sentences == null && !isWordLevelJson(text)
            lyricsState.sentences?.let {
                SynchronizedLyrics(it.toImmutableMap()) {
                    binder?.player?.let { player ->
                        val currentPosition: Long = player.currentPosition
                        val updateDelayMs: Long = UPDATE_DELAY.toDouble(DurationUnit.MILLISECONDS).toLong()
                        val offset: Long = lyricsState.offset
                        val startTime: Long = lyrics?.startTime ?: 0L
                        currentPosition + updateDelayMs + offset - startTime
                    } ?: 0L
                }
            }
        }

        AnimatedContent(
            targetState = showSynchronizedLyrics,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = ""
        ) { synchronized ->
            when {
                // Case 1: Word-level sync is available and requested
                synchronized && wordSyncedAvailable && wordSyncedManager != null -> {
                    LaunchedEffect(Unit) {
                        while (isActive) {
                            wordSyncedManager?.updatePosition()
                            delay(UPDATE_DELAY)
                        }
                    }

                    AppleStyleLyrics(
                        manager = wordSyncedManager!!,
                        isVisible = isDisplayed,
                        isFullScreen = true
                    )
                }

                // Case 2: Line-level sync is parsed and ready
                synchronized && synchronizedLyrics != null -> {
                    val lazyListState = rememberLazyListState()

                    LaunchedEffect(synchronizedLyrics, density, animatedHeight) {
                        val currentSynchronizedLyrics = synchronizedLyrics
                        val centerOffset = with(density) { (-animatedHeight / 3).roundToPx() }

                        lazyListState.animateScrollToItem(
                            index = currentSynchronizedLyrics.index + 1,
                            scrollOffset = centerOffset
                        )

                        while (true) {
                            delay(UPDATE_DELAY)
                            if (!currentSynchronizedLyrics.update()) continue

                            lazyListState.animateScrollToItem(
                                index = currentSynchronizedLyrics.index + 1,
                                scrollOffset = centerOffset
                            )
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = false,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .verticalFadingEdge()
                            .fillMaxWidth()
                    ) {
                        item(key = "header", contentType = 0) {
                            Spacer(modifier = Modifier.height(maxHeight))
                        }

                        itemsIndexed(
                            items = synchronizedLyrics.sentences.values.toImmutableList()
                        ) { index, sentence ->
                            val color by animateColorAsState(
                                if (index == synchronizedLyrics.index) Color.White
                                else colorPalette.textDisabled
                            )

                            if (sentence.isBlank()) {
                                Image(
                                    painter = painterResource(R.drawable.musical_notes),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(color),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp, horizontal = 32.dp)
                                        .size(typography.xs.fontSize.dp)
                                )
                            } else {
                                BasicText(
                                    text = sentence,
                                    style = typography.xs.center.medium.color(color),
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp)
                                )
                            }
                        }

                        item(key = "footer", contentType = 0) {
                            Spacer(modifier = Modifier.height(maxHeight))
                        }
                    }
                }

                // Case 3: Unsynchronized mode is explicitly selected
                !synchronized -> {
                    // Unsynchronized mode - show non-JSON text.
                    // We add isLrcFormat check as a final safeguard against flicker.
                    val displayText = if (!isWordLevelJson(text) && !isLrcFormat(text) && !text.isNullOrBlank()) {
                        text
                    } else {
                        "" // Do not show anything if it's JSON or LRC format
                    }

                    BasicText(
                        text = displayText,
                        style = typography.xs.center.medium.color(colorPalette.onOverlay),
                        modifier = Modifier
                            .verticalFadingEdge()
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(vertical = maxHeight / 4, horizontal = 32.dp)
                    )
                }
            }
        }

        if (text == null && !error && !wordSyncedAvailable) Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.shimmer()
        ) {
            repeat(4) {
                TextPlaceholder(
                    color = colorPalette.onOverlayShimmer,
                    modifier = Modifier.alpha(1f - it * 0.2f)
                )
            }
        }

        if (showControls) {
            if (onOpenDialog != null) Image(
                painter = painterResource(R.drawable.expand),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            onOpenDialog()
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomStart)
            )

            Image(
                painter = painterResource(R.drawable.ellipsis_horizontal),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            onMenuLaunch()
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.time,
                                        text = stringResource(
                                            if (shouldShowSynchronizedLyrics) R.string.show_unsynchronized_lyrics
                                            else R.string.show_synchronized_lyrics
                                        ),
                                        onClick = {
                                            menuState.hide()
                                            setShouldShowSynchronizedLyrics(!shouldShowSynchronizedLyrics)
                                        }
                                    )

                                    // Karaoke/Apple style now default: no toggle needed

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = stringResource(R.string.edit_lyrics),
                                        onClick = {
                                            menuState.hide()
                                            editing = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = stringResource(R.string.search_lyrics_online),
                                        onClick = {
                                            menuState.hide()
                                            val mediaMetadata = currentMediaMetadataProvider()

                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                        putExtra(
                                                            SearchManager.QUERY,
                                                            "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                        )
                                                    }
                                                )
                                            } catch (_: ActivityNotFoundException) {
                                                context.toast(context.getString(R.string.no_browser_installed))
                                            }
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.sync,
                                        text = stringResource(R.string.refetch_lyrics),
                                        enabled = lyrics != null,
                                        onClick = {
                                            menuState.hide()

                                            // When refetching, clear the file cache for this song.
                                            try {
                                                File(File(context.cacheDir, "lyrics"), "${mediaId}_word.json").delete()
                                            } catch (e: Exception) { e.printStackTrace() }

                                            // Clear word sync cache
                                            wordSyncCache.remove(mediaId)
                                            wordSyncedManager = null
                                            wordSyncedAvailable = false

                                            transaction {
                                                runCatching {
                                                    currentEnsureSongInserted()

                                                    // Clear both fixed and synced to force refetch
                                                    Database.instance.upsert(
                                                        Lyrics(
                                                            songId = mediaId,
                                                            fixed = null,
                                                            synced = null
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    if (shouldShowSynchronizedLyrics) {
                                        MenuEntry(
                                            icon = R.drawable.download,
                                            text = stringResource(R.string.pick_from_lrclib),
                                            onClick = {
                                                menuState.hide()
                                                picking = true
                                            }
                                        )
                                        MenuEntry(
                                            icon = R.drawable.play_skip_forward,
                                            text = stringResource(R.string.set_lyrics_start_offset),
                                            secondaryText = stringResource(
                                                R.string.set_lyrics_start_offset_description
                                            ),
                                            onClick = {
                                                menuState.hide()
                                                lyrics?.let {
                                                    val startTime = binder?.player?.currentPosition
                                                    query {
                                                        Database.instance.upsert(it.copy(startTime = startTime))
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun LrcLibSearchDialog(
    query: String,
    setQuery: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (Track) -> Unit,
    modifier: Modifier = Modifier
) = DefaultDialog(
    onDismiss = onDismiss,
    horizontalPadding = 0.dp,
    modifier = modifier
) {
    val (_, typography) = LocalAppearance.current

    val tracks = remember { mutableStateListOf<Track>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        loading = true
        error = false

        delay(1000)

        LrcLib.lyrics(
            query = query,
            synced = true
        )?.onSuccess { newTracks ->
            tracks.clear()
            tracks.addAll(newTracks.filter { !it.syncedLyrics.isNullOrBlank() })
            loading = false
            error = false
        }?.onFailure {
            loading = false
            error = true
            it.printStackTrace()
        } ?: run { loading = false }
    }

    TextField(
        value = query,
        onValueChange = setQuery,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        maxLines = 1,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))

    when {
        loading -> CircularProgressIndicator(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        error || tracks.isEmpty() -> BasicText(
            text = stringResource(R.string.no_lyrics_found),
            style = typography.s.semiBold.center,
            modifier = Modifier
                .padding(all = 24.dp)
                .align(Alignment.CenterHorizontally)
        )

        else -> ValueSelectorDialogBody(
            onDismiss = onDismiss,
            title = stringResource(R.string.choose_lyric_track),
            selectedValue = null,
            values = tracks.toImmutableList(),
            onValueSelect = {
                transaction {
                    onPick(it)
                    onDismiss()
                }
            },
            valueText = { track ->
                val seconds = track.duration.toInt()
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                "${track.artistName} - ${track.trackName} ($minutes:${remainingSeconds.toString().padStart(2, '0')})"
            }
        )
    }
}
