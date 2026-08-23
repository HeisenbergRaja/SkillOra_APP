package com.simats.skillora.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class LeaderboardUser(
    val userId: String = "",
    val displayName: String = "Anonymous",
    val profileImageUrl: String? = null,
    val totalCreditsEarned: Int = 0,
    val skillsCompleted: Int = 0,
    val rank: Int = 0,
    val dept: String = "",
    val isCurrent: Boolean = false
)

class LeaderboardRepository {
    private val TAG = "LeaderboardRepo"
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getLeaderboard(sortBy: String, currentUserId: String?): Result<List<LeaderboardUser>> {
        return try {
            val field = if (sortBy == "credits") "totalCreditsEarned" else "skillsCompleted"
            
            val snapshot = usersCollection
                .orderBy(field, Query.Direction.DESCENDING)
                .limit(50) 
                .get()
                .await()

            val users = mutableListOf<LeaderboardUser>()
            var currentRank = 1
            var prevValue = -1
            var usersAtSameRank = 0

            snapshot.documents.forEach { doc ->
                val userId = doc.id
                val name = doc.getString("name") ?: "Anonymous"
                val avatar = doc.getString("profileImageUrl")
                val credits = doc.getLong("totalCreditsEarned")?.toInt() ?: 0
                val skills = doc.getLong("skillsCompleted")?.toInt() ?: 0
                val dept = doc.getString("dept") ?: ""

                val currentValue = if (sortBy == "credits") credits else skills
                
                if (currentValue != prevValue) {
                    currentRank += usersAtSameRank
                    usersAtSameRank = 1
                } else {
                    usersAtSameRank++
                }
                prevValue = currentValue

                users.add(
                    LeaderboardUser(
                        userId = userId,
                        displayName = name,
                        profileImageUrl = avatar,
                        totalCreditsEarned = credits,
                        skillsCompleted = skills,
                        rank = currentRank,
                        dept = dept,
                        isCurrent = userId == currentUserId
                    )
                )
            }
            
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leaderboard", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRank(userId: String, sortBy: String): Int {
        return try {
            val field = if (sortBy == "credits") "totalCreditsEarned" else "skillsCompleted"
            val snapshot = usersCollection.orderBy(field, Query.Direction.DESCENDING).get().await()
            
            var rank = 1
            var prevValue = -1
            var usersAtSameRank = 0

            for (doc in snapshot.documents) {
                val valInDoc = doc.getLong(field)?.toInt() ?: 0
                if (valInDoc != prevValue) {
                    rank += usersAtSameRank
                    usersAtSameRank = 1
                } else {
                    usersAtSameRank++
                }
                prevValue = valInDoc
                
                if (doc.id == userId) return rank
            }
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user rank", e)
            0
        }
    }
}
