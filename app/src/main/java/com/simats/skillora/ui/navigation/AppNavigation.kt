package com.simats.skillora.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.simats.skillora.ui.auth.*
import com.simats.skillora.ui.home.HomeScreen
import com.simats.skillora.ui.marketplace.*
import com.simats.skillora.ui.learning.MyLearningScreen
import com.simats.skillora.ui.leaderboard.LeaderboardScreen
import com.simats.skillora.ui.profile.ProfileScreen
import com.simats.skillora.ui.skill.SkillDetailsScreen
import com.simats.skillora.ui.quiz.QuizScreen
import com.simats.skillora.ui.chat.CreatorChatScreen
import com.simats.skillora.ui.chat.PublisherChatListScreen
import com.simats.skillora.ui.upload.*
import com.simats.skillora.ui.splash.SplashScreen
import com.simats.skillora.ui.components.PlaceholderScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuthUiClient = remember {
        GoogleAuthUiClient(context = context)
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onTimeout = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            })
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoogleSignIn = {
                    val result = googleAuthUiClient.signIn()
                    if (result.isSuccess) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                    result
                },
                onNavigateToRegister = {
                    navController.navigate("register") {
                        launchSingleTop = true
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate("forgot_password")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }
        composable("home") {
            HomeScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("marketplace") {
            MarketplaceScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSkillDetails = { skillId ->
                    navController.navigate("skill_details?id=$skillId")
                },
                onNavigateToUploadSkill = { req ->
                    if (req != null) {
                        val encodedTitle = android.net.Uri.encode(req.title)
                        navController.navigate("upload_skill?title=$encodedTitle&id=${req.id}")
                    } else {
                        navController.navigate("upload_skill")
                    }
                },
                onNavigateToSkillRequestDetails = { request ->
                    navController.navigate("request_details")
                }
            )
        }
        composable("learning") {
            MyLearningScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onContinueLesson = { skillId ->
                    navController.navigate("enrolled_skill?id=$skillId")
                }
            )
        }
        composable("leaders") {
            LeaderboardScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    scope.launch {
                        googleAuthUiClient.signOut()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
        composable("skill_request") {
            SkillRequestScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("publisher_chats") {
            PublisherChatListScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(
            route = "skill_details?id={id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            SkillDetailsScreen(
                skillId = id,
                onBack = { navController.popBackStack() },
                onEnrollSuccess = { alreadyEnrolled ->
                    if (alreadyEnrolled) {
                        navController.navigate("enrolled_skill?id=$id")
                    } else {
                        navController.navigate("learning") {
                            popUpTo("marketplace")
                        }
                    }
                }
            )
        }
        composable("request_details") {
            SkillRequestDetailsScreen(
                request = MarketplaceDummyData.skillRequests[0],
                onBack = { navController.popBackStack() },
                onUploadSkill = { req ->
                    val encodedTitle = android.net.Uri.encode(req.title)
                    navController.navigate("upload_skill?title=$encodedTitle&id=${req.id}")
                }
            )
        }
        composable(
            route = "upload_skill?title={title}&id={id}",
            arguments = listOf(
                navArgument("title") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("id") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val requestedTitle = backStackEntry.arguments?.getString("title")
            val requestId = backStackEntry.arguments?.getString("id")
            
            UploadSkillScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
                requestedTitle = requestedTitle,
                requestId = requestId
            )
        }
        composable("upload_success") {
            UploadSuccessScreen(onBackToMarketplace = {
                navController.navigate("marketplace") {
                    popUpTo("marketplace") { inclusive = true }
                }
            })
        }
        composable(
            route = "enrolled_skill?id={id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            com.simats.skillora.ui.enrolled.EnrolledSkillScreen(
                skillId = id,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(
            route = "quiz_view?skillId={skillId}",
            arguments = listOf(
                navArgument("skillId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
            QuizScreen(
                skillId = skillId,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(
            route = "chat?skillId={skillId}&studentId={studentId}",
            arguments = listOf(
                navArgument("skillId") { type = NavType.StringType },
                navArgument("studentId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
            val studentId = backStackEntry.arguments?.getString("studentId")
            CreatorChatScreen(
                skillId = skillId,
                studentId = studentId,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("enrollment") {
            PlaceholderScreen(title = "Enrollment", message = "Enrollment flow will be implemented here.", icon = Icons.Default.Inventory2 as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
        composable("lesson_player") {
            PlaceholderScreen(title = "Lesson Player", message = "Video player will be implemented here.", icon = Icons.Default.PlayCircle as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
        composable("pdf_viewer") {
            PlaceholderScreen(title = "PDF Viewer", message = "PDF Document will be displayed here.", icon = Icons.Default.Description as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
        composable("drive_link") {
            PlaceholderScreen(title = "Drive Link", message = "External resource will open here.", icon = Icons.Default.Cloud as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
        composable("quiz_result") {
            PlaceholderScreen(title = "Quiz Result", message = "Your score and rewards will appear here.", icon = Icons.Default.EmojiEvents as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
        composable("ai_chat") {
            PlaceholderScreen(title = "AI Chat", message = "AI Assistant will help you here.", icon = Icons.Default.Chat as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
        composable("filters") {
            PlaceholderScreen(title = "Filters", message = "Marketplace filters will be here.", icon = Icons.Default.Settings as androidx.compose.ui.graphics.vector.ImageVector, onBack = { navController.popBackStack() })
        }
    }
}
