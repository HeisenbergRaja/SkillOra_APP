package com.simats.skillora.data

data class DayProgress(
    val dayId: String = "",
    val completed: Boolean = false,
    val completedAt: Long? = null
)

data class Enrollment(
    val userId: String = "",
    val skillId: String = "",
    val enrolledAt: Long = System.currentTimeMillis(),
    val progress: Int = 0,
    val completedDays: Int = 0,
    val totalDays: Int = 0,
    val currentDayId: String? = null,
    val completed: Boolean = false, // FULL COMPLETION (Roadmap + Quiz)
    val roadmapCompleted: Boolean = false,
    val completedAt: Long? = null,
    val completionRewardClaimed: Boolean = false,
    val dayProgress: List<DayProgress> = emptyList(),
    val quizPassed: Boolean = false,
    val finalQuizScore: Int = 0,
    val finalQuizAttemptId: String? = null
)

data class EnrolledSkill(
    val skill: com.simats.skillora.ui.upload.Skill,
    val enrollment: Enrollment
)
