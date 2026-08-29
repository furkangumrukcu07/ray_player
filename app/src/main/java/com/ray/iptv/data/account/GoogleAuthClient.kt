package com.ray.iptv.data.account

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Android Google Sign-In + Firebase Auth client.
 * Uses GoogleSignInOptions + Android Activity Result Launcher with fallback recovery.
 */
@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accounts: AccountRepository
) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun getGoogleSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("106605380304-i0t4dgqur6ot75omnp6v21eev96p36lc.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun extractAccountFromIntent(data: Intent?): Pair<GoogleSignInAccount?, Int?> {
        if (data == null) return Pair(null, null)
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val acc = task.getResult(ApiException::class.java)
            Pair(acc, null)
        } catch (e: ApiException) {
            Log.w("GoogleAuthClient", "GoogleSignIn ApiException code=${e.statusCode}")
            val last = GoogleSignIn.getLastSignedInAccount(context)
            if (last != null) {
                Pair(last, null)
            } else {
                Pair(null, e.statusCode)
            }
        } catch (e: Exception) {
            Log.e("GoogleAuthClient", "GoogleSignIn parse error: ${e.message}", e)
            val last = GoogleSignIn.getLastSignedInAccount(context)
            Pair(last, -1)
        }
    }

    suspend fun handleSignInResult(account: GoogleSignInAccount?): Boolean = withContext(Dispatchers.IO) {
        if (account == null) return@withContext false
        val email = account.email.orEmpty().ifBlank { "user@google.com" }
        val displayName = account.displayName.orEmpty().ifBlank { email.substringBefore("@") }
        val photoUrl = account.photoUrl?.toString().orEmpty()
        val defaultUid = account.id.orEmpty().ifBlank { "google-${email.hashCode()}" }

        // Attempt Firebase Auth sign in if idToken is available
        val idToken = account.idToken
        if (!idToken.isNullOrBlank()) {
            val fbResult = runCatching {
                val cred = GoogleAuthProvider.getCredential(idToken, null)
                val userCred = auth.signInWithCredential(cred).await()
                userCred.user?.let { u ->
                    accounts.signIn(
                        email = u.email ?: email,
                        displayName = u.displayName ?: displayName,
                        photoUrl = u.photoUrl?.toString() ?: photoUrl,
                        uid = u.uid
                    )
                    Log.d("GoogleAuthClient", "Firebase Auth successful for ${u.email}, uid=${u.uid}")
                    true
                } ?: false
            }
            if (fbResult.getOrDefault(false)) {
                return@withContext true
            } else {
                Log.w("GoogleAuthClient", "Firebase Auth signInWithCredential warning: ${fbResult.exceptionOrNull()?.message}")
            }
        }

        // Fallback to Google Account profile sign-in
        accounts.signIn(
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            uid = defaultUid
        )
        Log.d("GoogleAuthClient", "Google Account profile sign-in fallback successful for $email, uid=$defaultUid")
        return@withContext true
    }

    suspend fun signInLocal(email: String, displayName: String = "", photoUrl: String = "") {
        val clean = RayAdmin.normalize(email)
        accounts.signIn(
            email = clean,
            displayName = displayName.ifBlank { clean.substringBefore("@") },
            photoUrl = photoUrl,
            uid = "local-$clean"
        )
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        runCatching {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut().await()
        }
        runCatching { auth.signOut() }
        accounts.signOut()
    }
}
