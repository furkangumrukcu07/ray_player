package com.ray.iptv.data.firebase

import android.content.Context
import android.os.Build
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
                
                try {
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().addOnSuccessListener {
                            Log.d("RayFirebase", "Anonymous auth established: ${it.user?.uid}")
                        }
                    }
                } catch (_: Exception) {}

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
    suspend fun backupToCloud(uid: String, jsonString: String, email: String = ""): Result<Long> {
        if (!isFirebaseReady) initFirebaseGuarded()
        if (!isFirebaseReady) return Result.failure(IllegalStateException("Firebase is not initialized"))
        return try {
            val now = System.currentTimeMillis()
            val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val cleanEmail = email.ifBlank { authUser?.email.orEmpty() }.trim().lowercase()
            val docData = mutableMapOf<String, Any>(
                "data" to jsonString,
                "updatedAt" to now,
                "lastActive" to now,
                "schema" to 1,
                "platform" to "android"
            )
            if (cleanEmail.isNotBlank()) {
                docData["email"] = cleanEmail
                docData["isAnonymous"] = false
            }
            if (authUser != null) {
                if (!authUser.displayName.isNullOrBlank()) {
                    docData["displayName"] = authUser.displayName!!
                }
                if (authUser.photoUrl != null) {
                    docData["photoUrl"] = authUser.photoUrl.toString()
                }
            }
            val targetDocId = uid.ifBlank { authUser?.uid.orEmpty() }
            if (targetDocId.isBlank()) {
                return Result.failure(IllegalStateException("Kullanıcı kimliği (UID) bulunamadı"))
            }
            val docRef = FirebaseFirestore.getInstance().collection("users").document(targetDocId)
            val setTask = docRef.set(docData, com.google.firebase.firestore.SetOptions.merge())

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


    private suspend fun getDocSnapshotWithListener(docRef: com.google.firebase.firestore.DocumentReference, timeoutMs: Long = 10_000L): com.google.firebase.firestore.DocumentSnapshot? {
        return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                var reg: com.google.firebase.firestore.ListenerRegistration? = null
                reg = docRef.addSnapshotListener { snap, err ->
                    if (cont.isActive) {
                        if (snap != null) {
                            reg?.remove()
                            cont.resume(snap, null)
                        } else if (err != null) {
                            Log.w("RayFirebase", "addSnapshotListener error: ${err.message}")
                        }
                    }
                }
                cont.invokeOnCancellation {
                    reg?.remove()
                }
            }
        }
    }

    suspend fun restoreFromCloud(uid: String, email: String = ""): Result<String> {
        if (!isFirebaseReady) initFirebaseGuarded()
        if (!isFirebaseReady) return Result.failure(IllegalStateException("Firebase is not initialized"))
        
        val firestore = FirebaseFirestore.getInstance()
        runCatching { firestore.enableNetwork() }

        // 1. Try direct Document ID query by UID
        if (uid.isNotBlank()) {
            val docRef = firestore.collection("users").document(uid)
            for (attempt in 0 until 2) {
                try {
                    val snapshot = try {
                        com.google.android.gms.tasks.Tasks.await(docRef.get(), 8, java.util.concurrent.TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        getDocSnapshotWithListener(docRef, timeoutMs = 5_000L)
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val jsonString = snapshot.getString("data")
                        if (!jsonString.isNullOrEmpty()) {
                            Log.d("RayFirebase", "Cloud backup found by UID ($uid)")
                            return Result.success(jsonString)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("RayFirebase", "Cloud restore attempt $attempt by UID failed: ${e.message}")
                }
            }
        }

        // 2. Fallback: Query by Google verified email
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isNotBlank()) {
            try {
                val queryTask = firestore.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                val querySnap = com.google.android.gms.tasks.Tasks.await(queryTask, 8, java.util.concurrent.TimeUnit.SECONDS)
                if (querySnap != null && !querySnap.isEmpty) {
                    val bestDoc = querySnap.documents
                        .filter { !it.getString("data").isNullOrEmpty() }
                        .maxByOrNull { it.getLong("updatedAt") ?: 0L }
                    val jsonString = bestDoc?.getString("data")
                    if (!jsonString.isNullOrEmpty()) {
                        Log.d("RayFirebase", "Cloud backup found by Email ($cleanEmail), docId=${bestDoc.id}")
                        return Result.success(jsonString)
                    }
                }
            } catch (e: Exception) {
                Log.w("RayFirebase", "Cloud restore by email query failed: ${e.message}")
            }
        }

        return Result.failure(NoSuchElementException("NO_BACKUP"))
    }


    suspend fun deleteCloudData(uid: String): Result<Boolean> {
        if (!isFirebaseReady) return Result.failure(IllegalStateException("Firebase is not initialized"))
        return try {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val task = docRef.delete()
            com.google.android.gms.tasks.Tasks.await(task, 10, java.util.concurrent.TimeUnit.SECONDS)
            Result.success(true)
        } catch (e: Exception) {
            Log.e("RayFirebase", "deleteCloudData failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Boolean> {
        return try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null) {
                deleteCloudData(user.uid)
                com.google.android.gms.tasks.Tasks.await(user.delete(), 10, java.util.concurrent.TimeUnit.SECONDS)
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e("RayFirebase", "deleteAccount failed: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun syncUserProfile(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String = "",
        isPremium: Boolean = false,
        isAnonymous: Boolean = false,
        licenseCode: String = ""
    ) {
        if (!isFirebaseReady || uid.isBlank()) return
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val isTv = try {
                    val pm = context.packageManager
                    pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
                    pm.hasSystemFeature("android.hardware.type.television")
                } catch (_: Exception) { false }

                val deviceCategory = if (isTv) "TV Box" else "Mobil"
                val devName = "$deviceCategory (${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL})"
                val devOs = "Android ${Build.VERSION.RELEASE}"
                val isAnon = if (email.isNotBlank()) false else isAnonymous
                val version = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.3.23"
                } catch (_: Exception) { "1.3.23" }

                val data = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName.ifBlank { if (email.isNotBlank()) email.substringBefore("@") else if (isTv) "Android TV Kullanıcısı" else "Mobil Kullanıcı" },
                    "photoUrl" to photoUrl,
                    "isPremium" to isPremium,
                    "isAnonymous" to isAnon,
                    "lastDeviceName" to devName,
                    "lastDeviceOs" to devOs,
                    "appVersion" to version,
                    "lastActive" to System.currentTimeMillis()
                )
                if (licenseCode.isNotBlank()) {
                    data["licenseCode"] = licenseCode
                }
                firestore.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                Log.d("RayFirebase", "User profile synced to Firestore: $uid ($email, dev=$devName)")
            } catch (e: Exception) {
                Log.w("RayFirebase", "syncUserProfile error: ${e.message}")
            }
        }
    }


    fun syncPresence(
        uid: String,
        name: String,
        email: String = "",
        photoUrl: String = ""
    ) {
        if (!isFirebaseReady || uid.isBlank()) return
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val devName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
                val data = mapOf(
                    "uid" to uid,
                    "name" to name.ifBlank { email.substringBefore("@").ifBlank { "Kullanıcı" } },
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "deviceName" to devName,
                    "lastSeen" to System.currentTimeMillis()
                )
                firestore.collection("presence").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.w("RayFirebase", "syncPresence error: ${e.message}")
            }
        }
    }

    fun logOrder(
        uid: String,
        email: String,
        productId: String,
        playOrderId: String,
        source: String = "Google Play",
        adminNote: String = ""
    ) {
        if (!isFirebaseReady) return
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val id = playOrderId.ifBlank { "ORD-" + UUID.randomUUID().toString().take(8).uppercase() }
                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val data = mapOf(
                    "uid" to uid,
                    "email" to email,
                    "productId" to productId,
                    "playOrderId" to id,
                    "source" to source,
                    "adminNote" to adminNote,
                    "purchaseDate" to nowStr,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("orders").document(id).set(data, com.google.firebase.firestore.SetOptions.merge())
                Log.d("RayFirebase", "Order recorded in Firestore: $id")
            } catch (e: Exception) {
                Log.w("RayFirebase", "logOrder error: ${e.message}")
            }
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

    suspend fun getCloudBackupSummary(uid: String): CloudBackupSummary? {
        if (!isFirebaseReady) return null
        return try {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            val snapshot = try {
                com.google.android.gms.tasks.Tasks.await(docRef.get(), 10, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: Exception) {
                getDocSnapshotWithListener(docRef, timeoutMs = 6_000L)
            }
            if (snapshot == null || !snapshot.exists()) return null

            val jsonStr = snapshot.getString("data") ?: return null
            val updatedAt = snapshot.getLong("updatedAt") ?: 0L
            val platform = snapshot.getString("platform") ?: "android"
            val sizeBytes = jsonStr.toByteArray(Charsets.UTF_8).size.toLong()
            
            var sourcesCount = 0
            var profilesCount = 0
            var favoritesCount = 0
            var progressCount = 0
            var epgCount = 0
            var settingsCount = 0
            
            runCatching {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val obj = json.decodeFromString<kotlinx.serialization.json.JsonObject>(jsonStr)
                sourcesCount = (obj["sources"] as? kotlinx.serialization.json.JsonArray)?.size ?: 0
                profilesCount = (obj["profiles"] as? kotlinx.serialization.json.JsonArray)?.size ?: 0
                favoritesCount = (obj["favorites"] as? kotlinx.serialization.json.JsonArray)?.size ?: 0
                progressCount = (obj["progress"] as? kotlinx.serialization.json.JsonArray)?.size ?: 0
                epgCount = (obj["epgSources"] as? kotlinx.serialization.json.JsonArray)?.size ?: 0
                settingsCount = (obj["settings"] as? kotlinx.serialization.json.JsonObject)?.size ?: 0
            }
            
            CloudBackupSummary(
                sizeBytes = sizeBytes,
                updatedAt = updatedAt,
                sourcesCount = sourcesCount,
                profilesCount = profilesCount,
                favoritesCount = favoritesCount,
                progressCount = progressCount,
                epgCount = epgCount,
                settingsCount = settingsCount,
                platform = platform
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class CloudBackupSummary(
    val sizeBytes: Long = 0L,
    val updatedAt: Long = 0L,
    val sourcesCount: Int = 0,
    val profilesCount: Int = 0,
    val favoritesCount: Int = 0,
    val progressCount: Int = 0,
    val epgCount: Int = 0,
    val settingsCount: Int = 0,
    val platform: String = "android"
) {
    val sizeLabel: String
        get() {
            if (sizeBytes <= 0) return "0 KB"
            val mb = sizeBytes / (1024f * 1024f)
            if (mb >= 1f) return String.format(java.util.Locale.US, "%.2f MB", mb)
            val kb = sizeBytes / 1024f
            return String.format(java.util.Locale.US, "%.1f KB", kb)
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
