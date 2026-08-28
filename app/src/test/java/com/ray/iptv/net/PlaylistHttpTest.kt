package com.ray.iptv.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistHttpTest {

    @Test
    fun normalizeUrl_handlesSchemesAndFormatting() {
        assertEquals("https://www.tinyurl.com/playlist123", PlaylistHttp.normalizeUrl("www.tinyurl.com/playlist123"))
        assertEquals("https://tinyurl.com/playlist123", PlaylistHttp.normalizeUrl("tinyurl.com/playlist123"))
        assertEquals("http://tinyurl.com/playlist123", PlaylistHttp.normalizeUrl("http://tinyurl.com/playlist123"))
        assertEquals("https://tinyurl.com/playlist123", PlaylistHttp.normalizeUrl("https://tinyurl.com/playlist123"))
        assertEquals("https://tinyurl.com/playlist123", PlaylistHttp.normalizeUrl("  \"https://tinyurl.com/playlist123\" \n"))
        assertEquals("https://bit.ly/3xyz", PlaylistHttp.normalizeUrl("bit.ly/3xyz"))
        assertEquals("https://is.gd/abc", PlaylistHttp.normalizeUrl("is.gd/abc"))
        assertEquals("http://server.com:8080/get.php?username=u&password=p", PlaylistHttp.normalizeUrl("server.com:8080/get.php?username=u&password=p"))
        assertEquals("content://com.android.providers/123", PlaylistHttp.normalizeUrl("content://com.android.providers/123"))
    }

    @Test
    fun isShortUrl_identifiesShortlinks() {
        assertTrue(PlaylistHttp.isShortUrl("https://tinyurl.com/abc1234"))
        assertTrue(PlaylistHttp.isShortUrl("www.tinyurl.com/abc1234"))
        assertTrue(PlaylistHttp.isShortUrl("http://preview.tinyurl.com/abc1234"))
        assertTrue(PlaylistHttp.isShortUrl("https://bit.ly/myplaylist"))
        assertTrue(PlaylistHttp.isShortUrl("https://is.gd/xyz"))
        assertTrue(PlaylistHttp.isShortUrl("https://t.ly/1234"))
        assertTrue(PlaylistHttp.isShortUrl("https://shorturl.at/abc"))
        assertFalse(PlaylistHttp.isShortUrl("http://regular-iptv-server.com:8080/get.php?username=1&password=2&type=m3u_plus"))
    }
}
