package com.zen.launcher

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.zen.launcher.databinding.ActivityHomeBinding
import java.util.concurrent.Executors
import kotlin.math.abs

/** Home: clock and up to [MAX_PINNED] pinned apps. The full list lives in [DrawerActivity]. */
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var repo: AppRepository
    private lateinit var actions: AppActions
    // Single thread so overlapping reloads (e.g. an app update) finish in order.
    private val loader = Executors.newSingleThreadExecutor()
    private var allApps = emptyList<AppEntry>()
    private val adapter = AppListAdapter(
        onClick = { app, view -> actions.launch(app, view) },
        onLongClick = { app, view -> actions.showMenu(app, view) },
    )
    private lateinit var gestures: GestureDetector

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) binding.battery.text = getString(R.string.battery, level * 100 / scale)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        zenEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSystemBars()
        repo = AppRepository(this)
        actions = AppActions(this, repo, installedApps = { allApps }, onChanged = ::render)

        binding.pinnedList.adapter = adapter
        binding.pinnedList.itemAnimator = null
        binding.settings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.navNotifications.setOnClickListener { expandNotifications() }
        binding.navSearch.setOnClickListener { openDrawer(search = true) }
        binding.navAllApps.setOnClickListener { openDrawer(search = false) }
        binding.swipeHint.setOnClickListener { openDrawer(search = false) }
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (abs(velocityY) < abs(velocityX) || abs(velocityY) < FLING_VELOCITY) return false
                if (velocityY < 0) openDrawer(search = false) else expandNotifications()
                return true
            }
        })
        // Home has nowhere to go back to.
        onBackPressedDispatcher.addCallback(this) {}

        allApps = AppRepository.cached
        repo.observe(::reload)
        reload()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev) // observe only: rows still get their taps
        return super.dispatchTouchEvent(ev)
    }

    override fun onStart() {
        super.onStart()
        // EXPORTED: a protected broadcast only the system can send. NOT_EXPORTED adds a signature
        // permission on API < 33 that blocks system broadcasts.
        ContextCompat.registerReceiver(
            this, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        render() // pins or hidden apps may have changed in the drawer or Settings
    }

    override fun onStop() {
        unregisterReceiver(batteryReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        repo.stopObserving()
        loader.shutdownNow()
        super.onDestroy()
    }

    private fun reload() {
        if (isDestroyed) return // a LauncherApps callback queued before unregister can still arrive
        loader.execute {
            val apps = repo.load()
            runOnUiThread {
                if (!isDestroyed) {
                    allApps = apps
                    render()
                }
            }
        }
    }

    private fun render() {
        val pinned = pinnedApps(allApps, actions.favorites.all(), actions.hidden.all())
        adapter.submitList(pinned)
        // Before the first load finishes we cannot tell "no pins" from "not loaded yet".
        binding.pinHint.isVisible = pinned.isEmpty() && allApps.isNotEmpty()
    }

    private fun openDrawer(search: Boolean) {
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_up, R.anim.stay)
        startActivity(
            Intent(this, DrawerActivity::class.java).putExtra(DrawerActivity.EXTRA_SEARCH, search),
            options.toBundle(),
        )
    }

    // shortcut: hidden StatusBarManager API (needs EXPAND_STATUS_BAR); silently does nothing where blocked.
    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandNotifications() {
        try {
            val statusBar = getSystemService("statusbar") ?: return
            statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
        } catch (e: ReflectiveOperationException) {
            // not available on this device
        } catch (e: SecurityException) {
            // not available on this device
        }
    }

    private companion object {
        const val FLING_VELOCITY = 1200f
    }
}
