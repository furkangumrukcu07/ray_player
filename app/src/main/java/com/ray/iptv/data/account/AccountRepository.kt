package com.ray.iptv.data.account

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rayAccountStore by preferencesDataStore("ray_account")

@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val ds = context.rayAccountStore

    val session: Flow<AccountSession> = ds.data.map { p ->
        val email = p[Keys.email].orEmpty()
        val signedIn = p[Keys.signedIn] == true && email.isNotBlank()
        val premium = p[Keys.premium] == true || RayAdmin.isManualPremium(email)
        AccountSession(
            signedIn = signedIn,
            email = email,
            displayName = p[Keys.name].orEmpty(),
            photoUrl = p[Keys.photo].orEmpty(),
            uid = p[Keys.uid].orEmpty(),
            isPremium = signedIn && premium,
            isTrial = signedIn && p[Keys.trial] == true && !premium,
            licenseCode = p[Keys.license].orEmpty(),
            trialUntilMs = p[Keys.trialUntil] ?: 0L
        )
    }

    suspend fun signIn(
        email: String,
        displayName: String,
        photoUrl: String = "",
        uid: String = ""
    ) {
        val clean = RayAdmin.normalize(email)
        require(clean.isNotBlank()) { "email" }
        val premium = RayAdmin.isManualPremium(clean)
        ds.edit {
            it[Keys.signedIn] = true
            it[Keys.email] = clean
            it[Keys.name] = displayName.ifBlank { clean.substringBefore("@") }
            it[Keys.photo] = photoUrl
            it[Keys.uid] = uid.ifBlank { "local-$clean" }
            it[Keys.premium] = premium
            it[Keys.trial] = false
            if (premium) it[Keys.license] = it[Keys.license].orEmpty()
        }
    }

    suspend fun signOut() {
        ds.edit { it.clear() }
    }

    suspend fun setPremium(value: Boolean) = ds.edit { it[Keys.premium] = value }

    suspend fun setLicenseCode(code: String) = ds.edit { it[Keys.license] = code }

    private object Keys {
        val signedIn = booleanPreferencesKey("signed_in")
        val email = stringPreferencesKey("email")
        val name = stringPreferencesKey("name")
        val photo = stringPreferencesKey("photo")
        val uid = stringPreferencesKey("uid")
        val premium = booleanPreferencesKey("premium")
        val trial = booleanPreferencesKey("trial")
        val license = stringPreferencesKey("license")
        val trialUntil = longPreferencesKey("trial_until")
    }
}
