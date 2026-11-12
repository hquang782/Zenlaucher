package com.zen.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat

/** Launchable apps from every profile (personal + work), via LauncherApps. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private var callback: LauncherApps.Callback? = null
    private var profileReceiver: BroadcastReceiver? = null

    /** Reads labels from other packages: call off the main thread. */
    fun load(): List<AppEntry> = launcherApps.profiles.flatMap { user ->
        val serial = userManager.getSerialNumberForUser(user)
        launcherApps.getActivityList(null, user).map { info ->
            AppEntry(
                // Badged: work-profile copies read "Work Chrome", not a second "Chrome".
                label = context.packageManager.getUserBadgedLabel(info.label, user).toString(),
                packageName = info.componentName.packageName,
                key = "${info.componentName.flattenToString()}#$serial",
            )
        }
    }.also { cached = it }

    /** False when the app is gone or its profile cannot start it. */
    fun launch(key: String, bounds: Rect?): Boolean = withTarget(key) { component, user ->
        launcherApps.startMainActivity(component, user, bounds, null)
    }

    fun openAppInfo(key: String, bounds: Rect?): Boolean = withTarget(key) { component, user ->
        launcherApps.startAppDetailsActivity(component, user, bounds, null)
    }

    /** [onChange] runs on the main thread whenever an app is installed, removed or updated. */
    fun observe(onChange: () -> Unit) {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()
            override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()
            override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = onChange()
        }
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
        callback = cb
        // LauncherApps sends no package events when a whole work profile is added or removed.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = onChange()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        }
        // EXPORTED: protected broadcasts only the system can send; NOT_EXPORTED blocks them on API < 33.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        profileReceiver = receiver
    }

    fun stopObserving() {
        callback?.let(launcherApps::unregisterCallback)
        callback = null
        profileReceiver?.let(context::unregisterReceiver)
        profileReceiver = null
    }

    companion object {
        /** Last loaded list, so a newly opened screen can render before its own reload finishes. */
        @Volatile
        var cached: List<AppEntry> = emptyList()
            private set
    }

    private fun withTarget(key: String, start: (ComponentName, UserHandle) -> Unit): Boolean {
        val split = key.lastIndexOf('#')
        val component = ComponentName.unflattenFromString(key.substring(0, split.coerceAtLeast(0))) ?: return false
        val user = key.substring(split + 1).toLongOrNull()?.let(userManager::getUserForSerialNumber) ?: return false
        return try {
            start(component, user)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
