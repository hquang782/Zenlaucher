package com.zen.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings.Global
import android.provider.Settings.Secure
import androidx.core.content.edit

/**
 * Grayscale screen and no animations, written straight into system settings.
 * Both need WRITE_SECURE_SETTINGS, which only adb can grant: see [grantCommand].
 */
class DisplayTweaks(private val context: Context) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("display", Context.MODE_PRIVATE)

    val canWrite get() =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    val grantCommand get() = "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    /** Writes throw SecurityException when the grant is missing or the ROM blocks it. */
    var grayscale: Boolean
        get() = Secure.getInt(resolver, DALTONIZER_ENABLED, 0) == 1 && Secure.getInt(resolver, DALTONIZER, -1) == MONOCHROMACY
        set(on) {
            if (on) {
                // Grayscale reuses Android's color correction: keep the user's own mode to give it back.
                prefs.edit {
                    putInt(DALTONIZER_ENABLED, Secure.getInt(resolver, DALTONIZER_ENABLED, 0))
                    putInt(DALTONIZER, Secure.getInt(resolver, DALTONIZER, -1))
                }
                Secure.putInt(resolver, DALTONIZER, MONOCHROMACY)
                Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
            } else {
                val mode = prefs.getInt(DALTONIZER, -1)
                if (mode >= 0) Secure.putInt(resolver, DALTONIZER, mode)
                val restore = restoresColorCorrection(prefs.getInt(DALTONIZER_ENABLED, 0) == 1, mode)
                Secure.putInt(resolver, DALTONIZER_ENABLED, if (restore) 1 else 0)
            }
        }

    var animationsOff: Boolean
        get() = ANIMATION_SCALES.all { Global.getFloat(resolver, it, 1f) == 0f }
        set(off) {
            // Remember the user's own scales (e.g. 0.5x from developer options) to put them back.
            if (off) prefs.edit { ANIMATION_SCALES.forEach { putFloat(it, Global.getFloat(resolver, it, 1f)) } }
            ANIMATION_SCALES.forEach { Global.putFloat(resolver, it, if (off) 0f else restoredScale(prefs.getFloat(it, 1f))) }
        }

    companion object {
        private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        private const val DALTONIZER = "accessibility_display_daltonizer"
        private const val MONOCHROMACY = 0
        private val ANIMATION_SCALES = listOf(
            Global.WINDOW_ANIMATION_SCALE, Global.TRANSITION_ANIMATION_SCALE, Global.ANIMATOR_DURATION_SCALE,
        )
    }
}

/** Turn color correction back on only if the user had it on in a mode other than our grayscale. */
fun restoresColorCorrection(wasEnabled: Boolean, savedMode: Int): Boolean = wasEnabled && savedMode != 0

/** A saved scale of 0 (animations were already off) would restore nothing: fall back to normal speed. */
fun restoredScale(saved: Float): Float = if (saved > 0f) saved else 1f
