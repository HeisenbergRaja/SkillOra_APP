package com.simats.skillora.data

data class UserProfile(
    val userId: String = "",
    val name: String = "Anonymous",
    val email: String = "",
    val profileImageUrl: String? = null,
    val dept: String = "Student",
    val college: String = "Skillora Academy",
    val credits: Int = 0,
    val totalCreditsEarned: Int = 0,
    val skillsCompleted: Int = 0,
    val skillsCreated: Int = 0,
    val skillsEnrolled: Int = 0,
    val rank: Int = 0
)
