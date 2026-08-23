package com.simats.skillora.ui.marketplace

data class MarketplaceUser(val credits: Int, val avatarUrl: String)
data class MarketplaceCategory(val id: String, val label: String)

data class PopularSkill(
    val id: String,
    val imageUrl: String,
    val title: String,
    val instructor: String,
    val category: String,
    val rating: String,
    val credits: Int,
    val categoryId: String
)

data class SkillRequest(
    val id: String = "",
    val userAvatarUrl: String = "",
    val userName: String = "",
    val title: String = "",
    val likes: Int = 0,
    val requestedAgo: String = "",
    val requestedBy: String = "",
    val requesterEmail: String = "",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val fulfilledBy: String? = null,
    val fulfilledAt: Long? = null,
    val publishedSkillId: String? = null
)

object MarketplaceDummyData {
    val user = MarketplaceUser(credits = 150, avatarUrl = "https://i.pravatar.cc/150?img=12")

    val categories = listOf(
        MarketplaceCategory("all", "All"),
        MarketplaceCategory("programming", "Programming"),
        MarketplaceCategory("ai", "Artificial Intelligence"),
        MarketplaceCategory("ml", "Machine Learning"),
        MarketplaceCategory("ds", "Data Science"),
        MarketplaceCategory("web", "Web Development"),
        MarketplaceCategory("mobile", "Mobile Development"),
        MarketplaceCategory("cyber", "Cybersecurity"),
        MarketplaceCategory("cloud", "Cloud Computing"),
        MarketplaceCategory("devops", "DevOps"),
        MarketplaceCategory("db", "Database"),
        MarketplaceCategory("uiux", "UI/UX Design"),
        MarketplaceCategory("graphic", "Graphic Design"),
        MarketplaceCategory("business", "Business"),
        MarketplaceCategory("finance", "Finance"),
        MarketplaceCategory("marketing", "Marketing"),
        MarketplaceCategory("comm", "Communication"),
        MarketplaceCategory("pd", "Personal Development"),
        MarketplaceCategory("photo", "Photography"),
        MarketplaceCategory("video", "Video Editing"),
        MarketplaceCategory("music", "Music")
    )

    val popularSkills = listOf(
        PopularSkill(
            "ps-1", "https://picsum.photos/seed/marketplace-python/200/200",
            "Python Basics", "Raja", "Programming", "4.8", 20, "programming"
        ),
        PopularSkill(
            "ps-2", "https://picsum.photos/seed/marketplace-flutter/200/200",
            "Flutter Dev", "Priya", "Mobile Development", "4.5", 20, "mobile"
        ),
        PopularSkill(
            "ps-3", "https://picsum.photos/seed/marketplace-ml/200/200",
            "Machine Learning", "Arjun", "Artificial Intelligence", "4.9", 20, "ai"
        )
    )

    val skillRequests = listOf(
        SkillRequest(
            "sr-1", "https://i.pravatar.cc/150?img=56",
            "Marcus Miller", "Need Docker Basics", 12, "2d ago"
        ),
        SkillRequest(
            "sr-2", "https://i.pravatar.cc/150?img=32",
            "Lena", "Advanced React Hooks", 8, "1d ago"
        ),
        SkillRequest(
            "sr-3", "https://i.pravatar.cc/150?img=4",
            "Sam", "UI Design System Basics", 6, "5h ago"
        )
    )
}
