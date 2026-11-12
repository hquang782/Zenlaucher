package com.zen.launcher

import android.content.Context
import androidx.core.content.edit

/** Keys of apps the user hid from the home list. */
class HiddenApps(context: Context) {
    private val prefs = context.getSharedPreferences("hidden_apps", Context.MODE_PRIVATE)

    // Copy: the set returned by SharedPreferences must not be modified.
    fun all(): Set<String> = prefs.getStringSet(KEY, null)?.toSet() ?: emptySet()

    fun set(key: String, hidden: Boolean) {
        val keys = all()
        prefs.edit { putStringSet(KEY, if (hidden) keys + key else keys - key) }
    }

    private companion object {
        const val KEY = "keys"
    }
}
