package com.simats.skillora.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class CreditTransactionType {
    INITIAL_BONUS,
    ENROLLMENT,
    SKILL_COMPLETION,
    SKILL_PUBLISHED,
    REFUND,
    ADMIN_ADJUSTMENT
}

data class CreditTransaction(
    val id: String = "",
    val userId: String = "",
    val type: CreditTransactionType = CreditTransactionType.INITIAL_BONUS,
    val amount: Int = 0,
    val description: String = "",
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object CreditConfig {
    const val INITIAL_CREDITS = 120
    const val DEFAULT_SKILL_COMPLETION_REWARD = 20
    const val DEFAULT_SKILL_PUBLICATION_REWARD = 20
}

sealed class CreditResult {
    object Success : CreditResult()
    object InsufficientCredits : CreditResult()
    data class Error(val message: String) : CreditResult()
}

class CreditsManager {
    private val TAG = "CreditsManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val usersCollection = firestore.collection("users")
    private val transactionsCollection = firestore.collection("creditTransactions")

    fun observeCredits(): Flow<Int> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing credits", error)
                return@addSnapshotListener
            }
            val credits = snapshot?.getLong("credits")?.toInt() ?: 0
            trySend(credits)
        }

        awaitClose { listener.remove() }
    }

    fun observeTotalCreditsEarned(): Flow<Int> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing total earned credits", error)
                return@addSnapshotListener
            }
            val earned = snapshot?.getLong("totalCreditsEarned")?.toInt() ?: 0
            trySend(earned)
        }

        awaitClose { listener.remove() }
    }

    suspend fun getCurrentCredits(): Int {
        val userId = auth.currentUser?.uid ?: return 0
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.getLong("credits")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current credits", e)
            0
        }
    }

    suspend fun initializeCreditsIfNew(userId: String): CreditResult {
        return try {
            val user = auth.currentUser
            val userDoc = usersCollection.document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                
                val updates = mutableMapOf<String, Any>()
                
                if (!snapshot.exists() || snapshot.getBoolean("creditsInitialized") != true) {
                    updates["credits"] = CreditConfig.INITIAL_CREDITS
                    updates["totalCreditsEarned"] = 0
                    updates["skillsCompleted"] = 0
                    updates["creditsInitialized"] = true
                    updates["userId"] = userId
                    updates["createdAt"] = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                    
                    val transactionRecord = CreditTransaction(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        type = CreditTransactionType.INITIAL_BONUS,
                        amount = CreditConfig.INITIAL_CREDITS,
                        description = "Welcome bonus",
                        referenceId = "${userId}_INITIAL_BONUS"
                    )
                    transaction.set(transactionsCollection.document(transactionRecord.id), transactionRecord)
                }
                
                // Ensure name and email are always up to date from Auth
                if (user != null) {
                    updates["name"] = user.displayName ?: snapshot.getString("name") ?: "Anonymous"
                    updates["email"] = user.email ?: snapshot.getString("email") ?: ""
                    if (user.photoUrl != null) {
                        updates["profileImageUrl"] = user.photoUrl.toString()
                    }
                }
                
                updates["updatedAt"] = System.currentTimeMillis()
                transaction.set(userDoc, updates, com.google.firebase.firestore.SetOptions.merge())
                
                CreditResult.Success
            }.await() as CreditResult
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing credits", e)
            CreditResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun rewardSkillPublication(userId: String, skillId: String, skillTitle: String): CreditResult {
        val referenceId = "${skillId}_PUBLICATION"
        return addCredits(
            userId = userId,
            amount = CreditConfig.DEFAULT_SKILL_PUBLICATION_REWARD,
            type = CreditTransactionType.SKILL_PUBLISHED,
            description = "Published skill: $skillTitle",
            referenceId = referenceId
        )
    }

    suspend fun rewardSkillCompletion(userId: String, skillId: String, skillTitle: String, amount: Int? = null): CreditResult {
        val referenceId = "${skillId}_COMPLETION"
        return addCredits(
            userId = userId,
            amount = amount ?: CreditConfig.DEFAULT_SKILL_COMPLETION_REWARD,
            type = CreditTransactionType.SKILL_COMPLETION,
            description = "Completed skill: $skillTitle",
            referenceId = referenceId
        )
    }

    suspend fun spendForEnrollment(userId: String, skillId: String, skillTitle: String, amount: Int): CreditResult {
        val referenceId = "${skillId}_ENROLLMENT_$userId"
        return spendCredits(
            userId = userId,
            amount = amount,
            type = CreditTransactionType.ENROLLMENT,
            description = "Enrolled in skill: $skillTitle",
            referenceId = referenceId
        )
    }

    private suspend fun addCredits(
        userId: String,
        amount: Int,
        type: CreditTransactionType,
        description: String,
        referenceId: String? = null
    ): CreditResult {
        if (amount <= 0) return CreditResult.Error("Invalid amount")
        
        return try {
            firestore.runTransaction { transaction ->
                val userDoc = usersCollection.document(userId)
                val snapshot = transaction.get(userDoc)
                
                val currentCredits = snapshot.getLong("credits")?.toInt() ?: 0
                val currentEarned = snapshot.getLong("totalCreditsEarned")?.toInt() ?: 0
                
                val isQualifyingEarning = type == CreditTransactionType.SKILL_COMPLETION || 
                                        type == CreditTransactionType.SKILL_PUBLISHED
                
                val updates = mutableMapOf<String, Any>(
                    "credits" to currentCredits + amount,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                if (isQualifyingEarning) {
                    updates["totalCreditsEarned"] = currentEarned + amount
                }
                
                transaction.update(userDoc, updates)

                val transactionRecord = CreditTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = type,
                    amount = amount,
                    description = description,
                    referenceId = referenceId
                )
                transaction.set(transactionsCollection.document(transactionRecord.id), transactionRecord)
                
                CreditResult.Success
            }.await() as CreditResult
        } catch (e: Exception) {
            Log.e(TAG, "Error adding credits", e)
            CreditResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun spendCredits(
        userId: String,
        amount: Int,
        type: CreditTransactionType,
        description: String,
        referenceId: String? = null
    ): CreditResult {
        if (amount <= 0) return CreditResult.Error("Invalid amount")

        return try {
            firestore.runTransaction { transaction ->
                val userDoc = usersCollection.document(userId)
                val snapshot = transaction.get(userDoc)
                val currentCredits = snapshot.getLong("credits")?.toInt() ?: 0

                if (currentCredits < amount) {
                    return@runTransaction CreditResult.InsufficientCredits
                }

                transaction.update(userDoc, "credits", currentCredits - amount)
                transaction.update(userDoc, "updatedAt", System.currentTimeMillis())

                val transactionRecord = CreditTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = type,
                    amount = -amount,
                    description = description,
                    referenceId = referenceId
                )
                transaction.set(transactionsCollection.document(transactionRecord.id), transactionRecord)
                
                CreditResult.Success
            }.await() as CreditResult
        } catch (e: Exception) {
            Log.e(TAG, "Error spending credits", e)
            CreditResult.Error(e.message ?: "Unknown error")
        }
    }
}
