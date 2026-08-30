package com.ray.iptv.data.chat

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CommunityChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhotoUrl: String = "",
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Singleton
class CommunityChatRepository @Inject constructor() {

    private val tag = "CommunityChat"

    fun listenMessages(): Flow<List<CommunityChatMessage>> = callbackFlow {
        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Firestore not available for chat: ${e.message}")
            trySend(emptyList())
            close(e)
            return@callbackFlow
        }

        val listener = firestore.collection("community_chat")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(150)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "listenMessages error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text").orEmpty()
                        if (text.isBlank()) return@mapNotNull null
                        CommunityChatMessage(
                            id = doc.id,
                            senderUid = doc.getString("senderUid").orEmpty(),
                            senderName = doc.getString("senderName") ?: "Kullanıcı",
                            senderEmail = doc.getString("senderEmail").orEmpty(),
                            senderPhotoUrl = doc.getString("senderPhotoUrl").orEmpty(),
                            isPremium = doc.getBoolean("isPremium") == true,
                            isAdmin = doc.getBoolean("isAdmin") == true,
                            text = text,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        text: String,
        senderUid: String,
        senderName: String,
        senderEmail: String,
        senderPhotoUrl: String,
        isPremium: Boolean,
        isAdmin: Boolean
    ): Result<Unit> = runCatching {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty()) { "Mesaj boş olamaz" }
        require(cleanText.length <= 500) { "Mesaj 500 karakterden uzun olamaz" }

        val firestore = FirebaseFirestore.getInstance()
        val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val data = mapOf(
            "id" to msgId,
            "senderUid" to senderUid,
            "senderName" to senderName.ifBlank { senderEmail.substringBefore("@").ifBlank { "Kullanıcı" } },
            "senderEmail" to senderEmail,
            "senderPhotoUrl" to senderPhotoUrl,
            "isPremium" to isPremium,
            "isAdmin" to isAdmin,
            "text" to cleanText,
            "createdAt" to System.currentTimeMillis()
        )

        Tasks.await(firestore.collection("community_chat").document(msgId).set(data), 6, TimeUnit.SECONDS)
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        require(messageId.isNotBlank()) { "Geçersiz mesaj ID" }
        val firestore = FirebaseFirestore.getInstance()
        Tasks.await(firestore.collection("community_chat").document(messageId).delete(), 6, TimeUnit.SECONDS)
    }
}
