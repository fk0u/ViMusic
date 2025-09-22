package it.vfsfitvnm.vimusic.utils

import android.content.Context
import it.vfsfitvnm.vimusic.preferences.DataPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages a file-based cache specifically for word-level synchronized lyrics (JSON format).
 */
object LyricsCacheManager {
    private fun getCacheDir(context: Context): File {
        return File(context.cacheDir, "lyrics").apply { mkdirs() }
    }

    private fun getCacheFileForSong(context: Context, songId: String): File {
        return File(getCacheDir(context), "${songId}_word.json")
    }

    suspend fun save(context: Context, songId: String, jsonLyrics: String) {
        withContext(Dispatchers.IO) {
            try {
                getCacheFileForSong(context, songId).writeText(jsonLyrics)
                enforceSizeLimit(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get the cached lyrics for a song
     * @param context Android context
     * @param songId The song ID to retrieve lyrics for
     * @return The cached lyrics as a string, or null if not found
     */
    fun get(context: Context, songId: String): String? {
        val cacheFile = getCacheFileForSong(context, songId)
        return if (cacheFile.exists() && cacheFile.canRead()) {
            try {
                cacheFile.readText()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    fun clear(context: Context): Boolean {
        return try {
            getCacheDir(context).deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getCacheSize(context: Context): Long {
        return try {
            getCacheDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun enforceSizeLimit(context: Context) {
        val maxSize = DataPreferences.lyricsCacheMaxSize.bytes
        val cacheDir = getCacheDir(context)
        var currentSize = getCacheSize(context)

        if (currentSize > maxSize) {
            val files = cacheDir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return

            for (file in files) {
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                    if (currentSize <= maxSize) {
                        break
                    }
                }
            }
        }
    }
}
