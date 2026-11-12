package com.zen.launcher

import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.zen.launcher.databinding.ActivityDrawerBinding
import kotlin.concurrent.thread
import kotlin.math.abs

/** All apps with search; slides up over Home. */
class DrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawerBinding
    private lateinit var actions: AppActions
    private var allApps = emptyList<AppEntry>()
    private val adapter = AppListAdapter(
        onClick = { app, view -> actions.launch(app, view) },
        onLongClick = { app, view -> actions.showMenu(app, view) },
    )
    private lateinit var gestures: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        zenEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.stay, R.anim.slide_down)
        binding = ActivityDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.padForSystemBars()
        val repo = AppRepository(this)
        actions = AppActions(this, repo, installedApps = { allApps }, onChanged = ::render)

        binding.appList.adapter = adapter
        binding.appList.itemAnimator = null
        binding.search.doAfterTextChanged { render() }
        binding.search.setOnEditorActionListener { _, actionId, event ->
            // IME_NULL = hardware Enter. Recompute: adapter.currentList lags behind the text (async diff).
            val enter = actionId == EditorInfo.IME_ACTION_GO ||
                (actionId == EditorInfo.IME_NULL && event?.action == KeyEvent.ACTION_DOWN)
            val first = visibleApps(allApps, actions.hidden.all(), binding.search.text.toString()).firstOrNull()
            if (enter && first != null) {
                actions.launch(first, binding.search)
                true
            } else {
                false
            }
        }
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val atTop = !binding.appList.canScrollVertically(-1)
                if (atTop && velocityY > FLING_VELOCITY && velocityY > abs(velocityX)) finish()
                return false
            }
        })
        if (intent.getBooleanExtra(EXTRA_SEARCH, false)) {
            binding.search.requestFocus()
            WindowCompat.getInsetsController(window, binding.search).show(WindowInsetsCompat.Type.ime())
        }

        // Render the cached list at once, then refresh it.
        allApps = AppRepository.cached
        render()
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onStop() {
        super.onStop()
        // An app was opened or Home was pressed: come back to Home, not to the drawer.
        if (!isChangingConfigurations) finish()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < 34) {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.stay, R.anim.slide_down)
        }
    }

    private fun render() {
        adapter.submitList(visibleApps(allApps, actions.hidden.all(), binding.search.text.toString()))
    }

    companion object {
        const val EXTRA_SEARCH = "search"
        private const val FLING_VELOCITY = 1200f
    }
}
