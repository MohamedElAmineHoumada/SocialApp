package com.Groupe15.SocialApp.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val targetUid = arguments?.getString("uid") ?: ""
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        viewModel.loadProfile(
            targetUid = targetUid.ifEmpty { currentUid },
            currentUid = currentUid
        )

        if (targetUid.isNotEmpty() && targetUid != currentUid) {
            followViewModel.checkIsFollowing(targetUid)
        }

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    ProfileScreen(
                        viewModel = viewModel,
                        followViewModel = followViewModel,
                        targetUid = targetUid,
                        onEditProfile = { findNavController().navigate(R.id.editProfileFragment) },
                        onSettings = { findNavController().navigate(R.id.action_profile_to_settings) },
                        onMessage = { name ->
                            val bundle = Bundle().apply {
                                putString("chatId", targetUid)
                                putString("userName", name)
                            }
                            findNavController().navigate(R.id.action_profile_to_chat, bundle)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    followViewModel: FollowViewModel,
    targetUid: String,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onMessage: (String) -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isOwnProfile by viewModel.isOwnProfile.collectAsStateWithLifecycle()
    val followState by followViewModel.followState.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AFN", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val newMode = !isDarkMode
                        isDarkMode = newMode
                        prefs.edit().putBoolean("dark_mode", newMode).apply()
                        AppCompatDelegate.setDefaultNightMode(
                            if (newMode) AppCompatDelegate.MODE_NIGHT_YES
                            else AppCompatDelegate.MODE_NIGHT_NO
                        )
                    }) {
                        Icon(
                            painter = painterResource(if (isDarkMode) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                            contentDescription = "Theme Toggle"
                        )
                    }
                    IconButton(onClick = { /* Notifs */ }) {
                        Icon(painterResource(R.drawable.ic_notification), contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Cover + Profile Pic
            Box(modifier = Modifier.height(160.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.LightGray)
                )
                
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(80.dp)
                        .align(Alignment.BottomStart)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = user?.profileImageUrl ?: R.drawable.ic_default_avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnProfile) {
                        OutlinedButton(
                            onClick = onEditProfile,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(stringResource(R.string.edit_profile), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        ) {
                            Icon(painterResource(R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        FollowButton(
                            state = followState,
                            onFollowClick = {
                                if (followState is FollowState.IsFollowing && (followState as FollowState.IsFollowing).isFollowing) {
                                    followViewModel.unfollowUser(targetUid)
                                } else {
                                    followViewModel.followUser(targetUid)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onMessage(user?.displayName ?: "") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                            border = BorderStroke(1.dp, Color(0xFF6200EE))
                        ) {
                            Text(stringResource(R.string.message), color = Color(0xFF6200EE), fontSize = 13.sp)
                        }
                    }
                }
            }

            // User Info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = user?.displayName ?: "User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user?.username ?: ""}",
                    color = Color(0xFF6200EE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!user?.bio.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = user!!.bio, fontSize = 14.sp, lineHeight = 20.sp)
                }
                if (!user?.website.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_link), contentDescription = null, tint = Color(0xFF6200EE), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = user!!.website, color = Color(0xFF6200EE), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = stringResource(R.string.posts_stat), count = user?.postsCount?.toString() ?: "0")
                StatItem(label = stringResource(R.string.followers_stat), count = user?.followersCount?.toString() ?: "0")
                StatItem(label = stringResource(R.string.following_stat), count = user?.followingCount?.toString() ?: "0")
            }

            // Tabs (Grid/Reels/Tags)
            var selectedTab by remember { mutableIntStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF6200EE),
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Icon(painterResource(R.drawable.ic_grid), contentDescription = null, modifier = Modifier.padding(12.dp).size(22.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Icon(painterResource(R.drawable.ic_reels), contentDescription = null, modifier = Modifier.padding(12.dp).size(22.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Icon(painterResource(R.drawable.ic_tag), contentDescription = null, modifier = Modifier.padding(12.dp).size(22.dp))
                }
            }

            // Fake Grid (En attendant le chargement réel des posts)
            Box(modifier = Modifier.height(400.dp).fillMaxWidth()) {
                Text(stringResource(R.string.no_posts), modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FollowButton(state: FollowState, onFollowClick: () -> Unit) {
    val isFollowing = state is FollowState.IsFollowing && state.isFollowing || state is FollowState.FollowSuccess
    val isLoading = state is FollowState.Loading

    Button(
        onClick = onFollowClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) Color(0xFFE0E0E0) else Color(0xFF6200EE),
            contentColor = if (isFollowing) Color.Black else Color.White
        )
    ) {
        Text(if (isFollowing) stringResource(R.string.unfollow) else stringResource(R.string.follow), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
