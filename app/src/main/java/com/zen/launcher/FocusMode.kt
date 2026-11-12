package com.zen.launcher

import android.content.Context
import android.text.format.DateFormat
import androidx.core.content.edit
import java.util.Calendar

/** Focus mode: during a daily time window, chosen apps open from ZenLauncher only after a wait. */
class FocusMode(context: Context) {
    private val prefs = context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    /** Minutes after midnight. */
    var start: Int
        get() = prefs.getInt(KEY_START, 8 * 60)
        set(value) = prefs.edit { putInt(KEY_START, value) }

    var end: Int
        get() = prefs.getInt(KEY_END, 17 * 60)
        set(value) = prefs.edit { putInt(KEY_END, value) }

    /** [Calendar.DAY_OF_WEEK] values; Monday to Friday until the user picks. */
    var days: Set<Int>
        get() = prefs.getStringSet(KEY_DAYS, null)?.mapTo(HashSet(), String::toInt) ?: WEEKDAYS
        set(value) = prefs.edit { putStringSet(KEY_DAYS, value.mapTo(HashSet(), Int::toString)) }

    /** Package names locked during the window. */
    var apps: Set<String>
        get() = prefs.getStringSet(KEY_APPS, null)?.toSet() ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_APPS, value) }

    fun locks(packageName: String, now: Calendar = Calendar.getInstance()): Boolean =
        enabled && packageName in apps && isLocked(
            now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE), now.get(Calendar.DAY_OF_WEEK), start, end, days,
        )

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_START = "start"
        const val KEY_END = "end"
        const val KEY_DAYS = "days"
        const val KEY_APPS = "apps"
        val WEEKDAYS = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
    }
}

/**
 * Whether [minute] (after midnight) on [day] falls in the window [start, end). A window that
 * crosses midnight belongs to the day it starts on; [start] == [end] is 24 hours from [start],
 * so "locked until <end>" stays true.
 */
fun isLocked(minute: Int, day: Int, start: Int, end: Int, days: Set<Int>): Boolean {
    val previousDay = if (day == Calendar.SUNDAY) Calendar.SATURDAY else day - 1
    return when {
        start < end -> day in days && minute in start until end
        else -> (day in days && minute >= start) || (previousDay in days && minute < end)
    }
}

/** [minutes] after midnight in the phone's own 12/24-hour format. */
fun formatMinutes(context: Context, minutes: Int): String {
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    return DateFormat.getTimeFormat(context).format(time.time)
}
