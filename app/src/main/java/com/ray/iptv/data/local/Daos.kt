package com.ray.iptv.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Immutable
data class ChannelLayoutSnap(
    val id: String,
    val hidden: Boolean,
    val layoutSort: Int
)

@Immutable
data class GroupCount(
    val categoryId: String,
    val total: Int
)

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sources ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE sources ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE sources SET sortOrder = rowid")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE channels ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE channels ADD COLUMN layoutSort INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE vod ADD COLUMN addedUnix INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_epg_channelId_startMs_endMs ON epg(channelId, startMs, endMs)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_epg_channelId_endMs ON epg(channelId, endMs)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_epg_endMs ON epg(endMs)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_sourceId_hidden_categoryId ON channels(sourceId, hidden, categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_hidden_categoryId ON channels(hidden, categoryId)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS global_epg_channel (
              countryCode TEXT NOT NULL,
              xmlChannelId TEXT NOT NULL,
              displayName TEXT NOT NULL,
              logoUrl TEXT NOT NULL,
              sourceFile TEXT NOT NULL,
              PRIMARY KEY (countryCode, xmlChannelId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS global_epg_programme (
              countryCode TEXT NOT NULL,
              xmlChannelId TEXT NOT NULL,
              startMs INTEGER NOT NULL,
              endMs INTEGER NOT NULL,
              title TEXT NOT NULL,
              description TEXT NOT NULL,
              sourceFile TEXT NOT NULL,
              PRIMARY KEY (countryCode, xmlChannelId, startMs)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_global_epg_channel_displayName ON global_epg_channel (displayName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_global_epg_programme_xmlChannelId_startMs ON global_epg_programme (xmlChannelId, startMs)")
    }
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt")
    fun observe(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt")
    suspend fun all(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun byId(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY sortOrder, createdAt")
    fun observe(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY sortOrder, createdAt")
    suspend fun all(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun byId(id: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE sources SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE sourceId = :sourceId AND kind = :kind ORDER BY pinned DESC, sortOrder, name")
    fun observe(sourceId: String, kind: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE sourceId = :sourceId AND kind = :kind ORDER BY pinned DESC, sortOrder, name")
    suspend fun list(sourceId: String, kind: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE sourceId = :sourceId")
    suspend fun listBySource(sourceId: String): List<CategoryEntity>

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CategoryEntity>)

    @Query("UPDATE categories SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE categories SET hidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query("UPDATE categories SET locked = :locked WHERE id = :id")
    suspend fun setLocked(id: String, locked: Boolean)

    @Query("DELETE FROM categories WHERE sourceId = :sourceId")
    suspend fun clearSource(sourceId: String)

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY pinned DESC, sortOrder, name")
    fun observeKind(kind: String): Flow<List<CategoryEntity>>
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND (:categoryId = '' OR categoryId = :categoryId) AND hidden = 0 ORDER BY CASE WHEN layoutSort >= 0 THEN 0 ELSE 1 END, layoutSort, number")
    fun observe(sourceId: String, categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE (:sourceId = '' OR sourceId = :sourceId) AND (:categoryId = '' OR categoryId = :categoryId) ORDER BY CASE WHEN layoutSort >= 0 THEN 0 ELSE 1 END, layoutSort, number")
    suspend fun list(sourceId: String, categoryId: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId")
    suspend fun listBySource(sourceId: String): List<ChannelEntity>

    @Query("UPDATE channels SET hidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query("UPDATE channels SET layoutSort = :layoutSort WHERE id = :id")
    suspend fun setLayoutSort(id: String, layoutSort: Int)

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun byId(id: String): ChannelEntity?

    /** Do not collect this in UI — it emits the entire channel table. Prefer observePage / observePageAll. */
    @Query("SELECT * FROM channels WHERE hidden = 0 ORDER BY CASE WHEN layoutSort >= 0 THEN 0 ELSE 1 END, layoutSort, number")
    fun observeAll(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY number")
    suspend fun all(): List<ChannelEntity>

    @Query("UPDATE channels SET epgId = :epgId WHERE id = :id")
    suspend fun setEpgId(id: String, epgId: String)

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND number = :number AND hidden = 0 LIMIT 1")
    suspend fun byNumber(sourceId: String, number: Int): ChannelEntity?

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND hidden = 0 AND name LIKE '%' || :q || '%' ORDER BY number LIMIT 80")
    suspend fun search(sourceId: String, q: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE hidden = 0 AND name LIKE '%' || :q || '%' ORDER BY number LIMIT 80")
    suspend fun searchAll(q: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE number = :number AND hidden = 0 LIMIT 1")
    suspend fun byNumberAny(number: Int): ChannelEntity?

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND hidden = 0 ORDER BY RANDOM() LIMIT 24")
    suspend fun mixed(sourceId: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE hidden = 0 ORDER BY RANDOM() LIMIT 24")
    suspend fun mixedAll(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun clearSource(sourceId: String)

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    suspend fun count(sourceId: String): Int

    @Query("SELECT * FROM channels WHERE id IN (:ids) ORDER BY number")
    suspend fun byIds(ids: List<String>): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id IN (:keys) OR remoteId IN (:keys) OR epgId IN (:keys)")
    suspend fun byAnyKeys(keys: List<String>): List<ChannelEntity>

    @Query("SELECT id, hidden, layoutSort FROM channels WHERE sourceId = :sourceId")
    suspend fun layoutBySource(sourceId: String): List<ChannelLayoutSnap>

    @Query(
        """
        SELECT * FROM channels WHERE sourceId = :sourceId AND hidden = 0
        AND (:categoryId = '' OR categoryId = :categoryId)
        ORDER BY CASE WHEN layoutSort >= 0 THEN 0 ELSE 1 END, layoutSort, number
        LIMIT :limit
        """
    )
    fun observePage(sourceId: String, categoryId: String, limit: Int): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT * FROM channels WHERE hidden = 0
        AND (:categoryId = '' OR categoryId = :categoryId)
        ORDER BY CASE WHEN layoutSort >= 0 THEN 0 ELSE 1 END, layoutSort, number
        LIMIT :limit
        """
    )
    fun observePageAll(categoryId: String, limit: Int): Flow<List<ChannelEntity>>

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId AND hidden = 0")
    fun observeVisibleCount(sourceId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE hidden = 0")
    fun observeVisibleCountAll(): Flow<Int>

    @Query(
        """
        SELECT categoryId AS categoryId, COUNT(*) AS total FROM channels
        WHERE sourceId = :sourceId AND hidden = 0 GROUP BY categoryId
        """
    )
    fun observeCounts(sourceId: String): Flow<List<GroupCount>>

    @Query("SELECT categoryId AS categoryId, COUNT(*) AS total FROM channels WHERE hidden = 0 GROUP BY categoryId")
    fun observeCountsAll(): Flow<List<GroupCount>>
}

@Dao
interface VodDao {
    @Query("SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind AND (:categoryId = '' OR categoryId = :categoryId) ORDER BY name")
    fun observe(sourceId: String, kind: String, categoryId: String): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind AND (:categoryId = '' OR categoryId = :categoryId) ORDER BY name LIMIT :limit")
    suspend fun list(sourceId: String, kind: String, categoryId: String, limit: Int = 400): List<VodEntity>

    @Query("SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind AND categoryId = :categoryId ORDER BY name")
    suspend fun listByCategory(sourceId: String, kind: String, categoryId: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE kind = :kind AND categoryId = :categoryId ORDER BY name")
    suspend fun listByCategoryAll(kind: String, categoryId: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE id = :id")
    suspend fun byId(id: String): VodEntity?

    @Query("SELECT * FROM vod WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind
        ORDER BY addedUnix DESC, CAST(remoteId AS INTEGER) DESC, rowid DESC
        LIMIT 50
        """
    )
    suspend fun lastAdded(sourceId: String, kind: String): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE kind = :kind
        ORDER BY addedUnix DESC, CAST(remoteId AS INTEGER) DESC, rowid DESC
        LIMIT 50
        """
    )
    suspend fun lastAddedAll(kind: String): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind
        ORDER BY CAST(REPLACE(IFNULL(rating, ''), ',', '.') AS REAL) DESC, name
        LIMIT 50
        """
    )
    suspend fun topRated(sourceId: String, kind: String): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE kind = :kind
        ORDER BY CAST(REPLACE(IFNULL(rating, ''), ',', '.') AS REAL) DESC, name
        LIMIT 50
        """
    )
    suspend fun topRatedAll(kind: String): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind
        AND CAST(REPLACE(IFNULL(rating, ''), ',', '.') AS REAL) >= 7.0
        ORDER BY CAST(REPLACE(IFNULL(rating, ''), ',', '.') AS REAL) DESC, name
        LIMIT 50
        """
    )
    suspend fun trendRated(sourceId: String, kind: String): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE kind = :kind
        AND CAST(REPLACE(IFNULL(rating, ''), ',', '.') AS REAL) >= 7.0
        ORDER BY CAST(REPLACE(IFNULL(rating, ''), ',', '.') AS REAL) DESC, name
        LIMIT 50
        """
    )
    suspend fun trendRatedAll(kind: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind ORDER BY RANDOM() LIMIT 40")
    suspend fun mixed(sourceId: String, kind: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE kind = :kind ORDER BY RANDOM() LIMIT 40")
    suspend fun mixedAll(kind: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE sourceId = :sourceId AND name LIKE '%' || :q || '%' ORDER BY name LIMIT 80")
    suspend fun search(sourceId: String, q: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE name LIKE '%' || :q || '%' ORDER BY name LIMIT 80")
    suspend fun searchAll(q: String): List<VodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VodEntity>)

    @Query("DELETE FROM vod WHERE sourceId = :sourceId")
    suspend fun clearSource(sourceId: String)

    @Query("SELECT COUNT(*) FROM vod WHERE sourceId = :sourceId")
    suspend fun count(sourceId: String): Int

    @Query("SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind ORDER BY name")
    suspend fun listKind(sourceId: String, kind: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE kind = :kind ORDER BY name")
    suspend fun listAllKind(kind: String): List<VodEntity>

    @Query(
        """
        SELECT * FROM vod WHERE sourceId = :sourceId AND kind = :kind
        AND (:categoryId = '' OR categoryId = :categoryId)
        ORDER BY name LIMIT :limit
        """
    )
    fun observePage(sourceId: String, kind: String, categoryId: String, limit: Int): Flow<List<VodEntity>>

    @Query(
        """
        SELECT * FROM vod WHERE kind = :kind
        AND (:categoryId = '' OR categoryId = :categoryId)
        ORDER BY name LIMIT :limit
        """
    )
    fun observeKindPage(kind: String, categoryId: String, limit: Int): Flow<List<VodEntity>>

    @Query("SELECT COUNT(*) FROM vod WHERE sourceId = :sourceId AND kind = :kind")
    fun observeKindCount(sourceId: String, kind: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM vod WHERE kind = :kind")
    fun observeKindCountAll(kind: String): Flow<Int>

    @Query(
        """
        SELECT categoryId AS categoryId, COUNT(*) AS total FROM vod
        WHERE sourceId = :sourceId AND kind = :kind GROUP BY categoryId
        """
    )
    fun observeCounts(sourceId: String, kind: String): Flow<List<GroupCount>>

    @Query("SELECT categoryId AS categoryId, COUNT(*) AS total FROM vod WHERE kind = :kind GROUP BY categoryId")
    fun observeKindCounts(kind: String): Flow<List<GroupCount>>
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season, episode")
    fun observe(seriesId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season, episode")
    suspend fun list(seriesId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun byId(id: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun clearSeries(seriesId: String)

    @Query("DELETE FROM episodes WHERE sourceId = :sourceId")
    suspend fun clearSource(sourceId: String)

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season, episode")
    suspend fun nextAfter(seriesId: String): List<EpisodeEntity>
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    suspend fun all(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observe(profileId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND mediaId = :mediaId)")
    fun isFavorite(profileId: String, mediaId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND mediaId = :mediaId)")
    suspend fun exists(profileId: String, mediaId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun delete(profileId: String, mediaId: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress")
    suspend fun all(): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE profileId = :profileId ORDER BY updatedAt DESC LIMIT 50")
    fun continueWatching(profileId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE profileId = :profileId")
    suspend fun listByProfile(profileId: String): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE profileId = :profileId AND kind = 'LIVE' ORDER BY updatedAt DESC LIMIT 24")
    fun recentLive(profileId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun byId(profileId: String, mediaId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgressEntity)

    @Query("DELETE FROM progress WHERE profileId = :profileId AND mediaId = :mediaId")
    suspend fun delete(profileId: String, mediaId: String)

    @Query("DELETE FROM progress WHERE profileId = :profileId")
    suspend fun clear(profileId: String)

    @Query("DELETE FROM progress")
    suspend fun deleteAll()
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg WHERE channelId = :channelId AND endMs >= :now ORDER BY startMs LIMIT 24")
    fun upcoming(channelId: String, now: Long): Flow<List<EpgEntity>>

    @Query("SELECT * FROM epg WHERE channelId = :channelId AND startMs < :to AND endMs > :from ORDER BY startMs LIMIT 200")
    suspend fun window(channelId: String, from: Long, to: Long): List<EpgEntity>

    @Query("SELECT * FROM epg WHERE channelId = :channelId AND startMs <= :now AND endMs > :now LIMIT 1")
    suspend fun now(channelId: String, now: Long): EpgEntity?

    @Query("SELECT * FROM epg WHERE channelId IN (:ids) AND startMs <= :now AND endMs > :now")
    suspend fun nowMany(ids: List<String>, now: Long): List<EpgEntity>

    @Query("SELECT * FROM epg WHERE endMs > :now AND startMs < :until ORDER BY startMs LIMIT :limit")
    suspend fun upcomingInRange(now: Long, until: Long, limit: Int): List<EpgEntity>

    @Query("SELECT * FROM epg WHERE endMs BETWEEN :from AND :now ORDER BY endMs DESC LIMIT 250")
    suspend fun recentlyEnded(from: Long, now: Long): List<EpgEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EpgEntity>)

    @Query("DELETE FROM epg WHERE endMs < :before")
    suspend fun prune(before: Long)

    @Query("DELETE FROM epg")
    suspend fun clear()

    @Query("SELECT COUNT(DISTINCT channelId) FROM epg")
    suspend fun distinctChannelCount(): Int

    @Query("SELECT COUNT(*) FROM epg")
    suspend fun programmeCount(): Int

    @Query("SELECT DISTINCT epgId FROM epg WHERE epgId != ''")
    suspend fun allEpgIds(): List<String>

    @Query("SELECT DISTINCT channelId FROM epg WHERE channelId != ''")
    suspend fun allChannelIds(): List<String>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observe(profileId: String): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun byId(id: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE profileId = :profileId")
    fun observe(profileId: String): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMember(item: GroupMemberEntity)

    @Query("SELECT channelId FROM group_members WHERE groupId = :groupId")
    suspend fun members(groupId: String): List<String>

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM groups")
    suspend fun all(): List<GroupEntity>

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND channelId = :channelId")
    suspend fun removeMember(groupId: String, channelId: String)
}

@Dao
interface EpgSourceDao {
    @Query("SELECT * FROM epg_sources ORDER BY name")
    fun observe(): Flow<List<EpgSourceEntity>>

    @Query("SELECT * FROM epg_sources WHERE enabled = 1")
    suspend fun enabled(): List<EpgSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EpgSourceEntity)

    @Query("DELETE FROM epg_sources WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM epg_sources")
    suspend fun all(): List<EpgSourceEntity>
}

@Dao
interface EpgMatchDao {
    @Query("SELECT * FROM epg_match")
    suspend fun all(): List<EpgMatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EpgMatchEntity)
}

@Dao
interface GlobalEpgChannelDao {
    @Query("SELECT * FROM global_epg_channel")
    suspend fun all(): List<GlobalEpgChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GlobalEpgChannelEntity>)

    @Query("DELETE FROM global_epg_channel WHERE countryCode = :code")
    suspend fun deleteCountry(code: String)

    @Query("SELECT COUNT(*) FROM global_epg_channel")
    suspend fun count(): Int
}

@Dao
interface GlobalEpgProgrammeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GlobalEpgProgrammeEntity>)

    @Query("DELETE FROM global_epg_programme WHERE countryCode = :code")
    suspend fun deleteCountry(code: String)

    @Query("SELECT COUNT(*) FROM global_epg_programme")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT xmlChannelId) FROM global_epg_programme")
    suspend fun distinctChannelCount(): Int

    @Query(
        "SELECT * FROM global_epg_programme WHERE xmlChannelId = :xmlId AND startMs <= :now AND endMs > :now ORDER BY startMs LIMIT 1"
    )
    suspend fun now(xmlId: String, now: Long): GlobalEpgProgrammeEntity?

    @Query(
        "SELECT * FROM global_epg_programme WHERE xmlChannelId IN (:xmlIds) AND startMs <= :now AND endMs > :now"
    )
    suspend fun nowMany(xmlIds: List<String>, now: Long): List<GlobalEpgProgrammeEntity>

    @Query(
        "SELECT * FROM global_epg_programme WHERE xmlChannelId = :xmlId AND startMs < :to AND endMs > :from ORDER BY startMs LIMIT 200"
    )
    suspend fun window(xmlId: String, from: Long, to: Long): List<GlobalEpgProgrammeEntity>
}

@androidx.room.Database(
    entities = [
        ProfileEntity::class,
        SourceEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        VodEntity::class,
        EpisodeEntity::class,
        FavoriteEntity::class,
        ProgressEntity::class,
        EpgEntity::class,
        DownloadEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        EpgSourceEntity::class,
        EpgMatchEntity::class,
        GlobalEpgChannelEntity::class,
        GlobalEpgProgrammeEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class RayDatabase : androidx.room.RoomDatabase() {
    abstract fun profiles(): ProfileDao
    abstract fun sources(): SourceDao
    abstract fun categories(): CategoryDao
    abstract fun channels(): ChannelDao
    abstract fun vod(): VodDao
    abstract fun episodes(): EpisodeDao
    abstract fun favorites(): FavoriteDao
    abstract fun progress(): ProgressDao
    abstract fun epg(): EpgDao
    abstract fun downloads(): DownloadDao
    abstract fun groups(): GroupDao
    abstract fun epgSources(): EpgSourceDao
    abstract fun epgMatch(): EpgMatchDao
    abstract fun globalEpgChannels(): GlobalEpgChannelDao
    abstract fun globalEpgProgrammes(): GlobalEpgProgrammeDao

    @Transaction
    suspend fun replaceCatalog(
        sourceId: String,
        categories: List<CategoryEntity>,
        channels: List<ChannelEntity>,
        vod: List<VodEntity>
    ) {
        this.categories().clearSource(sourceId)
        this.channels().clearSource(sourceId)
        this.vod().clearSource(sourceId)
        this.categories().upsertAll(categories)
        channels.chunked(UPSERT_CHUNK).forEach { this.channels().upsertAll(it) }
        vod.chunked(UPSERT_CHUNK).forEach { this.vod().upsertAll(it) }
    }

    companion object {
        private const val UPSERT_CHUNK = 400
    }
}
