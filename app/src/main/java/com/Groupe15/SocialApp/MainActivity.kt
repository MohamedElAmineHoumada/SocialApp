package com.Groupe15.SocialApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.Groupe15.SocialApp.ui.auth.*
import com.Groupe15.SocialApp.ui.discover.*
import com.Groupe15.SocialApp.ui.feed.*
import com.Groupe15.SocialApp.ui.messages.*
import com.Groupe15.SocialApp.ui.network.*
import com.Groupe15.SocialApp.ui.post.*
import com.Groupe15.SocialApp.ui.profile.*
import com.Groupe15.SocialApp.ui.notifications.*
import com.Groupe15.SocialApp.ui.theme.SocialAppTheme
import com.Groupe15.SocialApp.viewmodel.*
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // ── Credential Manager (Google Sign-In moderne) ────────────────────────
    private lateinit var credentialManager: CredentialManager

    // ── Facebook callback manager ──────────────────────────────────────────
    private lateinit var callbackManager: CallbackManager

    private var onGoogleResult: ((String) -> Unit)? = null
    private var onFacebookResult: ((String) -> Unit)? = null
    private var onSocialError: ((String) -> Unit)? = null

    private val facebookLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            callbackManager.onActivityResult(result.resultCode, result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager.create(this)
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                onFacebookResult?.invoke(result.accessToken.token)
            }
            override fun onCancel() {
                onSocialError?.invoke("Connexion Facebook annulée")
            }
            override fun onError(error: FacebookException) {
                onSocialError?.invoke(error.message ?: "Erreur Facebook")
            }
        })

        setContent {
            SocialAppTheme(darkTheme = isDarkMode) {
                MainScreen(
                    onLaunchGoogle = { onResult, onError ->
                        onGoogleResult = onResult
                        onSocialError = onError
                        launchGoogleSignIn()
                    },
                    onLaunchFacebook = { onResult, onError ->
                        onFacebookResult = onResult
                        onSocialError = onError
                        LoginManager.getInstance().logInWithReadPermissions(
                            this,
                            callbackManager,
                            listOf("email", "public_profile")
                        )
                    }
                )
            }
        }
    }

    private fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = credentialManager.getCredential(this@MainActivity, request)
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onGoogleResult?.invoke(googleCredential.idToken)
                } else {
                    onSocialError?.invoke("Type de credential inattendu")
                }
            } catch (e: GetCredentialException) {
                val msg = when {
                    e.message?.contains("No credentials available") == true ->
                        "Aucun compte Google disponible sur cet appareil."
                    e.message?.contains("canceled") == true ->
                        "Connexion Google annulée."
                    else -> "Erreur Google : ${e.message}"
                }
                onSocialError?.invoke(msg)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable principal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    onLaunchGoogle: (onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onLaunchFacebook: (onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val noBottomBarRoutes = listOf(
        "login", "register", "forgotPassword", "onboardingWelcome",
        "onboardingDob", "onboardingGender", "onboardingInterests",
        "chat/{otherUserId}/{userName}", "createPost", "createStory", "storyViewer",
        "editProfile", "settings", "notifications", "notificationSettings"
    )
    val showBottomNav = currentDestination?.route !in noBottomBarRoutes &&
            currentDestination?.route?.contains("chat/") == false

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                val networkViewModel = hiltViewModel<NetworkViewModel>()
                val messagesViewModel = hiltViewModel<MessagesViewModel>()
                val notificationViewModel = hiltViewModel<NotificationViewModel>()

                val followRequests by networkViewModel.followRequests.collectAsState()
                val conversations by messagesViewModel.conversations.observeAsState(initial = emptyList<Conversation>())
                val unreadNotificationsCount by notificationViewModel.unreadCount.collectAsState()

                val unreadMessagesCount = conversations.count { it.hasUnread }

                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    CustomBottomNavigation(
                        navController = navController,
                        currentDestination = currentDestination,
                        networkBadgeCount = followRequests.size,
                        messagesBadgeCount = unreadMessagesCount,
                        notificationBadgeCount = unreadNotificationsCount
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(padding)
        ) {
            composable("login") {
                val loginViewModel: LoginViewModel = hiltViewModel()

                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginClick = { email, password -> loginViewModel.login(email, password) },
                    onGoogleClick = {
                        onLaunchGoogle(
                            { idToken -> loginViewModel.signInWithGoogle(idToken) },
                            { error -> loginViewModel.resetState() }
                        )
                    },
                    onFacebookClick = {
                        onLaunchFacebook(
                            { accessToken -> loginViewModel.signInWithFacebook(accessToken) },
                            { error -> loginViewModel.resetState() }
                        )
                    },
                    onRegisterClick = { navController.navigate("register") },
                    onForgotPasswordClick = { navController.navigate("forgotPassword") },
                    onSuccess = {
                        navController.navigate("feed") { popUpTo("login") { inclusive = true } }
                    }
                )
            }

            composable("forgotPassword") {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }

            composable("register") {
                val registerViewModel: RegisterViewModel = hiltViewModel()
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegisterClick = { email, password, name ->
                        registerViewModel.register(email, password, name)
                    },
                    onLoginClick = { navController.popBackStack() },
                    onGoogleClick = {
                        onLaunchGoogle(
                            { idToken -> registerViewModel.signInWithGoogle(idToken) },
                            { error -> registerViewModel.resetState() }
                        )
                    },
                    onFacebookClick = {
                        onLaunchFacebook(
                            { accessToken -> registerViewModel.signInWithFacebook(accessToken) },
                            { error -> registerViewModel.resetState() }
                        )
                    },
                    // ✅ FIX : popUpTo("register") pour vider la back-stack
                    // avant d'aller vers l'onboarding. Sans ça, "register" restait
                    // dans la pile et bloquait la navigation sur onboardingDob.
                    onSuccess = {
                        navController.navigate("onboardingWelcome") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }

            composable("onboardingWelcome") {
                OnboardingWelcomeScreen(onStart = { navController.navigate("onboardingDob") })
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
                    // ✅ FIX : popUpTo(0) vide toute la back-stack (login + onboarding)
                    // pour que l'utilisateur ne puisse pas revenir en arrière depuis le feed.
                    onFinish = {
                        navController.navigate("feed") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("feed") {
                val loginViewModel: LoginViewModel = hiltViewModel()
                Column {
                    EmailVerificationBannerWrapper(loginViewModel = loginViewModel)
                    FeedScreen(
                        viewModel = hiltViewModel<FeedViewModel>(),
                        onNavigateToDiscover = { navController.navigate("discover") },
                        onNavigateToProfile = { uid -> navController.navigate("profile/$uid") },
                        onCommentClick = {},
                        onShareClick = {}
                    )
                }
            }

            composable("messages") {
                MessagesScreen(
                    viewModel = hiltViewModel(),
                    onConversationClick = { conv ->
                        navController.navigate("chat/${conv.userId}/${conv.username}")
                    }
                )
            }

            composable("network") {
                val networkViewModel = hiltViewModel<NetworkViewModel>()
                NetworkScreen(
                    viewModel = networkViewModel,
                    onSeeAllSuggestions = { navController.navigate("all_suggestions") },
                    onNavigateToNotifications = { navController.navigate("notifications") },
                    onUserClick = { uid -> navController.navigate("profile/$uid") }
                )
            }

            composable("all_suggestions") {
                val networkViewModel = hiltViewModel<NetworkViewModel>()
                SuggestionsScreen(
                    viewModel = networkViewModel,
                    onBack = { navController.popBackStack() },
                    onUserClick = { uid -> navController.navigate("profile/$uid") }
                )
            }

            composable(
                "profile/{uid}",
                arguments = listOf(navArgument("uid") { defaultValue = "" })
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: ""
                val authViewModel = hiltViewModel<AuthViewModel>()
                val profileViewModel = hiltViewModel<ProfileViewModel>()
                val effectiveUid = if (uid.isEmpty() || uid == "YOUR_USER_ID")
                    authViewModel.getCurrentUserUid() ?: ""
                else uid

                ProfileScreen(
                    viewModel = profileViewModel,
                    followViewModel = hiltViewModel<FollowViewModel>(),
                    targetUid = effectiveUid,
                    onEditProfile = { navController.navigate("editProfile") },
                    onSettings = { navController.navigate("settings") },
                    onMessage = { name -> navController.navigate("chat/$effectiveUid/$name") },
                    onNavigateToProfile = { otherUid -> navController.navigate("profile/$otherUid") }
                )
            }

            composable("discover") {
                DiscoverScreen(
                    onNavigateToProfile = { uid -> navController.navigate("profile/$uid") },
                    onNavigateToNotifications = { navController.navigate("notifications") }
                )
            }

            composable("notifications") {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("createPost") {
                CreatePostScreen(
                    viewModel = hiltViewModel<CreatePostViewModel>(),
                    onBack = { navController.popBackStack() },
                    onPickImages = {}
                )
            }

            composable("editProfile") {
                EditProfileScreen(
                    viewModel = hiltViewModel<EditProfileViewModel>(),
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() },
                    onError = {}
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = hiltViewModel<AuthViewModel>(),
                    onBack = { navController.popBackStack() },
                    onShowToast = {},
                    onAccountDeleted = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onLoggedOut = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onNotifications = { navController.navigate("notificationSettings") }
                )
            }

            composable("notificationSettings") {
                NotificationSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "chat/{otherUserId}/{userName}",
                arguments = listOf(
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                val userName = backStackEntry.arguments?.getString("userName") ?: "Chat"
                val authViewModel = hiltViewModel<AuthViewModel>()
                val currentUserId = authViewModel.getCurrentUserUid() ?: ""
                val context = LocalContext.current

                ChatScreen(
                    viewModel = hiltViewModel(),
                    currentUserId = currentUserId,
                    otherUserId = otherUserId,
                    userName = userName,
                    onBack = { navController.popBackStack() },
                    onCall = { isVideo -> navController.navigate("call/$userName/$isVideo") },
                    onShowToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                )
            }

            composable(
                "call/{userName}/{isVideo}",
                arguments = listOf(
                    navArgument("userName") { type = NavType.StringType },
                    navArgument("isVideo") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val userName = backStackEntry.arguments?.getString("userName") ?: "User"
                val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
                CallScreen(
                    userName = userName,
                    userAvatar = "",
                    isVideoCall = isVideo,
                    onHangUp = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Affiche la bannière de vérification d'email uniquement si l'utilisateur
 * est connecté mais son email n'est pas encore vérifié.
 */
@Composable
fun EmailVerificationBannerWrapper(loginViewModel: LoginViewModel) {
    val state by loginViewModel.state.observeAsState(initial = com.Groupe15.SocialApp.ui.auth.AuthState.Idle)

    if (state is com.Groupe15.SocialApp.ui.auth.AuthState.EmailNotVerified) {
        com.Groupe15.SocialApp.ui.auth.EmailVerificationBanner(
            onResend = { loginViewModel.resendVerificationEmail() },
            onVerified = { loginViewModel.checkEmailVerified() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    currentDestination: NavDestination?,
    networkBadgeCount: Int,
    messagesBadgeCount: Int,
    notificationBadgeCount: Int = 0
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
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
                badgeCount = messagesBadgeCount,
                onClick = { navController.navigate("messages") }
            )

            Box(
                modifier = Modifier.weight(1.2f).fillMaxHeight()
                    .clickable { navController.navigate("createPost") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(48.dp).background(
                            brush = Brush.linearGradient(listOf(Color(0xFF6C47FF), Color(0xFF9D47FF))),
                            shape = RoundedCornerShape(16.dp)
                        ), contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_add_circle), null,
                            tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.nav_studio), fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentDestination?.route == "createPost") activeColor else inactiveColor)
                }
            }

            BottomNavItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_people,
                label = stringResource(R.string.nav_network),
                selected = currentDestination?.hierarchy?.any { it.route == "network" } == true,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                badgeCount = networkBadgeCount + notificationBadgeCount,
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
    modifier: Modifier = Modifier, iconRes: Int, label: String, selected: Boolean,
    activeColor: Color, inactiveColor: Color, badgeCount: Int = 0, onClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(painterResource(iconRes), null,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp))
            if (badgeCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp),
                    color = Color.Red, shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (selected) activeColor else inactiveColor)
    }
}