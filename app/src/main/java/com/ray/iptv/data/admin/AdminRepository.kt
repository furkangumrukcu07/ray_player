package com.ray.iptv.data.admin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Admin backend.
 *
 * Mina tarafında Firestore + Cloud Functions (`adminListAllUsers`,
 * `adminGetLatestOrders`, `adminSendSegmentedNotification`, …).
 * Google/Firebase bağlanınca bu sınıfın gövdesi callable'lara çevrilecek;
 * ekranlar aynı kalır.
 */
@Singleton
class AdminRepository @Inject constructor() {
    private val lock = Mutex()
    private val codes = mutableListOf<LicenseCode>()
    private val users = mutableListOf<AdminUser>()
    private val orders = mutableListOf<AdminOrder>()
    private val crashes = mutableListOf<CrashReport>()
    private val online = mutableListOf<OnlineUser>()
    private val history = mutableListOf<NotificationRecord>()

    private val _stats = MutableStateFlow(LicenseStats())
    val stats: StateFlow<LicenseStats> = _stats

    val firebaseReady: Boolean = false

    suspend fun refreshStats(): LicenseStats = lock.withLock {
        // TODO: Firestore license_codes count
        delay(120)
        emitStatsLocked()
    }

    suspend fun fetchUnusedCode(): String? = lock.withLock {
        // TODO: Firestore license_codes where is_used == false limit 1
        delay(180)
        val unused = codes.firstOrNull { !it.used }
        if (unused != null) return unused.value
        val generated = LicenseCode(value = newCode(), used = false)
        codes += generated
        emitStatsLocked()
        generated.value
    }

    suspend fun listUsers(): List<AdminUser> = lock.withLock {
        // TODO: httpsCallable adminListAllUsers
        delay(150)
        users.toList()
    }

    suspend fun userDetail(uid: String): AdminUser? = lock.withLock {
        // TODO: httpsCallable adminGetUserDetail
        delay(80)
        users.find { it.uid == uid }
    }

    suspend fun manageUser(uid: String, action: String, limit: Int? = null): AdminNotice =
        lock.withLock {
            // TODO: httpsCallable adminManageUser
            delay(120)
            val i = users.indexOfFirst { it.uid == uid }
            if (i < 0) return AdminNotice(false, "Kullanıcı bulunamadı")
            val cur = users[i]
            users[i] = when (action) {
                "ban" -> cur.copy(isBanned = true, isPremium = false)
                "unban" -> cur.copy(isBanned = false)
                "reset_devices" -> cur.copy(lastDeviceName = "", lastDeviceOs = "")
                "set_device_limit" -> cur.copy(maxDevices = (limit ?: cur.maxDevices).coerceAtLeast(0))
                else -> cur
            }
            AdminNotice(true, "İşlem tamamlandı ($action)")
        }

    suspend fun grantPremium(uid: String, durationDays: Int, note: String): AdminNotice =
        lock.withLock {
            // TODO: httpsCallable adminGrantPremium
            delay(140)
            val i = users.indexOfFirst { it.uid == uid }
            if (i < 0) return AdminNotice(false, "Kullanıcı bulunamadı")
            val expiry = if (durationDays <= 0) "" else isoDaysFromNow(durationDays)
            users[i] = users[i].copy(
                isPremium = true,
                premiumSource = "admin_grant",
                purchaseDate = isoNow(),
                premiumExpiry = expiry
            )
            orders.add(
                0,
                AdminOrder(
                    uid = uid,
                    email = users[i].email,
                    source = "admin_grant",
                    adminNote = note.ifBlank { "Yönetici Hediyesi" },
                    purchaseDate = isoNow()
                )
            )
            AdminNotice(true, "Premium verildi")
        }

    suspend fun revokePremium(uid: String): AdminNotice = lock.withLock {
        // TODO: httpsCallable adminRevokePremium
        delay(120)
        val i = users.indexOfFirst { it.uid == uid }
        if (i < 0) return AdminNotice(false, "Kullanıcı bulunamadı")
        users[i] = users[i].copy(isPremium = false, premiumSource = "", premiumExpiry = "")
        AdminNotice(true, "Premium iptal edildi")
    }

    suspend fun listOrders(): List<AdminOrder> = lock.withLock {
        // TODO: httpsCallable adminGetLatestOrders
        delay(120)
        orders.toList()
    }

    suspend fun listOnline(): List<OnlineUser> = lock.withLock {
        // TODO: Firestore presence where lastSeen > now-3m
        delay(80)
        online.toList()
    }

    suspend fun listCrashes(): List<CrashReport> = lock.withLock {
        // TODO: Firestore app_crashes
        delay(80)
        crashes.toList()
    }

    suspend fun sendSegmented(segment: String, title: String, body: String): AdminNotice {
        // TODO: httpsCallable adminSendSegmentedNotification + çeviri
        delay(200)
        lock.withLock {
            history.add(
                0,
                NotificationRecord(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    body = body,
                    type = "segmented",
                    segment = segment,
                    sentAt = System.currentTimeMillis()
                )
            )
        }
        return AdminNotice(true, "Bildirim kuyruğa alındı (Firebase sonra bağlanacak)")
    }

    suspend fun scheduleNotification(
        segment: String,
        title: String,
        body: String,
        atMs: Long
    ): AdminNotice {
        // TODO: httpsCallable adminScheduleNotification
        delay(200)
        lock.withLock {
            history.add(
                0,
                NotificationRecord(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    body = body,
                    type = "scheduled",
                    segment = segment,
                    sentAt = atMs
                )
            )
        }
        return AdminNotice(true, "Bildirim zamanlandı (Firebase sonra bağlanacak)")
    }

    suspend fun sendUserNotification(uid: String, title: String, body: String): AdminNotice {
        // TODO: httpsCallable adminSendUserNotification
        delay(160)
        lock.withLock {
            history.add(
                0,
                NotificationRecord(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    body = body,
                    type = "user",
                    segment = uid,
                    sentAt = System.currentTimeMillis()
                )
            )
        }
        return AdminNotice(true, "Kişisel bildirim kuyruğa alındı")
    }

    suspend fun notificationHistory(): List<NotificationRecord> = lock.withLock {
        // TODO: Firestore notification_history
        history.toList()
    }

    suspend fun sendAdminMessage(uid: String, text: String): AdminNotice {
        // TODO: ChatService.sendAdminMessageToUser
        delay(120)
        if (text.isBlank()) return AdminNotice(false, "Mesaj boş")
        return AdminNotice(true, "Mesaj gönderildi (Firebase sonra bağlanacak)")
    }

    private fun emitStatsLocked(): LicenseStats {
        val s = LicenseStats(total = codes.size, used = codes.count { it.used })
        _stats.value = s
        return s
    }

    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        fun chunk() = (1..4).map { alphabet.random() }.joinToString("")
        return "RAY-${chunk()}-${chunk()}"
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

    private fun isoDaysFromNow(days: Int): String {
        val ms = System.currentTimeMillis() + days * 86_400_000L
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(ms))
    }

    private data class LicenseCode(val value: String, var used: Boolean)
}
