package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val isAnonymous: Boolean = false,
    val creationTimestamp: Long = System.currentTimeMillis()
)

sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(private val context: Context) {
    private val tag = "AuthManager"
    
    private val firebaseAuth: FirebaseAuth?
        get() = try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(tag, "FirebaseAuth unavailable: ${e.message}")
            null
        }

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val prefs = context.getSharedPreferences("inex_tracker_auth_prefs", Context.MODE_PRIVATE)

    init {
        checkCurrentAuth()
    }

    fun checkCurrentAuth() {
        val currentUser = try {
            firebaseAuth?.currentUser
        } catch (e: Throwable) {
            null
        }

        val hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
        val isGuestMode = prefs.getBoolean("is_guest_mode", false)

        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(
                UserProfile(
                    uid = currentUser.uid,
                    displayName = currentUser.displayName ?: "User",
                    email = currentUser.email ?: "",
                    photoUrl = currentUser.photoUrl?.toString(),
                    isAnonymous = currentUser.isAnonymous,
                    creationTimestamp = currentUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                )
            )
        } else if (hasCompletedOnboarding && isGuestMode) {
            val guestId = prefs.getString("guest_user_id", null) ?: run {
                val newId = "guest_" + UUID.randomUUID().toString().take(8)
                prefs.edit().putString("guest_user_id", newId).apply()
                newId
            }
            _authState.value = AuthState.Authenticated(
                UserProfile(
                    uid = guestId,
                    displayName = "Guest User (অফলাইন)",
                    email = "guest@inextracker.local",
                    photoUrl = null,
                    isAnonymous = true,
                    creationTimestamp = prefs.getLong("guest_created_at", System.currentTimeMillis())
                )
            )
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun continueAsGuest() {
        var guestId = prefs.getString("guest_user_id", null)
        if (guestId == null) {
            guestId = "guest_" + UUID.randomUUID().toString().take(8)
            prefs.edit()
                .putString("guest_user_id", guestId)
                .putLong("guest_created_at", System.currentTimeMillis())
                .apply()
        }
        prefs.edit()
            .putBoolean("is_guest_mode", true)
            .putBoolean("has_completed_onboarding", true)
            .apply()

        _authState.value = AuthState.Authenticated(
            UserProfile(
                uid = guestId,
                displayName = "Guest User (অফলাইন)",
                email = "guest@inextracker.local",
                photoUrl = null,
                isAnonymous = true,
                creationTimestamp = prefs.getLong("guest_created_at", System.currentTimeMillis())
            )
        )
    }

    private fun resolveWebClientId(explicitClientId: String?): String? {
        if (!explicitClientId.isNullOrBlank()) return explicitClientId
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val value = context.getString(resId)
            if (value.isNotBlank()) return value
        }
        return null
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun completeFirebaseUserLogin(
        user: FirebaseUser,
        fallbackDisplayName: String? = null,
        fallbackEmail: String? = null,
        fallbackPhotoUrl: String? = null
    ): UserProfile {
        prefs.edit()
            .putBoolean("is_guest_mode", false)
            .putBoolean("has_completed_onboarding", true)
            .apply()
        val profile = UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: fallbackDisplayName ?: "User",
            email = user.email ?: fallbackEmail ?: "",
            photoUrl = user.photoUrl?.toString() ?: fallbackPhotoUrl,
            isAnonymous = false,
            creationTimestamp = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
        _authState.value = AuthState.Authenticated(profile)
        return profile
    }

    private suspend fun signInWithGoogleOAuthFallback(
        auth: FirebaseAuth,
        activity: Activity?
    ): Result<UserProfile> {
        if (activity == null) {
            return Result.failure(Exception("Google Sign-In fallback requires an Activity context."))
        }
        return try {
            val pendingTask = auth.pendingAuthResult
            val authResult = if (pendingTask != null) {
                pendingTask.await()
            } else {
                val provider = OAuthProvider.newBuilder("google.com")
                    .setScopes(listOf("email", "profile"))
                    .addCustomParameter("prompt", "select_account")
                    .build()
                auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            val user = authResult.user
            if (user != null) {
                Result.success(completeFirebaseUserLogin(user))
            } else {
                Result.failure(Exception("Firebase user is null after Google OAuth sign-in."))
            }
        } catch (e: Exception) {
            Log.e(tag, "Google OAuth fallback failed: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context = context, webClientId: String? = null): Result<UserProfile> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase is not initialized"))
        val activity = activityContext.findActivity()
        val targetContext = activity ?: activityContext

        return try {
            val clientId = resolveWebClientId(webClientId)
                ?: return signInWithGoogleOAuthFallback(auth, activity)

            val activeCredentialManager = CredentialManager.create(targetContext)

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(clientId)
                .build()

            val signInRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result: GetCredentialResponse = try {
                activeCredentialManager.getCredential(
                    request = signInRequest,
                    context = targetContext
                )
            } catch (e: NoCredentialException) {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                activeCredentialManager.getCredential(
                    request = fallbackRequest,
                    context = targetContext
                )
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    val profile = completeFirebaseUserLogin(
                        user = user,
                        fallbackDisplayName = googleIdTokenCredential.displayName,
                        fallbackEmail = googleIdTokenCredential.id,
                        fallbackPhotoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                    Result.success(profile)
                } else {
                    Result.failure(Exception("Firebase user is null after Google authentication."))
                }
            } else {
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            val detail = e.errorMessage?.toString() ?: e.message ?: ""
            Log.w(tag, "Google Sign-In cancelled/aborted: $detail", e)
            val isExplicitUserCancel =
                detail.contains("cancelled by the user", ignoreCase = true) ||
                    detail.contains("canceled by the user", ignoreCase = true)
            if (!isExplicitUserCancel && activity != null) {
                Log.i(tag, "Falling back to Firebase Google OAuth web flow due to CredentialManager error: $detail")
                signInWithGoogleOAuthFallback(auth, activity)
            } else {
                Result.failure(Exception("সাইন-ইন বাতিল করা হয়েছে"))
            }
        } catch (e: GetCredentialException) {
            val detail = e.errorMessage?.toString() ?: e.message ?: e.type
            Log.w(tag, "Google Credential error (${e.type}): $detail — trying OAuth fallback", e)
            if (activity != null) {
                signInWithGoogleOAuthFallback(auth, activity)
            } else {
                _authState.value = AuthState.Error(detail)
                Result.failure(Exception("Google Sign-In ত্রুটি: $detail"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In failed: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            prefs.edit()
                .putBoolean("is_guest_mode", false)
                .putBoolean("has_completed_onboarding", false)
                .apply()
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            Log.e(tag, "Error signing out: ${e.message}")
            prefs.edit()
                .putBoolean("is_guest_mode", false)
                .putBoolean("has_completed_onboarding", false)
                .apply()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun getCurrentUserId(): String {
        return (try { firebaseAuth?.currentUser?.uid } catch (e: Throwable) { null })
            ?: prefs.getString("guest_user_id", null)
            ?: "guest_local_user"
    }
}
