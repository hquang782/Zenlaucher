package com.zen.launcher

import java.text.Normalizer

/** One launchable activity. [key] = "<flattened component>#<user serial>", unique across profiles. */
data class AppEntry(val label: String, val packageName: String, val key: String)

private val combiningMarks = Regex("\\p{Mn}+")

/** Lowercase without Vietnamese diacritics, so "dien thoai" finds "Điện thoại". */
fun fold(text: String): String =
    combiningMarks.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "").replace('đ', 'd')

private fun List<AppEntry>.sortedByLabel() = sortedBy { fold(it.label) }

fun visibleApps(all: List<AppEntry>, hidden: Set<String>, query: String): List<AppEntry> {
    val q = fold(query.trim())
    return all.filter { it.key !in hidden && fold(it.label).contains(q) }.sortedByLabel()
}

fun hiddenApps(all: List<AppEntry>, hidden: Set<String>): List<AppEntry> =
    all.filter { it.key in hidden }.sortedByLabel()

const val MAX_PINNED = 5

/** Pinned apps in the user's pin order; uninstalled or hidden ones are skipped. */
fun pinnedApps(all: List<AppEntry>, pinned: List<String>, hidden: Set<String>): List<AppEntry> {
    val byKey = all.associateBy { it.key }
    return pinned.filter { it !in hidden }.mapNotNull(byKey::get)
}

/**
 * Unpins [key] if pinned, else appends it. Returns null when [MAX_PINNED] is reached.
 * Pins of apps no longer [installed] are dropped first so they cannot block the limit.
 */
fun togglePin(pinned: List<String>, key: String, installed: Set<String>): List<String>? {
    val live = pinned.filter { it in installed }
    return when {
        key in live -> live - key
        live.size >= MAX_PINNED -> null
        else -> live + key
    }
}
