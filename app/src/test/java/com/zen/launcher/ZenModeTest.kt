package com.zen.launcher

import android.app.Notification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenModeTest {
    private val allowed = setOf("com.zing.zalo")

    private fun block(pkg: String, category: String? = null, clearable: Boolean = true) =
        shouldBlock(pkg, category, clearable, allowed, ownPackage = "com.zen.launcher")

    @Test
    fun `blocks a normal notification from an app not on the allowed list`() {
        assertTrue(block("com.facebook.katana"))
    }

    @Test
    fun `keeps allowed apps and our own notifications`() {
        assertFalse(block("com.zing.zalo"))
        assertFalse(block("com.zen.launcher"))
    }

    @Test
    fun `never blocks calls, alarms, system or ongoing notifications`() {
        assertFalse(block("com.whatsapp", Notification.CATEGORY_CALL))
        assertFalse(block("com.google.android.deskclock", Notification.CATEGORY_ALARM))
        assertFalse(block("com.oem.battery", Notification.CATEGORY_SYSTEM))
        assertFalse(block("android"))
        assertFalse(block("com.android.systemui"))
        assertFalse(block("com.spotify.music", clearable = false))
    }
}
