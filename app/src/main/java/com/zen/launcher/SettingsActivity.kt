package com.zen.launcher

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.zen.launcher.databinding.ActivitySettingsBinding
import kotlin.concurrent.thread

/** Listed in the app drawer: the way back to settings once ZenLauncher is the home app. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var hidden: HiddenApps
    private lateinit var zen: ZenMode
    private lateinit var display: DisplayTweaks
    private var allApps = emptyList<AppEntry>()
    private val adapter = AppListAdapter(onClick = { app, _ ->
        hidden.set(app.key, false)
        render()
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        zenEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSystemBars()
        hidden = HiddenApps(this)

        binding.hiddenList.adapter = adapter
        binding.setDefaultHome.setOnClickListener { openHomeSettings() }
        zen = ZenMode(this)
        binding.allowedApps.setOnClickListener { pickAllowedApps() }
        display = DisplayTweaks(this)
        binding.adbCommand.text = display.grantCommand
        binding.adbHint.setOnClickListener {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("adb", display.grantCommand))
            // Android 13+ confirms clipboard copies itself.
            if (Build.VERSION.SDK_INT < 33) Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
        allApps = AppRepository.cached

        val repo = AppRepository(this)
        thread {
            val apps = repo.load()
            runOnUiThread {
                if (!isDestroyed) {
                    allApps = apps
                    render()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderZen()
        renderDisplay() // the adb grant or Android settings may have changed meanwhile
    }

    private fun renderZen() {
        binding.zenSwitch.bind(zen.active) { zen.setEnabled(it) } // onResume re-syncs after the access screen
        binding.allowedApps.text = getString(R.string.allowed_apps, zen.allowed.size)
    }

    private fun renderDisplay() {
        val canWrite = display.canWrite
        binding.adbHint.isVisible = !canWrite
        binding.grayscaleSwitch.isEnabled = canWrite
        binding.animationsSwitch.isEnabled = canWrite
        // Read back from the system (no permission needed): the user may change these in Android settings.
        binding.grayscaleSwitch.bind(display.grayscale) { writeDisplay { display.grayscale = it } }
        binding.animationsSwitch.bind(display.animationsOff) { writeDisplay { display.animationsOff = it } }
    }

    private fun writeDisplay(write: () -> Unit) {
        try {
            write()
        } catch (e: SecurityException) {
            renderDisplay() // grant revoked or blocked by the ROM: show the real state and the adb hint
        }
    }

    /** Detached while syncing, so only the user's tap or drag runs [onChange]. */
    private fun SwitchCompat.bind(checked: Boolean, onChange: (Boolean) -> Unit) {
        setOnCheckedChangeListener(null)
        isChecked = checked
        setOnCheckedChangeListener { _, on -> onChange(on) }
    }

    private fun pickAllowedApps() {
        // Shortest label first, so a package in both profiles shows as "Chrome", not "Work Chrome".
        val apps = allApps.filter { it.packageName != packageName }
            .sortedBy { it.label.length }.distinctBy { it.packageName }.sortedBy { fold(it.label) }
        val allowed = zen.allowed.toMutableSet()
        val checked = BooleanArray(apps.size) { apps[it].packageName in allowed }
        AlertDialog.Builder(this)
            .setTitle(R.string.allowed_title)
            .setMultiChoiceItems(apps.map { it.label }.toTypedArray(), checked) { _, which, isChecked ->
                if (isChecked) allowed += apps[which].packageName else allowed -= apps[which].packageName
            }
            .setPositiveButton(R.string.save) { _, _ ->
                zen.allowed = allowed
                renderZen()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun render() {
        val list = hiddenApps(allApps, hidden.all())
        adapter.submitList(list)
        binding.empty.isVisible = list.isEmpty()
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS)) // some OEMs lack the home picker screen
        }
    }
}
