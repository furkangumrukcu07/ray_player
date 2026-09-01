package com.ray.iptv.di

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.ray.iptv.data.local.MIGRATION_2_3
import com.ray.iptv.data.local.MIGRATION_3_4
import com.ray.iptv.data.local.MIGRATION_4_5
import com.ray.iptv.data.local.MIGRATION_5_6
import com.ray.iptv.data.local.MIGRATION_6_7
import com.ray.iptv.data.local.RayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import com.ray.iptv.net.SslBypass
import com.ray.iptv.net.ImageCacheConfig
import com.ray.iptv.player.AndroidPlaybackSocHints
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }


    @Provides
    @Singleton
    fun okHttp(): OkHttpClient {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                if (!SslBypass.enabled) {
                    val d = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm())
                    d.init(null as java.security.KeyStore?)
                    (d.trustManagers.first { it is X509TrustManager } as X509TrustManager)
                        .checkServerTrusted(chain, authType)
                }
            }
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        }
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), SecureRandom()) }
        return OkHttpClient.Builder()
            .connectionPool(okhttp3.ConnectionPool(16, 5, TimeUnit.MINUTES))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .sslSocketFactory(ssl.socketFactory, trust)
            .hostnameVerifier { host, session ->
                SslBypass.enabled || javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session)
            }
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", com.ray.iptv.net.PlaybackIdentity.userAgent)
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): RayDatabase =
        Room.databaseBuilder(context, RayDatabase::class.java, "ray.db")
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun imageLoader(@ApplicationContext context: Context, okHttp: OkHttpClient): ImageLoader {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memClass = am.memoryClass
        val tv = AndroidPlaybackSocHints.isAndroidTvOrTvBox(context)
        val socHints = AndroidPlaybackSocHints.get(context)
        val lowRam = tv || memClass < 192 || socHints.oneGiBRamClass || socHints.playbackChallengedTv
        val memPercent = when {
            lowRam -> 0.06
            memClass < 256 -> 0.12
            else -> 0.20
        }
        val diskMb = when {
            lowRam -> 48
            else -> (if (ImageCacheConfig.maxMb <= 0) 128 else ImageCacheConfig.maxMb.coerceIn(50, 512))
        }
        return ImageLoader.Builder(context)
            .okHttpClient(
                okHttp.newBuilder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .writeTimeout(12, TimeUnit.SECONDS)
                    .build()
            )
            .crossfade(false)
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            .memoryCache { MemoryCache.Builder(context).maxSizePercent(memPercent).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("images"))
                    .maxSizeBytes(diskMb * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
