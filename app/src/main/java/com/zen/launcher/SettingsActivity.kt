package com.zen.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.zen.launcher.databinding.ActivitySettingsBinding
import kotlin.concurrent.thread

/** Listed in the app drawer: the way back to settings once ZenLauncher is the home app. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var hidden: HiddenApps
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
