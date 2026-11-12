package com.zen.launcher

import android.content.Context
import androidx.core.content.edit

/** Ordered keys of apps pinned to the home screen. */
class Favorites(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    // A StringSet loses order, so keys are stored newline-joined (keys never contain '\n').
    fun all(): List<String> = prefs.getString(KEY, null)?.split('\n')?.filter(String::isNotEmpty) ?: emptyList()

    fun save(keys: List<String>) = prefs.edit { putString(KEY, keys.joinToString("\n")) }

    private companion object {
        const val KEY = "keys"
    }
}
