package com.ray.iptv.ui

import com.ray.iptv.data.local.ChannelEntity
import com.ray.iptv.data.local.EpgEntity

enum class Dest {
    LIVE, MOVIES, SERIES, CONTINUE, PLAYLISTS, CATCHUP, SETTINGS, PLAYER, WRAPPED, EPG_MIX, CHAT, ADMIN
}

data class NextUpPrompt(
    val title: String,
    val series: Boolean
)

data class ShowcaseEpgChip(
    val channelId: String,
    val channelName: String,
    val logo: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val live: Boolean
)

enum class EpgMixKind { ALL, REPLAY, SPORT, DOCUMENTARY, FILM, SERIES, NEWS }

data class ShowcaseEpgMixItem(
    val channel: ChannelEntity,
    val programme: EpgEntity,
    val kind: EpgMixKind
)

enum class LiveBrowsePhase {
    CATEGORIES, CONTENT
}

enum class Overlay {
    NONE, GUIDE, SEARCH
}

data class Playback(
    val url: String,
    val title: String,
    val subtitle: String = "",
    val poster: String = "",
    val mediaId: String,
    val kind: String,
    val startMs: Long = 0L,
    val userAgent: String = "",
    val referer: String = "",
    val live: Boolean = false,
    val channelNumber: Int = 0,
    val sourceId: String = "",
    val hasArchive: Boolean = false,
    val remoteId: String = "",
    val seriesId: String = "",
    val externalSubtitleUri: String = ""
)
