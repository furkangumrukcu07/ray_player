package com.ray.iptv.data.account

data class AccountSession(
    val signedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val uid: String = "",
    val isPremium: Boolean = false,
    val isTrial: Boolean = false,
    val licenseCode: String = "",
    val trialUntilMs: Long = 0L
) {
    val isAdmin: Boolean get() = RayAdmin.isAdmin(email)
    val letter: String
        get() = (displayName.ifBlank { email }.firstOrNull() ?: 'R')
            .uppercaseChar()
            .toString()
}

object RayAdmin {
    const val EMAIL = "furkangumrukcu07@gmail.com"
    val premiumEmails = setOf(
        EMAIL,
        "allachehata@gmail.com"
    )

    fun normalize(email: String?): String = email.orEmpty().trim().lowercase()

    fun isAdmin(email: String?): Boolean = normalize(email) == EMAIL

    fun isManualPremium(email: String?): Boolean = normalize(email) in premiumEmails
}
