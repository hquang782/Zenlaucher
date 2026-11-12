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

/** Swaps [key] with the nearest [visible] pin above or below it; pins the user cannot see stay put. */
fun movePin(pinned: List<String>, key: String, up: Boolean, visible: Set<String>): List<String> {
    val from = pinned.indexOf(key)
    if (from < 0) return pinned
    val range = if (up) (from - 1 downTo 0) else (from + 1 until pinned.size)
    val to = range.firstOrNull { pinned[it] in visible } ?: return pinned
    return pinned.toMutableList().apply { this[from] = this[to].also { this[to] = key } }
}

private val separators = Regex("[\t\r\n]")

/** One app per line, fields tab-separated. Keys and package names never contain tabs or line breaks. */
fun encodeApps(apps: List<AppEntry>): String =
    apps.joinToString("\n") { "${separators.replace(it.label, " ")}\t${it.packageName}\t${it.key}" }

fun decodeApps(text: String): List<AppEntry> = text.lineSequence().mapNotNull { line ->
    line.split('\t').takeIf { it.size == 3 }?.let { AppEntry(it[0], it[1], it[2]) }
}.toList()
