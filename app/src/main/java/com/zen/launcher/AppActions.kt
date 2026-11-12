package com.zen.launcher

import android.graphics.Rect
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

    fun launch(app: AppEntry, view: View): Boolean {
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

    private fun View.boundsOnScreen(): Rect {
        val xy = IntArray(2).also(::getLocationOnScreen)
        return Rect(xy[0], xy[1], xy[0] + width, xy[1] + height)
    }
}
