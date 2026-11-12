package com.zen.launcher

import android.app.Notification
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * While Zen mode is on: DND in priority mode, and notifications from apps not on the allowed
 * list are dismissed. Bound by the system once the user grants notification access.
 */
class ZenListener : NotificationListenerService() {
    private val zen by lazy { ZenMode(this) }
    // Keys already counted: an app updating one notification (progress, new messages) counts once.
    private val counted = HashSet<String>()
    private val onPrefChange = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ZenMode.KEY_ENABLED) sync()
    }

    override fun onListenerConnected() {
        zen.prefs.registerOnSharedPreferenceChangeListener(onPrefChange)
        sync()
    }

    override fun onListenerDisconnected() {
        zen.prefs.unregisterOnSharedPreferenceChangeListener(onPrefChange)
    }

    // Some Android versions destroy the service without onListenerDisconnected.
    override fun onDestroy() {
        zen.prefs.unregisterOnSharedPreferenceChangeListener(onPrefChange)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (zen.enabled) dismissIfBlocked(sbn, zen.allowed)
    }

    private fun sync() {
        if (zen.enabled) {
            if (zen.savedFilter == null) {
                val current = currentInterruptionFilter
                zen.savedFilter = if (current == INTERRUPTION_FILTER_UNKNOWN) INTERRUPTION_FILTER_ALL else current
                requestInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
            }
            val allowed = zen.allowed
            activeNotifications?.forEach { dismissIfBlocked(it, allowed) }
        } else {
            counted.clear()
            val saved = zen.savedFilter ?: return
            // The user changed DND themselves while Zen mode was on: keep their choice.
            if (currentInterruptionFilter == INTERRUPTION_FILTER_PRIORITY) requestInterruptionFilter(saved)
            zen.savedFilter = null
        }
    }

    private fun dismissIfBlocked(sbn: StatusBarNotification, allowed: Set<String>) {
        if (!shouldBlock(sbn.packageName, sbn.notification.category, sbn.isClearable, allowed, packageName)) return
        cancelNotification(sbn.key)
        // A chat message posts a summary too: count it once.
        val summary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        if (!summary && counted.add(sbn.key)) zen.countBlocked()
    }
}
