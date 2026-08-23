package com.simats.skillora.ui.upload

import com.google.firebase.firestore.Exclude
import java.util.UUID

data class ResourceLink(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val url: String = ""
)

data class RoadmapDay(
    val id: String = UUID.randomUUID().toString(),
    val dayNumber: Int = 0,
    val title: String = "",
    val description: String = "",
    val fileResources: List<ResourceLink> = emptyList(),
    val videoResources: List<ResourceLink> = emptyList(),
    @get:Exclude val isExpanded: Boolean = false
)

data class Skill(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val whatYoullLearn: String = "",
    val category: String = "",
    val creditsRequired: Int = 20,
    val completionCredits: Int = 20,
    val creatorId: String = "",
    val creatorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "published",
    val requestedSkillId: String? = null,
    val roadmap: List<RoadmapDay> = emptyList(),
    val finalQuiz: com.simats.skillora.data.FinalQuiz? = null
)
