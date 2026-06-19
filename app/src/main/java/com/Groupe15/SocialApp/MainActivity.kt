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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.livedata.observeAsState

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // ── Credential Manager (Google Sign-In moderne) ────────────────────────
    private lateinit var credentialManager: CredentialManager

    // ── Facebook callback manager ──────────────────────────────────────────
    private lateinit var callbackManager: CallbackManager

    // Lambda que l'écran de login va fournir pour transmettre le résultat
    private var onGoogleResult: ((String) -> Unit)? = null
    private var onFacebookResult: ((String) -> Unit)? = null
    private var onSocialError: ((String) -> Unit)? = null

    // Launcher pour Facebook (nécessaire avec le nouveau SDK)
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

        // Enregistrer le callback Facebook
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
            .setFilterByAuthorizedAccounts(false) // Affiche TOUS les comptes Google du téléphone
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false) // Forcer le sélecteur de compte
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
        "chat/{chatId}/{userName}", "createPost", "createStory", "storyViewer",
        "editProfile", "settings"
    )
    val showBottomNav = currentDestination?.route !in noBottomBarRoutes &&
            currentDestination?.route?.contains("chat/") == false

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                val networkViewModel = hiltViewModel<NetworkViewModel>()
                val followRequests by networkViewModel.followRequests.collectAsState()
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    CustomBottomNavigation(navController, currentDestination, followRequests.size)
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
                    onLoginClick = { _, _ -> },
                    onGoogleClick = {
                        onLaunchGoogle(
                            { idToken -> loginViewModel.signInWithGoogle(idToken) },
                            { error -> loginViewModel.resetState() /* l'erreur s'affiche via state */ }
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
                RegisterScreen(
                    viewModel = hiltViewModel<RegisterViewModel>(),
                    onRegisterClick = { _, _, _ -> },
                    onLoginClick = { navController.popBackStack() },
                    onSuccess = { navController.navigate("onboardingWelcome") }
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
                    onFinish = {
                        navController.navigate("feed") {
                            popUpTo("onboardingWelcome") { inclusive = true }
                        }
                    }
                )
            }

            composable("feed") {
                val loginViewModel: LoginViewModel = hiltViewModel()
                Column {
                    // Bannière email non vérifié en haut du feed
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
                val followRequests by networkViewModel.followRequests.collectAsState()
                val suggestions by networkViewModel.suggestions.collectAsState()
                NetworkScreen(
                    followRequests = followRequests,
                    suggestions = suggestions,
                    onAcceptRequest = { id -> networkViewModel.onAcceptRequest(id) },
                    onDeclineRequest = { id -> networkViewModel.onDeclineRequest(id) },
                    onFollowUser = { uid -> networkViewModel.onFollowUser(uid) }
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

                LaunchedEffect(effectiveUid) {
                    if (effectiveUid.isNotEmpty()) profileViewModel.loadProfile(effectiveUid)
                }
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
                    onNavigateToNotifications = {}
                )
            }
            composable("createPost") {
                CreatePostScreen(
                    viewModel = hiltViewModel<CreatePostViewModel>(),
                    onBack = { navController.popBackStack() },
                    onPickImages = {},
                    onShowAudiencePicker = {}
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
                    onAccountDeleted = {}
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
                    currentUserId = "USER_ID",
                    userName = userName,
                    onBack = { navController.popBackStack() },
                    onShowToast = {}
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
    // On surveille l'état du ViewModel pour détecter EmailNotVerified
    val state by loginViewModel.state.observeAsState(initial = com.Groupe15.SocialApp.ui.auth.AuthState.Idle)

    if (state is com.Groupe15.SocialApp.ui.auth.AuthState.EmailNotVerified) {
        com.Groupe15.SocialApp.ui.auth.EmailVerificationBanner(
            onResend = { loginViewModel.resendVerificationEmail() },
            onVerified = { loginViewModel.checkEmailVerified() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation (inchangée)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    currentDestination: NavDestination?,
    badgeCount: Int
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

            BottomNavItem(Modifier.weight(1f), R.drawable.ic_app_logo,
                stringResource(R.string.nav_home),
                currentDestination?.hierarchy?.any { it.route == "feed" } == true,
                activeColor, inactiveColor) { navController.navigate("feed") }

            BottomNavItem(Modifier.weight(1f), R.drawable.ic_comment,
                stringResource(R.string.nav_messages),
                currentDestination?.hierarchy?.any { it.route == "messages" } == true,
                activeColor, inactiveColor) { navController.navigate("messages") }

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

            BottomNavItem(Modifier.weight(1f), R.drawable.ic_people,
                stringResource(R.string.nav_network),
                currentDestination?.hierarchy?.any { it.route == "network" } == true,
                activeColor, inactiveColor, badgeCount) { navController.navigate("network") }

            BottomNavItem(Modifier.weight(1f), R.drawable.ic_default_avatar,
                stringResource(R.string.nav_profile),
                currentDestination?.hierarchy?.any { it.route?.startsWith("profile") == true } == true,
                activeColor, inactiveColor) { navController.navigate("profile/") }
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