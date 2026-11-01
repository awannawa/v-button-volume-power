package com.awannawa.vbutton.overlay

import com.awannawa.vbutton.Prefs
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for the pure T08 preset mapping (AGENTS.md: pure logic covered by JVM
 * unit tests). The stored Prefs string must land on the locked preset data from
 * 01-ui-spec.md section 3, and anything unreadable must fall back to the default
 * preset (M) rather than fail or render nothing.
 */
class SizePresetFromPrefTest {

    @Test
    fun `each ruled preset string maps to its locked preset`() {
        assertEquals(Tokens.PRESET_S, sizePresetFromPref(Prefs.SIZE_PRESET_SMALL))
        assertEquals(Tokens.PRESET_M, sizePresetFromPref(Prefs.SIZE_PRESET_MEDIUM))
        assertEquals(Tokens.PRESET_L, sizePresetFromPref(Prefs.SIZE_PRESET_LARGE))
    }

    @Test
    fun `anything unreadable falls back to the default preset`() {
        assertEquals(Tokens.PRESET_M, sizePresetFromPref("X"))
        assertEquals(Tokens.PRESET_M, sizePresetFromPref(""))
        assertEquals(Tokens.PRESET_M, sizePresetFromPref("m")) // case-sensitive keys
    }

    @Test
    fun `the fallback is the compiled default preset`() {
        // The mapping must fall back to Tokens.activePreset (M), not invent a size.
        assertEquals(Tokens.activePreset, sizePresetFromPref(Prefs.SIZE_PRESET_DEFAULT))
    }
}
