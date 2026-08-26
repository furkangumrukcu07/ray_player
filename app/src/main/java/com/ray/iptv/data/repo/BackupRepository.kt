package com.ray.iptv.data.repo

import android.content.Context
import android.net.Uri
import com.ray.iptv.data.local.EpgSourceEntity
import com.ray.iptv.data.local.FavoriteEntity
import com.ray.iptv.data.local.ProfileEntity
import com.ray.iptv.data.local.ProgressEntity
import com.ray.iptv.data.local.RayDatabase
import com.ray.iptv.data.local.SourceEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupFile(
    val version: Int = 2,
    val settings: BackupSettings = BackupSettings(),
    val profiles: List<BackupProfile> = emptyList(),
    val sources: List<BackupSource> = emptyList(),
    val epgSources: List<BackupEpg> = emptyList(),
    val favorites: List<BackupFavorite> = emptyList(),
    val progress: List<BackupProgress> = emptyList()
)

@Serializable
data class BackupSettings(
    val startup: String = "HOME",
    val glass: String = "DARK",
    val hideAdult: Boolean = true,
    val hideLocked: Boolean = true,
    val previewLive: Boolean = true,
    val autoplayNext: Boolean = true,
    val catchupTz: Boolean = true,
    val lang: String = "TR",
    val aspect: String = "STRETCH",
    val speed: String = "1.0",
    val combineM3u: Boolean = false
)

@Serializable
data class BackupProfile(
    val id: String,
    val name: String,
    val pinHash: String? = null,
    val isKids: Boolean = false,
    val avatarHue: Float = 200f
)

@Serializable
data class BackupSource(
    val id: String,
    val kind: String,
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val extra: String = ""
)

@Serializable
data class BackupEpg(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Serializable
data class BackupFavorite(
    val profileId: String,
    val mediaId: String,
    val kind: String,
    val createdAt: Long
)

@Serializable
data class BackupProgress(
    val profileId: String,
    val mediaId: String,
    val kind: String,
    val title: String,
    val poster: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: RayDatabase,
    private val settings: SettingsRepository,
    private val json: Json
) {
    suspend fun exportTo(uri: Uri) {
        val jsonStr = exportJson()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonStr.toByteArray())
        } ?: error("Cannot write backup")
    }

    suspend fun importFrom(uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Cannot read backup")
        importJson(text)
    }

    suspend fun exportJson(): String {
        val s = settings.settings.first()
        val file = BackupFile(
            version = 2,
            settings = BackupSettings(
                startup = s.startup.name,
                glass = s.glass.name,
                hideAdult = s.hideAdult,
                hideLocked = s.hideLocked,
                previewLive = s.previewLive,
                autoplayNext = s.autoplayNext,
                catchupTz = s.catchupTimezoneDevice,
                lang = s.lang.name,
                aspect = s.aspect.name,
                speed = s.speed.toString(),
                combineM3u = s.combineM3u
            ),
            profiles = db.profiles().all().map {
                BackupProfile(it.id, it.name, it.pinHash, it.isKids, it.avatarHue)
            },
            sources = db.sources().all().map {
                BackupSource(it.id, it.kind, it.name, it.baseUrl, it.username, it.password, it.extra)
            },
            epgSources = db.epgSources().all().map {
                BackupEpg(it.id, it.name, it.url, it.enabled)
            },
            favorites = db.favorites().all().map {
                BackupFavorite(it.profileId, it.mediaId, it.kind, it.createdAt)
            },
            progress = db.progress().all().map {
                BackupProgress(it.profileId, it.mediaId, it.kind, it.title, it.poster, it.positionMs, it.durationMs, it.updatedAt)
            }
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    suspend fun importJson(text: String) {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        file.profiles.forEach {
            db.profiles().upsert(
                ProfileEntity(it.id, it.name, it.pinHash, it.isKids, it.avatarHue, System.currentTimeMillis())
            )
        }
        file.sources.forEach {
            db.sources().upsert(
                SourceEntity(it.id, it.kind, it.name, it.baseUrl, it.username, it.password, it.extra, System.currentTimeMillis())
            )
        }
        file.epgSources.forEach {
            db.epgSources().upsert(EpgSourceEntity(it.id, it.name, it.url, it.enabled))
        }
        file.favorites.forEach {
            db.favorites().upsert(FavoriteEntity(it.profileId, it.mediaId, it.kind, it.createdAt))
        }
        file.progress.forEach {
            db.progress().upsert(
                ProgressEntity(it.profileId, it.mediaId, it.kind, it.title, it.poster, it.positionMs, it.durationMs, it.updatedAt)
            )
        }
        settings.applyBackup(file.settings)
        if (file.profiles.isNotEmpty()) settings.setProfile(file.profiles.first().id)
        if (file.sources.isNotEmpty()) settings.setSource(file.sources.first().id)
        settings.setOnboarded()
        settings.acceptDisclaimer()
    }
}
