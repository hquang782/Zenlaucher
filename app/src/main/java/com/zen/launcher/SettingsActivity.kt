package com.zen.launcher

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.zen.launcher.databinding.ActivitySettingsBinding
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlin.concurrent.thread

/** Listed in the app drawer: the way back to settings once ZenLauncher is the home app. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var hidden: HiddenApps
    private lateinit var zen: ZenMode
    private lateinit var display: DisplayTweaks
    private lateinit var focus: FocusMode
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
        binding.allowedApps.setOnClickListener {
            pickApps(R.string.allowed_title, zen.allowed) {
                zen.allowed = it
                renderZen()
            }
        }
        focus = FocusMode(this)
        binding.focusTime.setOnClickListener { pickFocusHours() }
        binding.focusDays.setOnClickListener { pickFocusDays() }
        binding.focusApps.setOnClickListener {
            pickApps(R.string.focus_apps_title, focus.apps) {
                focus.apps = it
                renderFocus()
            }
        }
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
        renderFocus()
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

    private fun renderFocus() {
        binding.focusSwitch.bind(focus.enabled) { focus.enabled = it }
        binding.focusSummary.text = AppActions.OPEN_ANYWAY_DELAY_SECONDS.let { resources.getQuantityString(R.plurals.focus_summary, it, it) }
        binding.focusTime.text = getString(R.string.focus_time, formatMinutes(this, focus.start), formatMinutes(this, focus.end))
        val names = DateFormatSymbols.getInstance().shortWeekdays
        binding.focusDays.text = getString(R.string.focus_days, WEEK.filter { it in focus.days }.joinToString { names[it] })
        binding.focusApps.text = getString(R.string.focus_apps, focus.apps.size)
    }

    private fun pickFocusHours() {
        val is24 = DateFormat.is24HourFormat(this)
        TimePickerDialog(this, { _, h, m ->
            TimePickerDialog(this, { _, h2, m2 ->
                // Saved together: cancelling the second picker must not leave a half-edited window.
                focus.start = h * 60 + m
                focus.end = h2 * 60 + m2
                renderFocus()
            }, focus.end / 60, focus.end % 60, is24).apply { setTitle(R.string.focus_end) }.show()
        }, focus.start / 60, focus.start % 60, is24).apply { setTitle(R.string.focus_start) }.show()
    }

    private fun pickFocusDays() {
        val names = DateFormatSymbols.getInstance().weekdays
        val days = focus.days.toMutableSet()
        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.focus_days_title)
            .setMultiChoiceItems(WEEK.map { names[it] }.toTypedArray(), BooleanArray(WEEK.size) { WEEK[it] in days }) { _, which, on ->
                if (on) days += WEEK[which] else days -= WEEK[which]
                // No days would silently turn Focus mode off while its switch still says on.
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = days.isNotEmpty()
            }
            .setPositiveButton(R.string.save) { _, _ ->
                focus.days = days
                renderFocus()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Multi-choice list of installed apps by package; [save] gets the ticked package names. */
    private fun pickApps(title: Int, current: Set<String>, save: (Set<String>) -> Unit) {
        // Shortest label first, so a package in both profiles shows as "Chrome", not "Work Chrome".
        val apps = allApps.filter { it.packageName != packageName }
            .sortedBy { it.label.length }.distinctBy { it.packageName }.sortedBy { fold(it.label) }
        val chosen = current.toMutableSet()
        val checked = BooleanArray(apps.size) { apps[it].packageName in chosen }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMultiChoiceItems(apps.map { it.label }.toTypedArray(), checked) { _, which, isChecked ->
                if (isChecked) chosen += apps[which].packageName else chosen -= apps[which].packageName
            }
            .setPositiveButton(R.string.save) { _, _ -> save(chosen) }
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

    private companion object {
        /** Monday first, as Vietnamese calendars show it. */
        val WEEK = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
        )
    }
}
