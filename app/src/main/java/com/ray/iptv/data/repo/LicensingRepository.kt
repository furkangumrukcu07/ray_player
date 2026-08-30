package com.ray.iptv.data.repo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class LicenseDeviceInfo(
    val deviceId: String = "",
    val model: String = "",
    val osVersion: String = "",
    val registeredAtMs: Long = 0L,
    val lastActiveMs: Long = 0L
)

data class LicensingState(
    val isPremium: Boolean = false,
    val isTrialActive: Boolean = true,
    val trialExpirationMs: Long? = null,
    val trialRemainingFormatted: String = "",
    val deviceCount: Int = 1,
    val maxDevices: Int = 3,
    val isDeviceLimitExceeded: Boolean = false,
    val isGrandfathered: Boolean = false,
    val isEnforced: Boolean = false // Soft-launch toggle (false = never lock out users before Play Store launch)
)

@Singleton
class LicensingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ds = context.rayStore

    companion object {
        const val TRIAL_DURATION_DAYS = 4
        /**
         * Soft-launch kontrolü:
         * false = Tüm trial ve lisanslar hesaplanır, kalan süre gösterilir ancak kullanıcı asla kilitlenmez.
         * true = Play Store'a yüklendikten sonra açılır; trial bittiğinde ödeme ekranı zorunlu olur.
         */
        const val ENFORCE_PAYWALL = false

        private val KEY_TRIAL_START = longPreferencesKey("ray_trial_start_ms")
        private val KEY_LOCAL_PREMIUM = booleanPreferencesKey("ray_local_premium")
        private val KEY_LICENSE_CODE = stringPreferencesKey("ray_license_code")
    }

    private val _state = MutableStateFlow(LicensingState(isEnforced = ENFORCE_PAYWALL))
    val state: StateFlow<LicensingState> = _state.asStateFlow()

    init {
        scope.launch {
            bootstrapLicensing()
        }
    }

    @SuppressLint("HardwareIds")
    fun getHardwareDeviceId(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            val raw = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD}_${Build.HARDWARE}_$androidId"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }.take(20)
        } catch (_: Exception) {
            "ray_dev_${Build.MODEL.hashCode().toUInt()}"
        }
    }

    fun getDeviceModelName(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    private fun getPackageInstallTimeMs(): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    suspend fun refresh() {
        bootstrapLicensing()
    }

    private suspend fun bootstrapLicensing() {
        val now = System.currentTimeMillis()
        val installTime = getPackageInstallTimeMs()

        // 1. Yerel Lisans Kontrolü
        val prefs = ds.data.first()
        var isPremium = prefs[KEY_LOCAL_PREMIUM] == true
        var trialStartMs = prefs[KEY_TRIAL_START] ?: 0L

        if (trialStartMs <= 0L) {
            trialStartMs = installTime
            ds.edit { it[KEY_TRIAL_START] = trialStartMs }
        }

        // 2. Firebase Firestore Anti-Reset Kontrolü
        val hardwareId = getHardwareDeviceId()
        try {
            val firestore = FirebaseFirestore.getInstance()
            val trialDocRef = firestore.collection("device_trials").document(hardwareId)
            val snap = trialDocRef.get().await()

            if (snap != null && snap.exists()) {
                val serverStart = snap.getLong("trialStartMs") ?: 0L
                if (serverStart > 0L) {
                    // Sunucudaki başlangıç tarihi yerelden daha eskiyse sunucuyu baz al (Anti-Reset)
                    trialStartMs = minOf(trialStartMs, serverStart)
                    ds.edit { it[KEY_TRIAL_START] = trialStartMs }
                }
            } else {
                // İlk defa kaydediliyor
                trialDocRef.set(
                    mapOf(
                        "hardwareId" to hardwareId,
                        "model" to getDeviceModelName(),
                        "firstInstallMs" to installTime,
                        "trialStartMs" to trialStartMs,
                        "createdAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }

            // Kullanıcı lisans kontrolü & Admin Whitelist
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val userEmail = user?.email?.lowercase()?.trim().orEmpty()
            val manualPremiums = listOf("furkangumrukcu07@gmail.com", "allachehata@gmail.com")

            if (userEmail.isNotEmpty() && manualPremiums.contains(userEmail)) {
                isPremium = true
                ds.edit { it[KEY_LOCAL_PREMIUM] = true }
            } else if (user != null) {
                val licenseDoc = firestore.collection("user_licenses").document(user.uid).get().await()
                if (licenseDoc != null && licenseDoc.exists() && licenseDoc.getBoolean("isPremium") == true) {
                    isPremium = true
                    ds.edit { it[KEY_LOCAL_PREMIUM] = true }
                }
            }
        } catch (_: Exception) {
            // Çevrimdışı fallback: Yerel verilerle devam et
        }

        // Whitelist kontrolü (çevrimdışı olsa bile)
        val authUserEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim().orEmpty()
        if (authUserEmail == "furkangumrukcu07@gmail.com" || authUserEmail == "allachehata@gmail.com") {
            isPremium = true
        }

        // 3. Kalan Süre Hesaplama
        val expireMs = trialStartMs + (TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L)
        val isTrialActive = if (isPremium) false else (now < expireMs)
        val remainingFormatted = if (isPremium) "Sınırsız (Ömür Boyu)" else formatRemainingTime(expireMs, now)

        _state.value = LicensingState(
            isPremium = isPremium,
            isTrialActive = isTrialActive,
            trialExpirationMs = expireMs,
            trialRemainingFormatted = remainingFormatted,
            deviceCount = 1,
            maxDevices = 3,
            isDeviceLimitExceeded = false,
            isGrandfathered = false,
            isEnforced = ENFORCE_PAYWALL
        )
    }

    private fun formatRemainingTime(expireMs: Long, nowMs: Long): String {
        if (nowMs >= expireMs) return "Süre Doldu"
        val diffMs = expireMs - nowMs
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60

        return when {
            days > 0 -> "$days gün $hours saat"
            hours > 0 -> "$hours saat $minutes dk"
            else -> "$minutes dakika"
        }
    }

    suspend fun redeemLicenseCode(code: String): Result<String> {
        val trimmed = code.trim().uppercase(Locale.ROOT)
        if (trimmed.length < 8) {
            return Result.failure(Exception("Geçersiz lisans kodu formatı"))
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            var uid = auth.currentUser?.uid

            if (uid == null) {
                // Anonim oturum aç
                val authRes = auth.signInAnonymously().await()
                uid = authRes.user?.uid ?: "user_${getHardwareDeviceId()}"
            }

            val codeDoc = firestore.collection("license_codes").document(trimmed).get().await()
            if (codeDoc != null && codeDoc.exists()) {
                val isUsed = codeDoc.getBoolean("isUsed") ?: false
                if (isUsed) {
                    return Result.failure(Exception("Bu lisans kodu daha önce kullanılmış."))
                }

                val now = System.currentTimeMillis()
                // Kodu kullanıldı olarak işaretle
                firestore.collection("license_codes").document(trimmed).update(
                    mapOf(
                        "isUsed" to true,
                        "usedByUid" to uid,
                        "usedAt" to now,
                        "hardwareId" to getHardwareDeviceId()
                    )
                ).await()

                // Kullanıcı lisansı oluştur
                firestore.collection("user_licenses").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "isPremium" to true,
                        "licenseCode" to trimmed,
                        "maxDevices" to 3,
                        "activatedAt" to now,
                        "updatedAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))
                    ),
                    SetOptions.merge()
                ).await()
            }

            // Yerel durumu Premium yap
            ds.edit {
                it[KEY_LOCAL_PREMIUM] = true
                it[KEY_LICENSE_CODE] = trimmed
            }

            _state.value = _state.value.copy(
                isPremium = true,
                isTrialActive = false
            )

            Result.success("Lisansınız başarıyla etkinleştirildi! Ömür boyu Premium aktif.")
        } catch (e: Exception) {
            // Test veya çevrimdışı master kod desteği
            if (trimmed == "RAY-PREMIUM-2026" || trimmed == "MINA-RAY-LIFETIME") {
                ds.edit {
                    it[KEY_LOCAL_PREMIUM] = true
                    it[KEY_LICENSE_CODE] = trimmed
                }
                _state.value = _state.value.copy(
                    isPremium = true,
                    isTrialActive = false
                )
                Result.success("Özel lisansınız başarıyla etkinleştirildi.")
            } else {
                Result.failure(Exception("Lisans doğrulanamadı: ${e.localizedMessage ?: "Sunucuya ulaşılamadı"}"))
            }
        }
    }
}
