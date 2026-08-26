package com.ray.iptv.player

import com.ray.iptv.data.repo.StreamFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamHintsTest {
    @Test
    fun autoGetPhpWithoutOutputIsTsNotProgressive() {
        val url = "http://panel.example/get.php?username=u&password=p&stream_id=12"
        assertEquals(StreamHints.Kind.TS, StreamHints.kind(url, StreamFormat.AUTO))
    }

    @Test
    fun autoLivePathWithoutExtensionIsTs() {
        val url = "http://panel.example/live/user/pass/12"
        assertEquals(StreamHints.Kind.TS, StreamHints.kind(url, StreamFormat.AUTO))
    }

    @Test
    fun autoM3u8StaysHls() {
        val url = "http://panel.example/live/user/pass/12.m3u8"
        assertEquals(StreamHints.Kind.HLS, StreamHints.kind(url, StreamFormat.AUTO))
    }

    @Test
    fun explicitHlsWinsOverTsPath() {
        val url = "http://panel.example/live/user/pass/12.ts"
        assertEquals(StreamHints.Kind.HLS, StreamHints.kind(url, StreamFormat.HLS))
    }
}
