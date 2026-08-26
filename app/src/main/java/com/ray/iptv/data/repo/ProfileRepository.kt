package com.ray.iptv.data.repo

import com.ray.iptv.data.local.ProfileEntity
import com.ray.iptv.data.local.RayDatabase
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val db: RayDatabase
) {
    fun observe() = db.profiles().observe()
    suspend fun all() = db.profiles().all()
    suspend fun byId(id: String) = db.profiles().byId(id)

    suspend fun create(name: String, pin: String?, kids: Boolean): ProfileEntity {
        val entity = ProfileEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Profile" },
            pinHash = pin?.takeIf { it.isNotBlank() }?.let { hashPin(it) },
            isKids = kids,
            avatarHue = (0..360).random().toFloat(),
            createdAt = System.currentTimeMillis()
        )
        db.profiles().upsert(entity)
        return entity
    }

    suspend fun delete(id: String) = db.profiles().delete(id)

    fun verifyPin(stored: String?, input: String): Boolean {
        if (stored.isNullOrBlank()) return true
        return stored == hashPin(input)
    }

    companion object {
        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(("ray-pin|" + pin).toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
