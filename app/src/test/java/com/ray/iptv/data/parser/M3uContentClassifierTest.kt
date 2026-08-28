package com.ray.iptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uContentClassifierTest {

    @Test
    fun classify_xtreamPathTokens() {
        assertEquals(M3uContentKind.SERIES, M3uContentClassifier.classify("Breaking Bad S01E01", "http://server/series/user/pass/123.mp4", "Diziler"))
        assertEquals(M3uContentKind.MOVIE, M3uContentClassifier.classify("Inception", "http://server/movie/user/pass/456.mp4", "Filmler"))
        assertEquals(M3uContentKind.LIVE, M3uContentClassifier.classify("TRT 1", "http://server/live/user/pass/789.ts", "Ulusal"))
    }

    @Test
    fun classify_hlsMoviesAndSeriesInFlatM3u() {
        // HLS movie stream with group title
        assertEquals(M3uContentKind.MOVIE, M3uContentClassifier.classify("Gladiator (2000)", "http://cdn.com/stream/gladiator/playlist.m3u8", "TURK SINEMA"))
        // HLS series stream
        assertEquals(M3uContentKind.SERIES, M3uContentClassifier.classify("Kurtlar Vadisi 15. Bolum", "http://cdn.com/kv_15.m3u8", "Genel"))
        // Season episode regex
        assertEquals(M3uContentKind.SERIES, M3uContentClassifier.classify("Dark S02E05", "http://cdn.com/dark.ts", "Genel"))
        // 1x02 episode format
        assertEquals(M3uContentKind.SERIES, M3uContentClassifier.classify("Lost 2x14", "http://cdn.com/lost.m3u8", "Genel"))
        // 4-digit year group
        assertEquals(M3uContentKind.MOVIE, M3uContentClassifier.classify("Dune: Part Two", "http://cdn.com/dune.m3u8", "2024"))
        // IMDb id in URL
        assertEquals(M3uContentKind.MOVIE, M3uContentClassifier.classify("Top Gun", "https://vidmody.com/vs/tt0092099/index.m3u8", "Uncategorised"))
    }
}
