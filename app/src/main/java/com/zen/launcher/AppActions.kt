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
                val next = togglePin(favorites.all(), app.key, installedApps().mapTo(HashSet()) { it.key })
                if (next == null) {
                    toast(activity.getString(R.string.pins_full, MAX_PINNED))
                } else {
                    favorites.save(next)
                    onChanged()
                }
                true
            }
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

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()

    private fun View.boundsOnScreen(): Rect {
        val xy = IntArray(2).also(::getLocationOnScreen)
        return Rect(xy[0], xy[1], xy[0] + width, xy[1] + height)
    }
}
