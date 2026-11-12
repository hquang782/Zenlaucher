package com.zen.launcher

import android.app.Notification
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

/** Zen mode state. Changes reach [ZenListener] and Home through the shared preferences file. */
class ZenMode(private val context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("zen_mode", Context.MODE_PRIVATE)

    val enabled get() = prefs.getBoolean(KEY_ENABLED, false)

    /** Zen mode can only act once the user grants notification access to [ZenListener]. */
    val hasAccess get() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** On and able to act: what the UI shows. */
    val active get() = enabled && hasAccess

    /** Package names whose notifications pass while Zen mode is on. */
    var allowed: Set<String>
        get() = prefs.getStringSet(KEY_ALLOWED, null)?.toSet() ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_ALLOWED, value) }

    /** Notifications dismissed since Zen mode was last turned on. */
    val blocked get() = prefs.getInt(KEY_BLOCKED, 0)

    /**
     * Turning on without notification access opens the screen to grant it instead, and clears a
     * stale "on" left by revoked access so the listener restores DND once it reconnects.
     */
    fun setEnabled(on: Boolean) {
        if (on && !hasAccess) {
            prefs.edit { putBoolean(KEY_ENABLED, false) }
            return openAccessSettings()
        }
        prefs.edit {
            putBoolean(KEY_ENABLED, on)
            if (on) putInt(KEY_BLOCKED, 0)
        }
    }

    private fun openAccessSettings() {
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        try {
            context.startActivity(
                if (Build.VERSION.SDK_INT >= 30) {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(context, ZenListener::class.java).flattenToString(),
                    )
                } else {
                    list
                },
            )
        } catch (e: ActivityNotFoundException) {
            context.startActivity(list) // some OEMs lack the detail screen
        }
    }

    fun countBlocked() = prefs.edit { putInt(KEY_BLOCKED, blocked + 1) }

    /** The DND filter to restore on exit, or null when Zen mode has not changed DND. */
    var savedFilter: Int?
        get() = prefs.getInt(KEY_SAVED_FILTER, NONE).takeIf { it != NONE }
        set(value) = prefs.edit { putInt(KEY_SAVED_FILTER, value ?: NONE) }

    companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_BLOCKED = "blocked"
        private const val KEY_ALLOWED = "allowed"
        private const val KEY_SAVED_FILTER = "saved_filter"
        private const val NONE = -1
    }
}

private val alwaysAllowedPackages = setOf("android", "com.android.systemui")
private val alwaysAllowedCategories = setOf(
    Notification.CATEGORY_CALL, Notification.CATEGORY_ALARM, Notification.CATEGORY_SYSTEM,
)

/** Whether Zen mode dismisses this notification. Ones the user cannot clear (media, navigation) stay. */
fun shouldBlock(
    packageName: String,
    category: String?,
    clearable: Boolean,
    allowed: Set<String>,
    ownPackage: String,
): Boolean = clearable &&
    category !in alwaysAllowedCategories &&
    packageName != ownPackage &&
    packageName !in alwaysAllowedPackages &&
    packageName !in allowed
