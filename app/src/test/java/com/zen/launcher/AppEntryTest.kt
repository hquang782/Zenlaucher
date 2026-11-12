package com.zen.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppEntryTest {
    private val zalo = AppEntry("Zalo", "com.zing.zalo", "zalo#0")
    private val phone = AppEntry("Điện thoại", "com.android.dialer", "dialer#0")
    private val chrome = AppEntry("chrome", "com.android.chrome", "chrome#0")
    private val chromeWork = AppEntry("Chrome", "com.android.chrome", "chrome#10")
    private val all = listOf(zalo, phone, chrome, chromeWork)

    @Test
    fun `visible apps are sorted case-insensitively and exclude hidden`() {
        assertEquals(
            listOf(chrome, chromeWork, phone),
            visibleApps(all, hidden = setOf("zalo#0"), query = ""),
        )
    }

    @Test
    fun `query matches ignoring case, diacritics and surrounding spaces`() {
        assertEquals(listOf(phone), visibleApps(all, emptySet(), "  dien THOAI "))
        assertEquals(listOf(zalo), visibleApps(all, emptySet(), "ALO"))
    }

    @Test
    fun `hidden key only hides that profile's entry`() {
        assertEquals(listOf(chromeWork), visibleApps(all, setOf("chrome#0"), "chrome"))
    }

    @Test
    fun `pinned apps keep pin order and skip uninstalled or hidden`() {
        assertEquals(
            listOf(zalo, chrome),
            pinnedApps(all, pinned = listOf("zalo#0", "gone#0", "dialer#0", "chrome#0"), hidden = setOf("dialer#0")),
        )
    }

    @Test
    fun `togglePin adds to the end and removes when already pinned`() {
        val installed = all.map { it.key }.toSet()
        assertEquals(listOf("zalo#0", "chrome#0"), togglePin(listOf("zalo#0"), "chrome#0", installed))
        assertEquals(listOf("chrome#0"), togglePin(listOf("zalo#0", "chrome#0"), "zalo#0", installed))
    }

    @Test
    fun `togglePin refuses a sixth pin but uninstalled pins do not count`() {
        val five = listOf("a#0", "b#0", "c#0", "d#0", "e#0")
        val installed = five.toSet() + "zalo#0"
        assertNull(togglePin(five, "zalo#0", installed))
        assertEquals(listOf("a#0", "b#0", "c#0", "d#0", "zalo#0"), togglePin(five, "zalo#0", installed - "e#0"))
    }

    @Test
    fun `movePin swaps with the nearest visible pin and keeps the rest in place`() {
        val pinned = listOf("a#0", "hid#0", "b#0", "c#0")
        val visible = setOf("a#0", "b#0", "c#0")
        assertEquals(listOf("b#0", "hid#0", "a#0", "c#0"), movePin(pinned, "b#0", up = true, visible))
        assertEquals(listOf("a#0", "hid#0", "c#0", "b#0"), movePin(pinned, "b#0", up = false, visible))
    }

    @Test
    fun `movePin at either end or for an unknown key changes nothing`() {
        val pinned = listOf("a#0", "b#0")
        val visible = pinned.toSet()
        assertEquals(pinned, movePin(pinned, "a#0", up = true, visible))
        assertEquals(pinned, movePin(pinned, "b#0", up = false, visible))
        assertEquals(pinned, movePin(pinned, "x#0", up = true, visible))
    }

    @Test
    fun `app list survives encode and decode, and tabs or line breaks in labels cannot break it`() {
        val odd = AppEntry("Bad\tlabel\nhere\rnow", "com.odd", "com.odd/.Main#0")
        val decoded = decodeApps(encodeApps(all + odd))
        assertEquals(all + odd.copy(label = "Bad label here now"), decoded)
        assertEquals(emptyList<AppEntry>(), decodeApps(""))
        assertEquals(emptyList<AppEntry>(), decodeApps("garbage line"))
    }

    @Test
    fun `hidden apps lists only hidden entries, sorted`() {
        assertEquals(listOf(phone, zalo), hiddenApps(all, setOf("zalo#0", "dialer#0", "gone#0")))
    }
}
