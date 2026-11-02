package com.awannawa.vbutton

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.awannawa.vbutton.overlay.ScreenSide

/**
 * Settings screen and launcher (T08, FR-10 to FR-12). Plain platform widgets in a
 * vertical list (DESIGN.md section 3); all copy is in strings.xml. Every change is
 * written to [Prefs] only — the service's Prefs listener forwards it to the overlay's
 * applyConfig, so nothing here touches the overlay or the service directly
 * (AGENTS.md integration points). The status read re-runs in onResume; there is no
 * polling or receiver (NFR-1).
 *
 * T09 adds the dead-service warning (PRD R2) and the Vivo keep-alive guide with two
 * buttons into the system battery and App info screens.
 */
class SettingsActivity : Activity() {

    private companion object {
        // Same log tag pattern as the service and the overlay (AGENTS.md error handling).
        const val LOG_TAG = "VButton"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate and any inflation: the DayNight theme is resource-
        // qualified (values-v29), so API 28 resolves the plain DeviceDefault fallback.
        setTheme(R.style.Theme_VButtonSettings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        wireListeners()
    }

    override fun onResume() {
        super.onResume()
        // A fresh read on every resume is the v1 status refresh (ruling): the user
        // returns from the accessibility settings and the state is re-read. The same
        // refresh updates the dead-service warning (T09).
        refreshUi()
    }

    private fun wireListeners() {
        // FR-10: opens the system screen where the service toggle lives.
        findViewById<Button>(R.id.enable_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // T09 (PRD R2): the battery-optimization listing screen — no permission needed
        // (unlike ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which is deliberately
        // not used: it would require REQUEST_IGNORE_BATTERY_OPTIMIZATIONS and the
        // manifest must stay at zero). Constant name note: the platform has no
        // ACTION_IGNORE_BATTERY_OPTIMIZATIONS; the listing screen is spelled
        // ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS (API 23+). Some builds ship no
        // handler for it; the fallback is App info, where the battery entry lives too.
        // Logged rather than silent (documented choice): logcat is the only diagnostic
        // channel on the target device and this fallback is a Funtouch-behavior signal.
        findViewById<Button>(R.id.vivo_battery_button).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                Log.w(LOG_TAG, "no activity for ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, falling back to App info", e)
                openAppInfo()
            }
        }

        // T09 (PRD R2): the app's own App info page — Autostart, battery, and the
        // restricted-settings path (R3) all live there. Core settings action, present
        // on every API level this app supports, so no fallback handling.
        findViewById<Button>(R.id.vivo_app_info_button).setOnClickListener {
            openAppInfo()
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

        // The screen side the overlay hugs (right default). Written live; the service's
        // Prefs listener pushes it through applyConfig, which re-anchors the window.
        findViewById<RadioGroup>(R.id.side_group).setOnCheckedChangeListener { _, checkedId ->
            val side = if (checkedId == R.id.side_left) ScreenSide.Left else ScreenSide.Right
            Prefs.setScreenSide(this, side)
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

    /** Opens this app's App info page (T09): package URI, no permission needed. */
    private fun openAppInfo() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    /** Re-reads every stored value into the views and the service status. */
    private fun refreshUi() {
        val active = isServiceActive()
        findViewById<TextView>(R.id.status_text)
            .setText(if (active) R.string.status_active else R.string.status_inactive)

        // T09 dead-service warning (PRD R2): shown when the service reads as inactive
        // although it was active before. The flag is never cleared (Prefs), so the
        // warning is a state statement that clears itself once the service is active
        // again; it re-reads here only — onResume, no polling (NFR-1).
        findViewById<TextView>(R.id.dead_service_warning).visibility =
            if (!active && Prefs.wasServiceActive(this)) View.VISIBLE else View.GONE

        findViewById<RadioGroup>(R.id.size_group).check(
            when (Prefs.sizePreset(this)) {
                Prefs.SIZE_PRESET_SMALL -> R.id.size_small
                Prefs.SIZE_PRESET_LARGE -> R.id.size_large
                else -> R.id.size_medium
            },
        )

        findViewById<RadioGroup>(R.id.side_group).check(
            if (Prefs.screenSide(this) == ScreenSide.Left) R.id.side_left else R.id.side_right,
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
