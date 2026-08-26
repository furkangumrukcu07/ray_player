package com.ray.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleLanguagesTest {
    @Test
    fun mapsTurkishAliasesToTr() {
        assertEquals("tr", SubtitleLanguages.canonical("Turkish"))
        assertEquals("tr", SubtitleLanguages.canonical("tr"))
        assertEquals("tr", SubtitleLanguages.canonical("tur"))
        assertEquals("tr", SubtitleLanguages.canonical("Türkçe"))
        assertEquals("tr", SubtitleLanguages.canonical("Turkish [Forced]"))
    }

    @Test
    fun englishDoesNotMatchFrench() {
        assertFalse(SubtitleLanguages.matches("fr", "French", "en"))
        assertFalse(SubtitleLanguages.matches("en", "English", "fr"))
        assertTrue(SubtitleLanguages.matches("en", "English", "eng"))
        assertTrue(SubtitleLanguages.matches("eng", "English", "en"))
    }

    @Test
    fun rememberedTurkishSelectsLangOrLabel() {
        assertTrue(SubtitleLanguages.matches("tr", "Track 2", "turkish"))
        assertTrue(SubtitleLanguages.matches("", "Türkçe", "tr"))
        assertTrue(SubtitleLanguages.matches("tur", "Forced", "tr"))
        assertFalse(SubtitleLanguages.matches("de", "German", "tr"))
    }

    @Test
    fun tokenPrefersIsoLanguage() {
        assertEquals("tr", SubtitleLanguages.tokenOf("tur", "Commentary"))
        assertEquals("en", SubtitleLanguages.tokenOf("", "English"))
    }
}
