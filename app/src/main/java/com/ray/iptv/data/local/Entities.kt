package com.ray.iptv.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pinHash: String?,
    val isKids: Boolean,
    val avatarHue: Float,
    val createdAt: Long
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val extra: String,
    val createdAt: Long,
    val enabled: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "categories",
    indices = [Index("sourceId"), Index("kind")]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val remoteId: String,
    val kind: String,
    val name: String,
    val sortOrder: Int,
    val pinned: Boolean = false,
    val hidden: Boolean = false,
    val locked: Boolean = false
)

@Entity(
    tableName = "channels",
    indices = [Index("sourceId"), Index("categoryId"), Index("number"), Index("epgId")]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val remoteId: String,
    val name: String,
    val number: Int,
    val logo: String,
    val categoryId: String,
    val categoryName: String,
    val epgId: String,
    val hasArchive: Boolean,
    val archiveDays: Int,
    val streamUrl: String,
    val userAgent: String,
    val referer: String,
    val hidden: Boolean = false,
    val layoutSort: Int = -1
)

@Entity(
    tableName = "vod",
    indices = [Index("sourceId"), Index("categoryId"), Index("kind")]
)
data class VodEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val remoteId: String,
    val kind: String,
    val name: String,
    val poster: String,
    val plot: String,
    val year: String,
    val rating: String,
    val genre: String,
    val categoryId: String,
    val categoryName: String,
    val streamUrl: String,
    val extension: String,
    val addedUnix: Long = 0
)

@Entity(tableName = "episodes", indices = [Index("seriesId")])
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val sourceId: String,
    val remoteId: String,
    val season: Int,
    val episode: Int,
    val name: String,
    val plot: String,
    val still: String,
    val streamUrl: String,
    val extension: String
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["profileId", "mediaId"]
)
data class FavoriteEntity(
    val profileId: String,
    val mediaId: String,
    val kind: String,
    val createdAt: Long
)

@Entity(
    tableName = "progress",
    primaryKeys = ["profileId", "mediaId"]
)
data class ProgressEntity(
    val profileId: String,
    val mediaId: String,
    val kind: String,
    val title: String,
    val poster: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

@Entity(tableName = "epg", indices = [Index("channelId"), Index("startMs")])
data class EpgEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val epgId: String,
    val title: String,
    val plot: String,
    val startMs: Long,
    val endMs: Long,
    val hasCatchup: Boolean
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val mediaId: String,
    val title: String,
    val poster: String,
    val url: String,
    val path: String,
    val bytes: Long,
    val total: Long,
    val status: String,
    val createdAt: Long
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "channelId"])
data class GroupMemberEntity(
    val groupId: String,
    val channelId: String
)

@Entity(tableName = "epg_sources")
data class EpgSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Entity(tableName = "epg_match", primaryKeys = ["channelId"])
data class EpgMatchEntity(
    val channelId: String,
    val epgId: String
)

/** Mina `global_epg_channel` — EPGShare01 yedek, birincil `epg` tablosuna yazılmaz. */
@Entity(
    tableName = "global_epg_channel",
    primaryKeys = ["countryCode", "xmlChannelId"],
    indices = [Index("displayName")]
)
data class GlobalEpgChannelEntity(
    val countryCode: String,
    val xmlChannelId: String,
    val displayName: String,
    val logoUrl: String,
    val sourceFile: String
)

@Entity(
    tableName = "global_epg_programme",
    primaryKeys = ["countryCode", "xmlChannelId", "startMs"],
    indices = [Index("xmlChannelId", "startMs")]
)
data class GlobalEpgProgrammeEntity(
    val countryCode: String,
    val xmlChannelId: String,
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val description: String,
    val sourceFile: String
)
