package com.zen.launcher

import android.graphics.Rect
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Tap and long-press behavior shared by app rows on Home and in the drawer. */
class AppActions(
    private val activity: AppCompatActivity,
    private val repo: AppRepository,
    private val installedApps: () -> List<AppEntry>,
    private val onChanged: () -> Unit,
    /** Home shows pins in order, so only there can moving one be seen. */
    private val canReorder: Boolean = false,
) {
    val hidden = HiddenApps(activity)
    val favorites = Favorites(activity)
    private val focus = FocusMode(activity)

    fun launch(app: AppEntry, view: View): Boolean {
        if (focus.locks(app.packageName)) {
            confirmLocked(app, view)
            return false
        }
        return openNow(app, view)
    }

    private fun openNow(app: AppEntry, view: View): Boolean {
        val ok = repo.launch(app.key, view.boundsOnScreen())
        if (!ok) toast(activity.getString(R.string.cannot_open, app.label))
        return ok
    }

    fun showMenu(app: AppEntry, view: View) {
        PopupMenu(activity, view).apply {
            val pinned = app.key in favorites.all()
            menu.add(if (pinned) R.string.unpin else R.string.pin).setOnMenuItemClickListener {
                // Only the latest real list may drop pins: the saved one or an older one may miss apps.
                val installed = installedApps().mapTo(HashSet()) { it.key }
                if (!AppRepository.fresh || installedApps() !== AppRepository.cached) installed += favorites.all()
                val next = togglePin(favorites.all(), app.key, installed)
                if (next == null) {
                    toast(activity.getString(R.string.pins_full, MAX_PINNED))
                } else {
                    favorites.save(next)
                    onChanged()
                }
                true
            }
            if (pinned && canReorder) addMoveItems(app.key)
            // Hiding our own Settings entry would leave no way to unhide anything.
            if (app.packageName != activity.packageName) {
                menu.add(R.string.hide).setOnMenuItemClickListener {
                    hidden.set(app.key, true)
                    onChanged()
                    true
                }
            }
            menu.add(R.string.app_info).setOnMenuItemClickListener {
                repo.openAppInfo(app.key, view.boundsOnScreen())
            }
            show()
        }
    }

    /** "Open anyway" unlocks after a pause: enough to change your mind, still a way out when it matters. */
    private fun confirmLocked(app: AppEntry, view: View) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.locked_title, app.label, formatMinutes(activity, focus.end)))
            .setMessage(R.string.locked_message)
            .setPositiveButton(R.string.open_anyway) { _, _ -> openNow(app, view) }
            .setNegativeButton(R.string.close, null)
            .show()
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.isEnabled = false
        var left = OPEN_ANYWAY_DELAY_SECONDS
        val tick = object : Runnable {
            override fun run() {
                if (left == 0) {
                    button.isEnabled = true
                    button.setText(R.string.open_anyway)
                } else {
                    button.text = activity.getString(R.string.open_anyway_in, left--)
                    button.postDelayed(this, 1000)
                }
            }
        }
        tick.run()
        // The drawer finishes itself in onStop: close the dialog with it instead of leaking its window.
        val closeOnStop = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) dialog.dismiss() }
        activity.lifecycle.addObserver(closeOnStop)
        dialog.setOnDismissListener {
            button.removeCallbacks(tick)
            activity.lifecycle.removeObserver(closeOnStop)
        }
    }

    private fun PopupMenu.addMoveItems(key: String) {
        fun visibleOrder() = pinnedApps(installedApps(), favorites.all(), hidden.all()).map { it.key }
        val order = visibleOrder()
        val index = order.indexOf(key)
        fun move(up: Boolean): Boolean {
            // Recomputed: the list may have reloaded while the menu was open.
            favorites.save(movePin(favorites.all(), key, up, visibleOrder().toSet()))
            onChanged()
            return true
        }
        if (index > 0) menu.add(R.string.move_up).setOnMenuItemClickListener { move(up = true) }
        if (index in 0 until order.lastIndex) menu.add(R.string.move_down).setOnMenuItemClickListener { move(up = false) }
    }

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()

    companion object {
        const val OPEN_ANYWAY_DELAY_SECONDS = 10
    }

    private fun View.boundsOnScreen(): Rect {
        val xy = IntArray(2).also(::getLocationOnScreen)
        return Rect(xy[0], xy[1], xy[0] + width, xy[1] + height)
    }
}
