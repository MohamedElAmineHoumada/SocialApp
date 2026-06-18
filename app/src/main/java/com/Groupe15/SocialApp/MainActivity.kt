package com.Groupe15.SocialApp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.Groupe15.SocialApp.ui.theme.SocialAppTheme
import com.Groupe15.SocialApp.ui.auth.*
import com.Groupe15.SocialApp.ui.feed.*
import com.Groupe15.SocialApp.ui.profile.*
import com.Groupe15.SocialApp.ui.messages.*
import com.Groupe15.SocialApp.ui.network.*
import com.Groupe15.SocialApp.ui.discover.*
import com.Groupe15.SocialApp.ui.post.*
import com.Groupe15.SocialApp.ui.story.*
import dagger.hilt.android.AndroidEntryPoint
import com.Groupe15.SocialApp.viewmodel.*
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        
        setContent {
            SocialAppTheme(darkTheme = isDarkMode) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val noBottomBarRoutes = listOf(
        "login", "register", "forgotPassword", "onboardingWelcome",
        "onboardingDob", "onboardingGender", "onboardingInterests",
        "chat/{chatId}/{userName}", "createPost", "createStory", "storyViewer",
        "editProfile", "settings"
    )

    val showBottomNav = currentDestination?.route !in noBottomBarRoutes && 
                       currentDestination?.route?.contains("chat/") == false

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                CustomBottomNavigation(navController, currentDestination)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(padding)
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = hiltViewModel(),
                    onLoginClick = { e, p -> /* logic in screen */ },
                    onGoogleClick = { /* logic in fragment/activity */ },
                    onFacebookClick = { /* logic in fragment/activity */ },
                    onRegisterClick = { navController.navigate("register") },
                    onForgotPasswordClick = { navController.navigate("forgotPassword") },
                    onSuccess = { navController.navigate("feed") { popUpTo("login") { inclusive = true } } }
                )
            }
            composable("forgotPassword") {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable("register") {
                RegisterScreen(
                    viewModel = hiltViewModel<RegisterViewModel>(),
                    onRegisterClick = { e: String, p: String, u: String -> /* logic in screen */ },
                    onLoginClick = { navController.popBackStack() },
                    onSuccess = { navController.navigate("onboardingWelcome") }
                )
            }
            composable("onboardingWelcome") {
                OnboardingWelcomeScreen(
                    onStart = { navController.navigate("onboardingDob") }
                )
            }
            composable("onboardingDob") {
                OnboardingDobScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("onboardingGender") }
                )
            }
            composable("onboardingGender") {
                OnboardingGenderScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("onboardingInterests") }
                )
            }
            composable("onboardingInterests") {
                OnboardingInterestsScreen(
                    onBack = { navController.popBackStack() },
                    onFinish = { 
                        navController.navigate("feed") {
                            popUpTo("onboardingWelcome") { inclusive = true }
                        }
                    }
                )
            }
            composable("feed") {
                FeedScreen(
                    viewModel = hiltViewModel<FeedViewModel>(),                    onNavigateToDiscover = { navController.navigate("discover") },
                    onNavigateToProfile = { uid: String -> navController.navigate("profile/$uid") },
                    onCommentClick = { /* logic for bottom sheet */ },
                    onShareClick = { /* logic for bottom sheet */ }
                )
            }
            composable("messages") {
                MessagesScreen(
                    viewModel = hiltViewModel(),
                    onConversationClick = { conv -> navController.navigate("chat/${conv.userId}/${conv.username}") }
                )
            }
            composable("network") {
                NetworkScreen(onBack = { navController.popBackStack() })
            }
            composable(
                "profile/{uid}",
                arguments = listOf(navArgument("uid") { defaultValue = "" })
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: ""
                ProfileScreen(
                    viewModel = hiltViewModel<ProfileViewModel>(),
                    followViewModel = hiltViewModel<FollowViewModel>(),
                    targetUid = uid,
                    onEditProfile = { navController.navigate("editProfile") },
                    onSettings = { navController.navigate("settings") },
                    onMessage = { name: String -> navController.navigate("chat/$uid/$name") },
                    onNavigateToProfile = { otherUid: String -> navController.navigate("profile/$otherUid") } // ✅ NOUVEAU
                )
            }
            composable("discover") {
                DiscoverScreen(
                    onNavigateToProfile = { uid -> navController.navigate("profile/$uid") },
                    onNavigateToNotifications = { /* TODO: Implement Notifications Route */ }
                )
            }
            composable("createPost") {
                CreatePostScreen(
                    viewModel = hiltViewModel<CreatePostViewModel>(),
                    onBack = { navController.popBackStack() },
                    onPickImages = { /* Managed internally in Screen */ },
                    onShowAudiencePicker = { /* TODO: Implement Compose BottomSheet */ }
                )
            }
            composable("editProfile") {
                EditProfileScreen(
                    viewModel = hiltViewModel<EditProfileViewModel>(),
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() },
                    onError = { /* Handle error toast */ }
                )
            }
            composable("settings") {
                // Note: SettingsScreen and its ViewModel need to be implemented
                // For now, using AuthViewModel as a placeholder if applicable or specify the intended type
                SettingsScreen(
                    viewModel = hiltViewModel<AuthViewModel>(),
                    onBack = { navController.popBackStack() },
                    onShowToast = { /* Handle toast */ },
                    onAccountDeleted = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
                    onLoggedOut = { navController.navigate("login") { popUpTo(0) { inclusive = true } } }

                )
            }
            composable(
                "chat/{chatId}/{userName}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: "Chat"
                ChatScreen(
                    viewModel = hiltViewModel(),
                    currentUserId = "USER_ID", // TODO: Get actual UID
                    userName = userName,
                    onBack = { navController.popBackStack() },
                    onShowToast = { }
                )
            }
            // Other routes...
        }
    }
}

@Composable
fun CustomBottomNavigation(navController: NavController, currentDestination: NavDestination?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

            BottomNavItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_app_logo,
                label = stringResource(R.string.nav_home),
                selected = currentDestination?.hierarchy?.any { it.route == "feed" } == true,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { navController.navigate("feed") }
            )

            BottomNavItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_comment,
                label = stringResource(R.string.nav_messages),
                selected = currentDestination?.hierarchy?.any { it.route == "messages" } == true,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { navController.navigate("messages") }
            )

            // Studio
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clickable { navController.navigate("createPost") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF6C47FF), Color(0xFF9D47FF))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_circle),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.nav_studio),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentDestination?.route == "createPost") activeColor else inactiveColor
                    )
                }
            }

            BottomNavItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_people,
                label = stringResource(R.string.nav_network),
                selected = currentDestination?.hierarchy?.any { it.route == "network" } == true,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { navController.navigate("network") }
            )

            BottomNavItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_default_avatar,
                label = stringResource(R.string.nav_profile),
                selected = currentDestination?.hierarchy?.any { it.route?.startsWith("profile") == true } == true,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = { navController.navigate("profile/") }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) activeColor else inactiveColor
        )
    }
}
