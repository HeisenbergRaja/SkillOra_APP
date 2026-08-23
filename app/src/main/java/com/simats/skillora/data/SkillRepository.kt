package com.simats.skillora.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.simats.skillora.ui.upload.Skill
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class SkillRepository {
    private val TAG = "SkillRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val skillsCollection = firestore.collection("skills")
    private val enrollmentsCollection = firestore.collection("enrollments")
    private val usersCollection = firestore.collection("users")
    private val creditsManager = CreditsManager()

    suspend fun publishSkill(skill: Skill): Result<Skill> {
        return try {
            Log.d(TAG, "Starting publishSkill for title: ${skill.title}")
            val user = auth.currentUser
            if (user == null) {
                Log.e(TAG, "Publish failed: User not authenticated")
                return Result.failure(Exception("Please sign in before publishing a skill."))
            }

            val skillWithCreator = skill.copy(
                creatorId = user.uid,
                creatorName = user.displayName ?: "Anonymous",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            Log.d(TAG, "Saving skill to Firestore: ${skillWithCreator.id}")
            skillsCollection.document(skillWithCreator.id).set(skillWithCreator).await()
            Log.d(TAG, "Firestore save successful")
            
            // Reward skill publication
            creditsManager.rewardSkillPublication(user.uid, skillWithCreator.id, skillWithCreator.title)
            
            Result.success(skillWithCreator)
        } catch (e: Exception) {
            Log.e(TAG, "Error in publishSkill", e)
            Result.failure(e)
        }
    }

    suspend fun getPublishedSkills(): Result<List<Skill>> {
        return try {
            Log.d(TAG, "Fetching published skills...")
            val snapshot = skillsCollection
                .whereEqualTo("status", "published")
                .get()
                .await()

            val skills = snapshot.toObjects(Skill::class.java)
            val sortedSkills = skills.sortedByDescending { it.createdAt }
            
            Log.d(TAG, "Successfully fetched and sorted ${sortedSkills.size} skills")
            Result.success(sortedSkills)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching published skills", e)
            Result.failure(e)
        }
    }

    suspend fun getSkillById(skillId: String): Result<Skill> {
        return try {
            val snapshot = skillsCollection.document(skillId).get().await()
            val skill = snapshot.toObject(Skill::class.java)
            if (skill != null) {
                Result.success(skill)
            } else {
                Result.failure(Exception("Skill not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEnrollment(skillId: String): Result<Enrollment?> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val snapshot = enrollmentsCollection.document("${user.uid}_$skillId").get().await()
            Result.success(snapshot.toObject(Enrollment::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enrollInSkill(skillId: String, credits: Int): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            val skillSnapshot = skillsCollection.document(skillId).get().await()
            val skill = skillSnapshot.toObject(Skill::class.java) ?: throw Exception("Skill not found")
            
            if (skill.creatorId == user.uid) {
                Log.d("Enrollment", "Enrollment blocked: user is skill creator. User: ${user.uid}, Skill: $skillId")
                return Result.failure(Exception("You cannot enroll in a skill you created."))
            }

            Log.d("Enrollment", "User is not creator, continuing enrollment. User: ${user.uid}, Skill: $skillId")
            
            // Spend credits for enrollment
            val spendResult = creditsManager.spendForEnrollment(user.uid, skillId, skill.title, credits)
            if (spendResult is CreditResult.InsufficientCredits) {
                return Result.failure(Exception("Insufficient credits. Required: $credits"))
            } else if (spendResult is CreditResult.Error) {
                return Result.failure(Exception(spendResult.message))
            }

            val totalDays = skill.roadmap.size
            val enrollment = Enrollment(
                userId = user.uid,
                skillId = skillId,
                enrolledAt = System.currentTimeMillis(),
                totalDays = totalDays,
                dayProgress = skill.roadmap.map { DayProgress(dayId = it.id) }
            )
            val enrollmentDoc = enrollmentsCollection.document("${user.uid}_$skillId")
            enrollmentDoc.set(enrollment).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgress(skillId: String, dayId: String): Result<Enrollment> {
        return try {
            Log.d("Progress", "Updating progress for skill: $skillId, day: $dayId")
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val enrollmentDoc = firestore.collection("enrollments").document("${user.uid}_$skillId")
            val skillDoc = skillsCollection.document(skillId)
            val userDoc = firestore.collection("users").document(user.uid)
            
            val result = firestore.runTransaction { transaction ->
                // READS FIRST
                val enrollmentSnapshot = transaction.get(enrollmentDoc)
                val skillSnapshot = transaction.get(skillDoc)
                val userSnapshot = transaction.get(userDoc)
                
                val enrollment = enrollmentSnapshot.toObject(Enrollment::class.java) ?: throw Exception("Enrollment not found")
                val skill = skillSnapshot.toObject(Skill::class.java) ?: throw Exception("Skill not found")
                
                // Synchronize dayProgress if missing or if roadmap changed
                var currentDayProgress = enrollment.dayProgress.toMutableList()
                if (currentDayProgress.isEmpty()) {
                    currentDayProgress = skill.roadmap.map { DayProgress(dayId = it.id) }.toMutableList()
                }
                
                var dayIndex = currentDayProgress.indexOfFirst { it.dayId == dayId }
                if (dayIndex == -1) {
                    if (skill.roadmap.any { it.id == dayId }) {
                        currentDayProgress.add(DayProgress(dayId = dayId))
                        dayIndex = currentDayProgress.size - 1
                    } else {
                        throw Exception("Day not found in roadmap")
                    }
                }
                
                val day = currentDayProgress[dayIndex]
                if (day.completed) return@runTransaction enrollment
                
                currentDayProgress[dayIndex] = day.copy(completed = true, completedAt = System.currentTimeMillis())
                
                val totalDaysCount = skill.roadmap.size
                val newCompletedDays = currentDayProgress.count { it.completed }
                val newProgress = if (totalDaysCount > 0) (newCompletedDays * 100) / totalDaysCount else 0
                val isRoadmapCompleted = newCompletedDays == totalDaysCount
                
                val updatedEnrollment = enrollment.copy(
                    dayProgress = currentDayProgress,
                    completedDays = newCompletedDays,
                    totalDays = totalDaysCount,
                    progress = newProgress,
                    roadmapCompleted = isRoadmapCompleted
                )
                
                // WRITES AFTER ALL READS
                transaction.set(enrollmentDoc, updatedEnrollment)
                
                updatedEnrollment
            }.await()
            
            Log.d("Progress", "Progress saved successfully: ${result.progress}%")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("Progress", "Failed to save progress", e)
            Result.failure(e)
        }
    }

    suspend fun saveQuiz(skillId: String, quiz: FinalQuiz): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val skill = getSkillById(skillId).getOrNull() ?: return Result.failure(Exception("Skill not found"))
            
            if (skill.creatorId != user.uid) {
                return Result.failure(Exception("Only the creator can modify the quiz."))
            }

            val quizToSave = quiz.copy(
                createdBy = user.uid,
                updatedAt = System.currentTimeMillis()
            )
            
            skillsCollection.document(skillId).update("finalQuiz", quizToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitQuizAttempt(skillId: String, attempt: QuizAttempt): Result<QuizAttempt> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val enrollmentDoc = enrollmentsCollection.document("${user.uid}_$skillId")
            val skillDoc = skillsCollection.document(skillId)
            val userDoc = usersCollection.document(user.uid)
            
            val result = firestore.runTransaction { transaction ->
                val enrollmentSnapshot = transaction.get(enrollmentDoc)
                val skillSnapshot = transaction.get(skillDoc)
                val userSnapshot = transaction.get(userDoc)
                
                val enrollment = enrollmentSnapshot.toObject(Enrollment::class.java) ?: throw Exception("Enrollment not found")
                val skill = skillSnapshot.toObject(Skill::class.java) ?: throw Exception("Skill not found")
                
                if (!enrollment.roadmapCompleted) {
                    throw Exception("Complete the roadmap before taking the quiz.")
                }

                val attemptId = UUID.randomUUID().toString()
                val finalAttempt = attempt.copy(
                    attemptId = attemptId,
                    userId = user.uid,
                    skillId = skillId,
                    attemptedAt = System.currentTimeMillis()
                )
                
                // Save attempt in subcollection
                val attemptDoc = enrollmentDoc.collection("quizAttempts").document(attemptId)
                transaction.set(attemptDoc, finalAttempt)
                
                // Update enrollment if passed
                if (finalAttempt.passed) {
                    val wasAlreadyCompleted = enrollment.completed
                    val updatedEnrollment = enrollment.copy(
                        quizPassed = true,
                        finalQuizScore = finalAttempt.scorePercentage,
                        finalQuizAttemptId = attemptId,
                        completed = true,
                        completedAt = enrollment.completedAt ?: System.currentTimeMillis(),
                        progress = 100 // Ensure 100% progress on completion
                    )
                    transaction.set(enrollmentDoc, updatedEnrollment)
                    
                    if (!wasAlreadyCompleted) {
                        val currentSkillsCompleted = userSnapshot.getLong("skillsCompleted")?.toInt() ?: 0
                        transaction.update(userDoc, "skillsCompleted", currentSkillsCompleted + 1)
                    }
                }
                
                finalAttempt
            }.await()
            
            // Reward skill completion if passed and not claimed
            if (result.passed) {
                val enrollmentSnapshot = enrollmentDoc.get().await()
                val completionRewardClaimed = enrollmentSnapshot.getBoolean("completionRewardClaimed") ?: false
                if (!completionRewardClaimed) {
                    val skillSnapshot = skillDoc.get().await()
                    val skill = skillSnapshot.toObject(Skill::class.java)
                    if (skill != null) {
                        creditsManager.rewardSkillCompletion(user.uid, skillId, skill.title, skill.completionCredits)
                        enrollmentDoc.update("completionRewardClaimed", true).await()
                    }
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEnrolledSkillsWithProgress(): Result<List<EnrolledSkill>> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val enrollmentSnapshot = enrollmentsCollection
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
            
            val enrolledSkills = mutableListOf<EnrolledSkill>()
            for (doc in enrollmentSnapshot.documents) {
                val enrollment = doc.toObject(Enrollment::class.java) ?: continue
                val skillDoc = skillsCollection.document(enrollment.skillId).get().await()
                val skill = skillDoc.toObject(Skill::class.java) ?: continue
                enrolledSkills.add(EnrolledSkill(skill, enrollment))
            }
            
            Result.success(enrolledSkills)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val skillsCreatedSnapshot = skillsCollection.whereEqualTo("creatorId", userId).get().await()
            val enrollmentsSnapshot = enrollmentsCollection.whereEqualTo("userId", userId).get().await()
            
            val totalEarned = userDoc.getLong("totalCreditsEarned")?.toInt() ?: 0
            val skillsCompleted = userDoc.getLong("skillsCompleted")?.toInt() ?: 0
            
            Result.success(UserProfile(
                userId = userId,
                name = userDoc.getString("name") ?: "Anonymous",
                email = userDoc.getString("email") ?: "",
                profileImageUrl = userDoc.getString("profileImageUrl"),
                dept = userDoc.getString("dept") ?: "Student",
                college = userDoc.getString("college") ?: "Skillora Academy",
                credits = userDoc.getLong("credits")?.toInt() ?: 0,
                totalCreditsEarned = totalEarned,
                skillsCompleted = skillsCompleted,
                skillsCreated = skillsCreatedSnapshot.size(),
                skillsEnrolled = enrollmentsSnapshot.size()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCreatedSkills(userId: String): Result<List<Skill>> {
        return try {
            val snapshot = skillsCollection
                .whereEqualTo("creatorId", userId)
                .whereEqualTo("status", "published")
                .get()
                .await()
            Result.success(snapshot.toObjects(Skill::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeEnrollment(skillId: String): Flow<Enrollment?> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = enrollmentsCollection.document("${userId}_$skillId").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Enrollment::class.java))
        }
        awaitClose { listener.remove() }
    }
    
    fun observeCredits() = creditsManager.observeCredits()
    
    fun observeTotalCreditsEarned() = creditsManager.observeTotalCreditsEarned()

    fun observeUserProfile(userId: String): Flow<UserProfile?> = kotlinx.coroutines.flow.callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val totalEarned = snapshot.getLong("totalCreditsEarned")?.toInt() ?: 0
                val skillsCompleted = snapshot.getLong("skillsCompleted")?.toInt() ?: 0
                
                // We'll still need to fetch counts for Created and Enrolled if they aren't stored in the user doc
                // For simplicity in this reactive Flow, we could store these counts in the user doc too.
                // But for now, let's just emit what we have in the doc.
                trySend(UserProfile(
                    userId = userId,
                    name = snapshot.getString("name") ?: "Anonymous",
                    email = snapshot.getString("email") ?: "",
                    profileImageUrl = snapshot.getString("profileImageUrl"),
                    dept = snapshot.getString("dept") ?: "Student",
                    college = snapshot.getString("college") ?: "Skillora Academy",
                    credits = snapshot.getLong("credits")?.toInt() ?: 0,
                    totalCreditsEarned = totalEarned,
                    skillsCompleted = skillsCompleted,
                    // These will be 0 here, we'll supplement them if needed
                    skillsCreated = snapshot.getLong("skillsCreated")?.toInt() ?: 0,
                    skillsEnrolled = snapshot.getLong("skillsEnrolled")?.toInt() ?: 0
                ))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }
}
