package com.ray.iptv.data.admin

import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private data class LicenseCode(
    val value: String,
    val used: Boolean = false,
    val usedBy: String = "",
    val usedAt: Long = 0L
)

@Singleton
class AdminRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val lock = Mutex()
    private val memoryCodes = mutableListOf<LicenseCode>()
    private val memoryUsers = mutableListOf<AdminUser>()
    private val memoryOrders = mutableListOf<AdminOrder>()
    private val memoryCrashes = mutableListOf<CrashReport>()
    private val memoryOnline = mutableListOf<OnlineUser>()
    private val memoryHistory = mutableListOf<NotificationRecord>()

    private val _stats = MutableStateFlow(LicenseStats())
    val stats: StateFlow<LicenseStats> = _stats

    val firebaseReady: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("RayAdmin", "Firestore instance not available: ${e.message}")
            null
        }
    }

    suspend fun refreshStats(): LicenseStats = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val codesSnap = Tasks.await(firestore.collection("license_codes").get(), 6, TimeUnit.SECONDS)
                var totalCodes = 0
                var usedCodes = 0
                for (doc in codesSnap.documents) {
                    totalCodes++
                    if (doc.getBoolean("used") == true || doc.getBoolean("is_used") == true) {
                        usedCodes++
                    }
                }
                val result = LicenseStats(total = totalCodes, used = usedCodes)
                _stats.value = result
                return@withContext result
            } catch (e: Exception) {
                Log.w("RayAdmin", "refreshStats from firestore failed: ${e.message}")
            }
        }
        lock.withLock {
            val total = memoryCodes.size
            val used = memoryCodes.count { it.used }
            val result = LicenseStats(total, used)
            _stats.value = result
            result
        }
    }

    suspend fun fetchUnusedCode(): String? = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val snap = Tasks.await(
                    firestore.collection("license_codes")
                        .whereEqualTo("used", false)
                        .limit(1)
                        .get(),
                    6,
                    TimeUnit.SECONDS
                )
                if (!snap.isEmpty) {
                    val codeVal = snap.documents[0].getString("value") ?: snap.documents[0].id
                    return@withContext codeVal
                }
                // Generate a new code in Firestore
                val newCode = newCode()
                val codeData = mapOf(
                    "value" to newCode,
                    "used" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                Tasks.await(firestore.collection("license_codes").document(newCode).set(codeData), 6, TimeUnit.SECONDS)
                return@withContext newCode
            } catch (e: Exception) {
                Log.w("RayAdmin", "fetchUnusedCode from firestore failed: ${e.message}")
            }
        }
        lock.withLock {
            val unused = memoryCodes.firstOrNull { !it.used }
            if (unused != null) return@withContext unused.value
            val generated = LicenseCode(value = newCode(), used = false)
            memoryCodes += generated
            generated.value
        }
    }

    suspend fun listUsers(): List<AdminUser> = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val snap = Tasks.await(firestore.collection("users").get(), 8, TimeUnit.SECONDS)
                val list = mutableListOf<AdminUser>()
                for (doc in snap.documents) {
                    val uid = doc.id
                    val email = doc.getString("email").orEmpty()
                    val devName = doc.getString("lastDeviceName") ?: doc.getString("deviceName").orEmpty()
                    val devOs = doc.getString("lastDeviceOs") ?: doc.getString("deviceOs").orEmpty()
                    val isAnon = when {
                        doc.getBoolean("isAnonymous") == false -> false
                        email.isNotBlank() -> false
                        doc.getBoolean("isAnonymous") == true -> true
                        uid.startsWith("dev-") -> true
                        else -> false
                    }
                    val rawName = doc.getString("displayName") ?: doc.getString("name")
                    val name = when {
                        !rawName.isNullOrBlank() -> rawName
                        email.isNotBlank() -> email.substringBefore("@")
                        devName.isNotBlank() -> "$devName Kullanıcısı"
                        else -> "Anonim (${uid.take(8)})"
                    }
                    val isPremium = doc.getBoolean("isPremium") == true || doc.getBoolean("premium") == true
                    val isBanned = doc.getBoolean("isBanned") == true || doc.getBoolean("banned") == true
                    val lastActive = doc.getLong("lastActive") ?: doc.getLong("updatedAt") ?: doc.getLong("lastLoginAt") ?: 0L
                    val purchaseDate = doc.getString("purchaseDate").orEmpty()
                    val premiumSource = doc.getString("premiumSource").orEmpty()
                    val premiumExpiry = doc.getString("premiumExpiry").orEmpty()
                    val maxDev = doc.getLong("maxDevices")?.toInt() ?: 3
                    val photoUrl = doc.getString("photoUrl").orEmpty()

                    list.add(
                        AdminUser(
                            uid = uid,
                            email = email.ifBlank { if (isAnon) "Anonim (Giriş Yapılmadı)" else name },
                            displayName = name,
                            isPremium = isPremium,
                            isBanned = isBanned,
                            isAnonymous = isAnon,
                            lastLoginAt = lastActive,
                            lastDeviceName = devName.ifBlank { "Bilinmeyen Cihaz" },
                            lastDeviceOs = devOs,
                            purchaseDate = purchaseDate,
                            premiumSource = premiumSource,
                            premiumExpiry = premiumExpiry,
                            maxDevices = maxDev,
                            photoUrl = photoUrl
                        )
                    )
                }

                list.sortByDescending { it.lastLoginAt }
                lock.withLock {
                    memoryUsers.clear()
                    memoryUsers.addAll(list)
                }
                return@withContext list
            } catch (e: Exception) {
                Log.w("RayAdmin", "listUsers from firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryUsers.toList() }
    }

    suspend fun userDetail(uid: String): AdminUser? = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val doc = Tasks.await(firestore.collection("users").document(uid).get(), 6, TimeUnit.SECONDS)
                if (doc.exists()) {
                    val email = doc.getString("email").orEmpty()
                    val name = doc.getString("displayName") ?: doc.getString("name") ?: email.substringBefore("@")
                    return@withContext AdminUser(
                        uid = doc.id,
                        email = email.ifBlank { "Anonim (${doc.id.take(8)})" },
                        displayName = name,
                        isPremium = doc.getBoolean("isPremium") == true || doc.getBoolean("premium") == true,
                        isBanned = doc.getBoolean("isBanned") == true || doc.getBoolean("banned") == true,
                        isAnonymous = doc.getBoolean("isAnonymous") == true || email.isBlank(),
                        lastLoginAt = doc.getLong("lastActive") ?: doc.getLong("updatedAt") ?: 0L,
                        lastDeviceName = doc.getString("lastDeviceName").orEmpty(),
                        lastDeviceOs = doc.getString("lastDeviceOs").orEmpty(),
                        purchaseDate = doc.getString("purchaseDate").orEmpty(),
                        premiumSource = doc.getString("premiumSource").orEmpty(),
                        premiumExpiry = doc.getString("premiumExpiry").orEmpty(),
                        maxDevices = doc.getLong("maxDevices")?.toInt() ?: 3,
                        photoUrl = doc.getString("photoUrl").orEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.w("RayAdmin", "userDetail from firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryUsers.find { it.uid == uid } }
    }

    suspend fun manageUser(uid: String, action: String, limit: Int? = null): AdminNotice = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val docRef = firestore.collection("users").document(uid)
                val updates = mutableMapOf<String, Any>()
                when (action) {
                    "ban" -> {
                        updates["isBanned"] = true
                        updates["isPremium"] = false
                    }
                    "unban" -> {
                        updates["isBanned"] = false
                    }
                    "reset_devices" -> {
                        updates["lastDeviceName"] = ""
                        updates["lastDeviceOs"] = ""
                    }
                    "set_device_limit" -> {
                        updates["maxDevices"] = (limit ?: 3).coerceAtLeast(1)
                    }
                }
                Tasks.await(docRef.set(updates, SetOptions.merge()), 6, TimeUnit.SECONDS)
                return@withContext AdminNotice(true, "İşlem Firestore'a kaydedildi ($action)")
            } catch (e: Exception) {
                Log.w("RayAdmin", "manageUser firestore failed: ${e.message}")
            }
        }
        lock.withLock {
            val i = memoryUsers.indexOfFirst { it.uid == uid }
            if (i >= 0) {
                val cur = memoryUsers[i]
                memoryUsers[i] = when (action) {
                    "ban" -> cur.copy(isBanned = true, isPremium = false)
                    "unban" -> cur.copy(isBanned = false)
                    "reset_devices" -> cur.copy(lastDeviceName = "", lastDeviceOs = "")
                    "set_device_limit" -> cur.copy(maxDevices = (limit ?: cur.maxDevices).coerceAtLeast(0))
                    else -> cur
                }
            }
            AdminNotice(true, "İşlem tamamlandı ($action)")
        }
    }

    suspend fun grantPremium(uid: String, durationDays: Int, note: String): AdminNotice = withContext(Dispatchers.IO) {
        val expiry = if (durationDays <= 0) "" else isoDaysFromNow(durationDays)
        val nowStr = isoNow()
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val docRef = firestore.collection("users").document(uid)
                val userUpdates = mapOf(
                    "isPremium" to true,
                    "premiumSource" to "admin_grant",
                    "purchaseDate" to nowStr,
                    "premiumExpiry" to expiry
                )
                Tasks.await(docRef.set(userUpdates, SetOptions.merge()), 6, TimeUnit.SECONDS)

                // Add order
                val orderId = "ADMIN-" + UUID.randomUUID().toString().take(8).uppercase()
                val orderData = mapOf(
                    "uid" to uid,
                    "source" to "admin_grant",
                    "adminNote" to note.ifBlank { "Yönetici Hediyesi ($durationDays gün)" },
                    "purchaseDate" to nowStr,
                    "timestamp" to System.currentTimeMillis()
                )
                Tasks.await(firestore.collection("orders").document(orderId).set(orderData), 6, TimeUnit.SECONDS)
                return@withContext AdminNotice(true, "Premium Firestore'da başarıyla tanımlandı")
            } catch (e: Exception) {
                Log.w("RayAdmin", "grantPremium firestore failed: ${e.message}")
            }
        }
        lock.withLock {
            val i = memoryUsers.indexOfFirst { it.uid == uid }
            if (i >= 0) {
                memoryUsers[i] = memoryUsers[i].copy(
                    isPremium = true,
                    premiumSource = "admin_grant",
                    purchaseDate = nowStr,
                    premiumExpiry = expiry
                )
                memoryOrders.add(
                    0,
                    AdminOrder(
                        uid = uid,
                        email = memoryUsers[i].email,
                        source = "admin_grant",
                        adminNote = note.ifBlank { "Yönetici Hediyesi" },
                        purchaseDate = nowStr
                    )
                )
            }
            AdminNotice(true, "Premium verildi")
        }
    }

    suspend fun revokePremium(uid: String): AdminNotice = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val docRef = firestore.collection("users").document(uid)
                val userUpdates = mapOf(
                    "isPremium" to false,
                    "premiumSource" to "",
                    "premiumExpiry" to ""
                )
                Tasks.await(docRef.set(userUpdates, SetOptions.merge()), 6, TimeUnit.SECONDS)
                return@withContext AdminNotice(true, "Premium Firestore'da iptal edildi")
            } catch (e: Exception) {
                Log.w("RayAdmin", "revokePremium firestore failed: ${e.message}")
            }
        }
        lock.withLock {
            val i = memoryUsers.indexOfFirst { it.uid == uid }
            if (i >= 0) {
                memoryUsers[i] = memoryUsers[i].copy(isPremium = false, premiumSource = "", premiumExpiry = "")
            }
            AdminNotice(true, "Premium iptal edildi")
        }
    }

    suspend fun listOrders(): List<AdminOrder> = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val snap = Tasks.await(firestore.collection("orders").get(), 8, TimeUnit.SECONDS)
                val list = mutableListOf<AdminOrder>()
                for (doc in snap.documents) {
                    list.add(
                        AdminOrder(
                            uid = doc.getString("uid").orEmpty(),
                            email = doc.getString("email").orEmpty(),
                            source = doc.getString("source") ?: "Google Play",
                            productId = doc.getString("productId") ?: doc.getString("sku").orEmpty(),
                            playOrderId = doc.getString("playOrderId") ?: doc.id,
                            adminNote = doc.getString("adminNote").orEmpty(),
                            purchaseDate = doc.getString("purchaseDate") ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(doc.getLong("timestamp") ?: 0L))
                        )
                    )
                }
                list.sortByDescending { it.purchaseDate }
                lock.withLock {
                    memoryOrders.clear()
                    memoryOrders.addAll(list)
                }
                return@withContext list
            } catch (e: Exception) {
                Log.w("RayAdmin", "listOrders firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryOrders.toList() }
    }

    suspend fun listOnline(): List<OnlineUser> = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val cutoff = System.currentTimeMillis() - (5 * 60 * 1000L)
                val snap = Tasks.await(
                    firestore.collection("presence")
                        .whereGreaterThanOrEqualTo("lastSeen", cutoff)
                        .get(),
                    8,
                    TimeUnit.SECONDS
                )
                val list = mutableListOf<OnlineUser>()
                for (doc in snap.documents) {
                    list.add(
                        OnlineUser(
                            uid = doc.id,
                            name = doc.getString("name") ?: doc.getString("displayName") ?: "Kullanıcı",
                            email = doc.getString("email").orEmpty(),
                            photoUrl = doc.getString("photoUrl").orEmpty(),
                            lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis()
                        )
                    )
                }
                list.sortByDescending { it.lastSeen }
                lock.withLock {
                    memoryOnline.clear()
                    memoryOnline.addAll(list)
                }
                return@withContext list
            } catch (e: Exception) {
                Log.w("RayAdmin", "listOnline firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryOnline.toList() }
    }

    suspend fun listCrashes(): List<CrashReport> = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                val snap = Tasks.await(firestore.collection("app_crashes").limit(50).get(), 8, TimeUnit.SECONDS)
                val list = mutableListOf<CrashReport>()
                for (doc in snap.documents) {
                    val ts = doc.getLong("timestamp") ?: 0L
                    val dateStr = doc.getString("date") ?: if (ts > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts)) else ""
                    list.add(
                        CrashReport(
                            id = doc.id,
                            errorMessage = doc.getString("message") ?: "Bilinmeyen Hata",
                            stackTrace = doc.getString("stacktrace").orEmpty(),
                            dateString = dateStr,
                            platform = doc.getString("package") ?: "Android",
                            ram = doc.getString("tag") ?: "App",
                            fatal = doc.getBoolean("fatal") ?: true
                        )
                    )
                }
                list.sortByDescending { it.dateString }
                lock.withLock {
                    memoryCrashes.clear()
                    memoryCrashes.addAll(list)
                }
                return@withContext list
            } catch (e: Exception) {
                Log.w("RayAdmin", "listCrashes firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryCrashes.toList() }
    }

    suspend fun sendSegmented(segment: String, title: String, body: String): AdminNotice = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val notifId = UUID.randomUUID().toString()
        val notifRecord = NotificationRecord(
            id = notifId,
            title = title,
            body = body,
            type = "segmented",
            segment = segment,
            sentAt = System.currentTimeMillis()
        )
        if (firestore != null) {
            try {
                val notifData = mapOf(
                    "id" to notifId,
                    "title" to title,
                    "body" to body,
                    "segment" to segment,
                    "sentAt" to System.currentTimeMillis()
                )
                Tasks.await(firestore.collection("notifications").document(notifId).set(notifData), 6, TimeUnit.SECONDS)
                lock.withLock { memoryHistory.add(0, notifRecord) }
                return@withContext AdminNotice(true, "Bildirim Firestore üzerinden yayınlandı ($segment)")
            } catch (e: Exception) {
                Log.w("RayAdmin", "sendSegmented firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryHistory.add(0, notifRecord) }
        AdminNotice(true, "Bildirim kaydedildi")
    }

    suspend fun scheduleNotification(segment: String, title: String, body: String, atMs: Long): AdminNotice = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val notifId = UUID.randomUUID().toString()
        val notifRecord = NotificationRecord(
            id = notifId,
            title = title,
            body = body,
            type = "scheduled",
            segment = segment,
            sentAt = atMs
        )
        if (firestore != null) {
            try {
                val notifData = mapOf(
                    "id" to notifId,
                    "title" to title,
                    "body" to body,
                    "segment" to segment,
                    "scheduledAt" to atMs,
                    "createdAt" to System.currentTimeMillis()
                )
                Tasks.await(firestore.collection("notifications").document(notifId).set(notifData), 6, TimeUnit.SECONDS)
                lock.withLock { memoryHistory.add(0, notifRecord) }
                return@withContext AdminNotice(true, "Bildirim zamanlandı")
            } catch (e: Exception) {
                Log.w("RayAdmin", "scheduleNotification firestore failed: ${e.message}")
            }
        }
        lock.withLock { memoryHistory.add(0, notifRecord) }
        AdminNotice(true, "Bildirim zamanlandı")
    }

    suspend fun sendAdminMessage(uid: String, message: String): AdminNotice = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val notifId = UUID.randomUUID().toString()
        if (firestore != null) {
            try {
                val msgData = mapOf(
                    "id" to notifId,
                    "targetUid" to uid,
                    "title" to "Yönetici Mesajı",
                    "body" to message,
                    "type" to "direct",
                    "sentAt" to System.currentTimeMillis()
                )
                Tasks.await(firestore.collection("user_messages").document(notifId).set(msgData), 6, TimeUnit.SECONDS)
                return@withContext AdminNotice(true, "Mesaj kullanıcıya iletildi")
            } catch (e: Exception) {
                Log.w("RayAdmin", "sendAdminMessage firestore failed: ${e.message}")
            }
        }
        AdminNotice(true, "Mesaj gönderildi")
    }

    suspend fun sendUserNotification(uid: String, title: String, body: String): AdminNotice = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val notifId = UUID.randomUUID().toString()
        if (firestore != null) {
            try {
                val msgData = mapOf(
                    "id" to notifId,
                    "targetUid" to uid,
                    "title" to title,
                    "body" to body,
                    "type" to "direct",
                    "sentAt" to System.currentTimeMillis()
                )
                Tasks.await(firestore.collection("user_messages").document(notifId).set(msgData), 6, TimeUnit.SECONDS)
                return@withContext AdminNotice(true, "Bildirim kullanıcıya iletildi")
            } catch (e: Exception) {
                Log.w("RayAdmin", "sendUserNotification firestore failed: ${e.message}")
            }
        }
        AdminNotice(true, "Bildirim gönderildi")
    }

    suspend fun notificationHistory(): List<NotificationRecord> = lock.withLock {
        memoryHistory.toList()
    }

    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        fun chunk(n: Int) = (1..n).map { alphabet.random() }.joinToString("")
        return "RAY-${chunk(4)}-${chunk(4)}-${chunk(4)}"
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

    private fun isoDaysFromNow(days: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal.time)
    }
}
