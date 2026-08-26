package com.ray.iptv.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class RayRemoteConfig(
    val defaultVideoEngine: String = "",
    val reviewModeActive: Boolean = false,
    val dailyQuoteRaw: String = "",
    val updateMinVersionCode: Int = 0,
    val updateMessage: String = ""
)

@Singleton
class FirebaseService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isFirebaseReady = false

    private val _remoteConfig = MutableStateFlow(RayRemoteConfig())
    val remoteConfig: StateFlow<RayRemoteConfig> = _remoteConfig

    init {
        initFirebaseGuarded()
    }

    fun initFirebaseGuarded() {
        try {
            val app = FirebaseApp.initializeApp(context)
            if (app != null) {
                isFirebaseReady = true
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
                Log.d("RayFirebase", "Firebase initialized successfully for package: ${context.packageName}")
                
                fetchRemoteConfig()
                logAdminTelemetry()
            }
        } catch (e: Exception) {
            Log.w("RayFirebase", "Firebase guarded init warning: ${e.message}")
            isFirebaseReady = false
        }
    }

    fun fetchRemoteConfig() {
        if (!isFirebaseReady) return
        try {
            val rc = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            rc.setConfigSettingsAsync(settings)

            val defaults = mapOf(
                "default_video_engine" to "",
                "review_mode_active" to false,
                "gunun_sozu" to "",
                "update_min_version_code" to 0,
                "update_message" to ""
            )
            rc.setDefaultsAsync(defaults)

            rc.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val config = RayRemoteConfig(
                        defaultVideoEngine = rc.getString("default_video_engine").trim(),
                        reviewModeActive = rc.getBoolean("review_mode_active"),
                        dailyQuoteRaw = rc.getString("gunun_sozu").trim(),
                        updateMinVersionCode = rc.getLong("update_min_version_code").toInt(),
                        updateMessage = rc.getString("update_message").trim()
                    )
                    _remoteConfig.value = config
                    Log.d("RayFirebase", "RemoteConfig fetched & applied: $config")
                }
            }
        } catch (e: Exception) {
            Log.w("RayFirebase", "RemoteConfig fetch error: ${e.message}")
        }
    }

    fun reportError(t: Throwable, tag: String = "App") {
        if (isFirebaseReady) {
            try {
                FirebaseCrashlytics.getInstance().recordException(t)
            } catch (_: Exception) {}
        }
        scope.launch {
            try {
                if (isFirebaseReady) {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val crashData = mapOf(
                        "date" to today,
                        "timestamp" to System.currentTimeMillis(),
                        "tag" to tag,
                        "message" to (t.message ?: t.toString()),
                        "stacktrace" to Log.getStackTraceString(t),
                        "package" to context.packageName
                    )
                    FirebaseFirestore.getInstance().collection("app_crashes").add(crashData)
                }
            } catch (_: Exception) {}
        }
    }

    fun logAdminTelemetry() {
        if (!isFirebaseReady) return
        scope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val docRef = FirebaseFirestore.getInstance().collection("admin_stats").document(today)
                
                FirebaseFirestore.getInstance().runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    if (!snapshot.exists()) {
                        val initial = mapOf(
                            "date" to today,
                            "active_users" to 1L,
                            "app_launches" to 1L,
                            "last_active" to System.currentTimeMillis()
                        )
                        transaction.set(docRef, initial)
                    } else {
                        val launches = snapshot.getLong("app_launches") ?: 0L
                        transaction.update(
                            docRef,
                            "app_launches", launches + 1L,
                            "last_active", System.currentTimeMillis()
                        )
                    }
                }.addOnSuccessListener {
                    Log.d("RayFirebase", "Admin telemetry transaction complete")
                }
            } catch (e: Exception) {
                Log.w("RayFirebase", "Admin telemetry error: ${e.message}")
            }
        }
    }
    suspend fun backupToCloud(uid: String, jsonString: String): Result<Long> {
        if (!isFirebaseReady) return Result.failure(IllegalStateException("Firebase is not initialized"))
        return try {
            val now = System.currentTimeMillis()
            val docData = mapOf(
                "data" to jsonString,
                "updatedAt" to now,
                "schema" to 1,
                "platform" to "android"
            )
            val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val setTask = docRef.set(docData)

            val taskOk = try {
                com.google.android.gms.tasks.Tasks.await(setTask, 15, java.util.concurrent.TimeUnit.SECONDS)
                true
            } catch (e: Exception) {
                Log.w("RayFirebase", "Tasks.await timed out or failed, verifying write: ${e.message}")
                try {
                    val snap = com.google.android.gms.tasks.Tasks.await(docRef.get(), 5, java.util.concurrent.TimeUnit.SECONDS)
                    val snapTime = snap.getLong("updatedAt") ?: 0L
                    kotlin.math.abs(snapTime - now) < 60_000L
                } catch (_: Exception) {
                    false
                }
            }
            if (taskOk) {
                Result.success(now)
            } else {
                Result.failure(IllegalStateException("Bulut sunucusuna veri yazılamadı"))
            }
        } catch (e: Exception) {
            Log.e("RayFirebase", "Cloud backup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(uid: String): Result<String> {
        if (!isFirebaseReady) return Result.failure(IllegalStateException("Firebase is not initialized"))
        return try {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val snapshot = com.google.android.gms.tasks.Tasks.await(docRef.get(), 20, java.util.concurrent.TimeUnit.SECONDS)
            if (!snapshot.exists()) {
                return Result.failure(IllegalStateException("No cloud backup found for this account"))
            }
            val jsonString = snapshot.getString("data")
            if (jsonString.isNullOrEmpty()) {
                return Result.failure(IllegalStateException("Cloud backup data is empty"))
            }
            Result.success(jsonString)
        } catch (e: Exception) {
            Log.e("RayFirebase", "Cloud restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCloudBackupTimestamp(uid: String): Long? {
        if (!isFirebaseReady) return null
        return try {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val snapshot = com.google.android.gms.tasks.Tasks.await(docRef.get(), 10, java.util.concurrent.TimeUnit.SECONDS)
            snapshot.getLong("updatedAt")
        } catch (_: Exception) {
            null
        }
    }
}

class RayFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("RayFCM", "New FCM Push Registration Token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("RayFCM", "Push Message received: ${message.notification?.title} - ${message.notification?.body}")
    }
}
