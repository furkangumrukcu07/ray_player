package com.ray.iptv.data.admin

data class LicenseStats(
    val total: Int = 0,
    val used: Int = 0
) {
    val remaining: Int get() = (total - used).coerceAtLeast(0)
}

data class AdminUser(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val isPremium: Boolean = false,
    val isBanned: Boolean = false,
    val isAnonymous: Boolean = false,
    val lastLoginAt: Long = 0L,
    val lastDeviceName: String = "",
    val lastDeviceOs: String = "",
    val purchaseDate: String = "",
    val premiumSource: String = "",
    val premiumExpiry: String = "",
    val maxDevices: Int = 3,
    val photoUrl: String = ""
)

data class AdminOrder(
    val uid: String = "",
    val email: String = "",
    val source: String = "",
    val productId: String = "",
    val playOrderId: String = "",
    val adminNote: String = "",
    val purchaseDate: String = ""
)

data class OnlineUser(
    val uid: String,
    val name: String,
    val email: String = "",
    val photoUrl: String = "",
    val lastSeen: Long = 0L
)

data class CrashReport(
    val id: String,
    val errorMessage: String,
    val stackTrace: String = "",
    val dateString: String = "",
    val platform: String = "",
    val ram: String = "",
    val fatal: Boolean = false
)

data class NotificationRecord(
    val id: String,
    val title: String,
    val body: String = "",
    val type: String = "segmented",
    val segment: String = "all",
    val sentAt: Long = 0L
)

data class AdminNotice(
    val ok: Boolean,
    val message: String
)
