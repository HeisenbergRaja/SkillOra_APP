package com.simats.skillora.ui.home

data class User(val name: String, val credits: Int)
data class Category(val id: String, val label: String, val icon: String)
data class ContinueLearning(val id: String, val title: String, val progress: Float)
data class Banner(val id: String, val title: String, val subtitle: String, val ctaLabel: String, val icon: String)
data class RecommendedSkill(
    val id: String,
    val imageUrl: String,
    val category: String,
    val title: String,
    val author: String,
    val rating: String,
    val credits: Int
)
data class TopLearner(val id: String, val name: String, val credits: Int, val rank: Int, val imageUrl: String)
data class RecentRequest(val id: String, val title: String, val requestedBy: String, val bountyCredits: Int, val upvotes: Int)

object HomeDummyData {
    val user = User(name = "Raja", credits = 150)

    val categories = listOf(
        Category("programming", "Programming", "code"),
        Category("ai", "Artificial Intelligence", "cpu"),
        Category("web", "Web Development", "globe"),
        Category("mobile", "Mobile Development", "smartphone"),
        Category("uiux", "UI/UX Design", "layout")
    )

    val continueLearning = listOf(
        ContinueLearning("cl-1", "Python Basics", 0.7f)
    )

    val banners = listOf(
        Banner("bn-1", "UI Sprint", "Daily design challenges", "Explore", "layout")
    )

    val recommended = listOf(
        RecommendedSkill(
            "sk-1", "https://picsum.photos/seed/skillora-flutter/800/600",
            "Mobile Development", "Flutter Mastery", "Sarah Jenkins", "4.8", 45
        ),
        RecommendedSkill(
            "sk-2", "https://picsum.photos/seed/skillora-docker/800/600",
            "DevOps", "Docker Basics", "Mike Cole", "4.6", 30
        ),
        RecommendedSkill(
            "sk-3", "https://picsum.photos/seed/skillora-react/800/600",
            "Web Development", "Advanced React Hooks", "Priya S.", "4.7", 35
        )
    )

    val topLearners = listOf(
        TopLearner("tl-1", "Lena", 1240, 2, "https://i.pravatar.cc/150?img=32"),
        TopLearner("tl-2", "Alex", 1850, 1, "https://i.pravatar.cc/150?img=12"),
        TopLearner("tl-3", "Jordan", 980, 3, "https://i.pravatar.cc/150?img=56"),
        TopLearner("tl-4", "Sam", 740, 4, "https://i.pravatar.cc/150?img=4")
    )

    val recentRequests = listOf(
        RecentRequest("rq-1", "Need Docker Basics", "Marcus Miller", 50, 12),
        RecentRequest("rq-2", "Advanced React Hooks", "Priya S.", 35, 8)
    )
}
