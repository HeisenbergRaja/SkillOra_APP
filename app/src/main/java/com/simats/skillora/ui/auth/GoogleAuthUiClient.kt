package com.simats.skillora.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "SkilloraAuth"

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    suspend fun signIn(): Result<Boolean> {
        val activity = context.findActivity() ?: return Result.failure(Exception("Activity context not found"))

        // Web Client ID from google-services.json
        val webClientId = "465133917467-pku5vl69h9t6bei4b1l6g5n2klrd9ois.apps.googleusercontent.com"
        
        Log.d(TAG, "Attempting Google Sign-In with Web Client ID: $webClientId")

        val credentialManager = CredentialManager.create(activity)

        // Using both options to ensure maximum compatibility
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            handleCredential(result.credential)
        } catch (e: NoCredentialException) {
            Log.e(TAG, "NoCredentialException: Request was rejected. CHECK: 1. SHA-1 registration in Firebase Console. 2. Google Sign-In enabled in Auth section. 3. Physical device with Google account.", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException [${e.type}]: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun handleCredential(credential: androidx.credentials.Credential): Result<Boolean> {
        return try {
            when (credential.type) {
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    signInToFirebase(googleIdTokenCredential.idToken)
                }
                else -> {
                    // Fallback for custom credential types if necessary
                    val idToken = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                        ?: credential.data.getString("idToken") // Try common keys
                    
                    if (idToken != null) {
                        signInToFirebase(idToken)
                    } else {
                        Log.e(TAG, "Received unknown credential type: ${credential.type}")
                        Result.failure(Exception("Unknown credential type: ${credential.type}"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing credential", e)
            Result.failure(e)
        }
    }

    private suspend fun signInToFirebase(idToken: String): Result<Boolean> {
        Log.d(TAG, "Signing into Firebase with ID Token...")
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
        Log.d(TAG, "Firebase Sign-In successful for user: ${authResult.user?.uid}")
        
        // Initialize credits for new user
        authResult.user?.uid?.let { uid ->
            com.simats.skillora.data.CreditsManager().initializeCreditsIfNew(uid)
        }

        return Result.success(true)
    }

    fun getSignedInUser(): UserInfo? = firebaseAuth.currentUser?.run {
        UserInfo(
            userId = uid,
            userName = displayName,
            email = email,
            profilePictureUrl = photoUrl?.toString()
        )
    }

    suspend fun signOut() {
        try {
            val activity = context.findActivity()
            if (activity != null) {
                CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
            }
            firebaseAuth.signOut()
            Log.d(TAG, "Signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
        }
    }
}

data class UserInfo(
    val userId: String,
    val userName: String?,
    val email: String?,
    val profilePictureUrl: String?
)
