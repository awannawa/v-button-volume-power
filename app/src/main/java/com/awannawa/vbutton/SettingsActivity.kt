package com.awannawa.vbutton

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Settings screen and launcher (T08, FR-10 to FR-12). Plain platform widgets in a
 * vertical list (DESIGN.md section 3); all copy is in strings.xml. Every change is
 * written to [Prefs] only — the service's Prefs listener forwards it to the overlay's
 * applyConfig, so nothing here touches the overlay or the service directly
 * (AGENTS.md integration points). The status read re-runs in onResume; there is no
 * polling or receiver (NFR-1).
 */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate and any inflation: the DayNight theme is resource-
        // qualified (values-v29), so API 28 resolves the plain DeviceDefault fallback.
        setTheme(R.style.Theme_VButtonSettings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        wireListeners()
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        // A fresh read on every resume is the v1 status refresh (ruling): the user
        // returns from the accessibility settings and the state is re-read. The
        // dead-service warning lives in T09.
        refreshUi()
    }

    private fun wireListeners() {
        // FR-10: opens the system screen where the service toggle lives.
        findViewById<Button>(R.id.enable_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // FR-10 size preset: the stored string is the desired preset; compact mode
        // may still step the rendered preset down for a short screen (01-ui-spec.md
        // section 4) without rewriting the stored choice.
        findViewById<RadioGroup>(R.id.size_group).setOnCheckedChangeListener { _, checkedId ->
            val preset = when (checkedId) {
                R.id.size_small -> Prefs.SIZE_PRESET_SMALL
                R.id.size_large -> Prefs.SIZE_PRESET_LARGE
                else -> Prefs.SIZE_PRESET_MEDIUM
            }
            Prefs.setSizePreset(this, preset)
        }

        // FR-11: written per tick, so the overlay's alpha follows the thumb live.
        findViewById<SeekBar>(R.id.opacity_seekbar).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    findViewById<TextView>(R.id.opacity_readout).text =
                        getString(R.string.handle_opacity_readout, progress)
                    if (fromUser) {
                        Prefs.setHandleOpacityPercent(this@SettingsActivity, progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        // FR-5: the ruled 1-10 s SeekBar (step 1 s) writes ms; the overlay's collapse
        // timer reads the value at schedule time, so the next expand uses it.
        findViewById<SeekBar>(R.id.timeout_seekbar).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    findViewById<TextView>(R.id.timeout_readout).text =
                        resources.getQuantityString(R.plurals.auto_collapse_readout, progress, progress)
                    if (fromUser) {
                        Prefs.setAutoCollapseTimeoutMs(this@SettingsActivity, progress * 1000L)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        // FR-9: read at press time by the overlay, so a flip applies to the next press.
        findViewById<Switch>(R.id.haptics_switch).setOnCheckedChangeListener { _, checked ->
            Prefs.setHapticsEnabled(this, checked)
        }

        // FR-10 reset position: writes the default ratio only — the overlay's
        // applyConfig re-clamps and repositions from the ratio (the px conversions
        // and clamping live in SafeArea, never here).
        findViewById<Button>(R.id.reset_position_button).setOnClickListener {
            Prefs.setYRatio(this, Prefs.Y_RATIO_DEFAULT)
        }
    }

    /** Re-reads every stored value into the views and the service status. */
    private fun refreshUi() {
        findViewById<TextView>(R.id.status_text)
            .setText(if (isServiceActive()) R.string.status_active else R.string.status_inactive)

        findViewById<RadioGroup>(R.id.size_group).check(
            when (Prefs.sizePreset(this)) {
                Prefs.SIZE_PRESET_SMALL -> R.id.size_small
                Prefs.SIZE_PRESET_LARGE -> R.id.size_large
                else -> R.id.size_medium
            },
        )

        val opacity = Prefs.handleOpacityPercent(this)
        findViewById<SeekBar>(R.id.opacity_seekbar).progress = opacity
        findViewById<TextView>(R.id.opacity_readout).text =
            getString(R.string.handle_opacity_readout, opacity)

        val seconds = (Prefs.autoCollapseTimeoutMs(this) / 1000L).toInt()
        findViewById<SeekBar>(R.id.timeout_seekbar).progress = seconds
        findViewById<TextView>(R.id.timeout_readout).text =
            resources.getQuantityString(R.plurals.auto_collapse_readout, seconds, seconds)

        findViewById<Switch>(R.id.haptics_switch).isChecked = Prefs.hapticsEnabled(this)
    }

    /**
     * Service status (FR-10): BOTH signals, OR-combined (ruling) — [VButtonService.instance]
     * alone misses a just-killed process whose enabled entry remains, and the secure
     * setting alone misses a binding race where the system enabled the service but
     * the process is not connected yet. The secure value is a colon-separated list of
     * flattened component names, matched exactly (no substring: a similarly named
     * package must not count).
     */
    private fun isServiceActive(): Boolean {
        if (VButtonService.instance != null) return true
        val wanted = ComponentName(this, VButtonService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it == wanted }
    }
}
