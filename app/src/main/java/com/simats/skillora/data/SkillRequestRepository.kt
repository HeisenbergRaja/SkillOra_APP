package com.simats.skillora.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simats.skillora.ui.marketplace.SkillRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SkillRequestRepository {
    private val TAG = "SkillRequestRepo"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val requestsCollection = firestore.collection("skillRequests")

    suspend fun submitRequest(skillName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            // Check for existing pending request by this user for the same skill
            val normalizedName = skillName.trim().lowercase()
            val existing = requestsCollection
                .whereEqualTo("requestedBy", user.uid)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            if (existing.documents.any { it.getString("title")?.trim()?.lowercase() == normalizedName }) {
                return Result.failure(Exception("You already requested this skill."))
            }

            val requestId = UUID.randomUUID().toString()
            val request = SkillRequest(
                id = requestId,
                userAvatarUrl = user.photoUrl?.toString() ?: "",
                userName = user.displayName ?: "Anonymous",
                title = skillName.trim(),
                likes = 0,
                requestedAgo = "Just now",
                requestedBy = user.uid,
                requesterEmail = user.email ?: "",
                status = "pending",
                createdAt = System.currentTimeMillis()
            )

            requestsCollection.document(requestId).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting request", e)
            Result.failure(e)
        }
    }

    fun observePendingRequests(limit: Int = 50): Flow<List<SkillRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing requests", error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(SkillRequest::class.java) ?: emptyList<SkillRequest>()
                // Sort in-memory and apply limit
                val sortedRequests = requests.sortedByDescending { it.createdAt }.take(limit)
                trySend(sortedRequests)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markRequestFulfilled(requestId: String, skillId: String, uploaderId: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId).update(
                mapOf(
                    "status" to "fulfilled",
                    "fulfilledBy" to uploaderId,
                    "fulfilledAt" to System.currentTimeMillis(),
                    "publishedSkillId" to skillId
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fulfilling request", e)
            Result.failure(e)
        }
    }
}
