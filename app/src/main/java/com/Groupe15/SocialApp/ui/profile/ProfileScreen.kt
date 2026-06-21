@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.Groupe15.SocialApp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.User
import com.google.firebase.auth.FirebaseAuth

enum class ProfileTab { POSTS, SAVED, TAGGED, REELS } // SAVED ajouté

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    followViewModel: FollowViewModel,
    targetUid: String,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onMessage: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {}
) {
    val profileUser by viewModel.currentUser.collectAsState()
    val isOwnProfile by viewModel.isOwnProfile.collectAsState()
    val followState by followViewModel.followState.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val isLoadingPosts by viewModel.isLoadingPosts.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState() // ✅ NOUVEAU
    val isLoadingSaved by viewModel.isLoadingSaved.collectAsState() // ✅ NOUVEAU

    val followersList by followViewModel.followersList.collectAsState()
    val followingList by followViewModel.followingList.collectAsState()
    val isLoadingList by followViewModel.isLoadingList.collectAsState()
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }

    val currentAuthUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }

    LaunchedEffect(targetUid) {
        viewModel.loadProfile(targetUid)
        if (targetUid.isNotEmpty() && targetUid != "me") {
            followViewModel.checkIsFollowing(targetUid)
        }
    }

    LaunchedEffect(followState) {
        if (followState is FollowState.FollowSuccess || followState is FollowState.UnfollowSuccess) {
            viewModel.refreshProfile()
        }
    }

    val user = profileUser

    LaunchedEffect(user?.id) {
        user?.id?.let { uid ->
            if (uid.isNotEmpty()) {
                followViewModel.resyncCounts(uid)
            }
        }
    }

    //  déclenche le chargement des posts sauvegardés dès qu'on arrive sur son propre profil
    LaunchedEffect(isOwnProfile) {
        if (isOwnProfile) {
            viewModel.loadSavedPosts()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(user?.username ?: "", fontWeight = FontWeight.Bold) },
            actions = {
                if (isOwnProfile) {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                    }
                }
            }
        )

        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (user.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.coverImageUrl,
                        contentDescription = "Photo de couverture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                AsyncImage(
                    model = user.profileImageUrl.ifBlank { null },
                    contentDescription = "Photo de profil",
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                        .offset(y = 45.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Text(
                    text = user.displayName.ifBlank { user.username },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)
                }
                if (user.website.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.website,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat(count = user.postsCount, label = "Publications")
                    ProfileStat(
                        count = user.followersCount,
                        label = "Abonnés",
                        onClick = {
                            followViewModel.loadFollowers(user.id)
                            showFollowersSheet = true
                        }
                    )
                    ProfileStat(
                        count = user.followingCount,
                        label = "Abonnements",
                        onClick = {
                            followViewModel.loadFollowing(user.id)
                            showFollowingSheet = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isOwnProfile) {
                    OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                        Text("Modifier le profil")
                    }
                } else {
                    val isFollowing = when (followState) {
                        is FollowState.IsFollowing -> (followState as FollowState.IsFollowing).isFollowing
                        is FollowState.FollowSuccess -> true
                        is FollowState.UnfollowSuccess -> false
                        else -> false
                    }
                    val isLoading = followState is FollowState.Loading

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isFollowing) followViewModel.unfollowUser(targetUid)
                                else followViewModel.followUser(targetUid)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = if (isFollowing) ButtonDefaults.outlinedButtonColors()
                            else ButtonDefaults.buttonColors()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text(if (isFollowing) "Abonné(e)" else "Suivre")
                            }
                        }
                        OutlinedButton(onClick = { onMessage(user.username) }, modifier = Modifier.weight(1f)) {
                            Text("Message")
                        }
                    }

                    if (followState is FollowState.Error) {
                        Text(
                            text = (followState as FollowState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()

            //  liste d'onglets dynamique — "Saved" uniquement sur son propre profil
            val availableTabs = if (isOwnProfile)
                listOf(ProfileTab.POSTS, ProfileTab.SAVED, ProfileTab.TAGGED, ProfileTab.REELS)
            else
                listOf(ProfileTab.POSTS, ProfileTab.TAGGED, ProfileTab.REELS)

            // Si l'onglet sélectionné n'est plus disponible (ex: changement de profil), on revient sur POSTS
            LaunchedEffect(availableTabs) {
                if (selectedTab !in availableTabs) selectedTab = ProfileTab.POSTS
            }

            TabRow(
                selectedTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                availableTabs.forEach { tab ->
                    ProfileTabItem(
                        icon = when (tab) {
                            ProfileTab.POSTS -> Icons.Default.GridOn
                            ProfileTab.SAVED -> Icons.Outlined.BookmarkBorder
                            ProfileTab.TAGGED -> Icons.Default.LocalOffer
                            ProfileTab.REELS -> Icons.Default.PlayCircleOutline
                        },
                        label = tab.name,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }

            when (selectedTab) {
                ProfileTab.POSTS -> {
                    PostsGrid(posts = userPosts, isLoading = isLoadingPosts, emptyLabel = "Aucune publication")
                }
                //  grille des posts sauvegardés
                ProfileTab.SAVED -> {
                    PostsGrid(posts = savedPosts, isLoading = isLoadingSaved, emptyLabel = "Aucune publication enregistrée")
                }
                ProfileTab.TAGGED -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune publication taguée", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ProfileTab.REELS -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun reel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showFollowersSheet) {
        FollowListBottomSheet(
            title = "Abonnés",
            users = followersList,
            isLoading = isLoadingList,
            showActionButton = isOwnProfile,
            actionLabel = "Retirer",
            onActionClick = { uid -> followViewModel.removeFollowerFromList(uid) },
            onDismiss = { showFollowersSheet = false },
            onUserClick = { uid -> showFollowersSheet = false; onNavigateToProfile(uid) }
        )
    }

    if (showFollowingSheet) {
        FollowListBottomSheet(
            title = "Abonnements",
            users = followingList,
            isLoading = isLoadingList,
            showActionButton = isOwnProfile,
            actionLabel = "Se désabonner",
            onActionClick = { uid -> followViewModel.unfollowFromList(uid) },
            onDismiss = { showFollowingSheet = false },
            onUserClick = { uid -> showFollowingSheet = false; onNavigateToProfile(uid) }
        )
    }
}

//  grille factorisée, réutilisée par POSTS et SAVED
@Composable
private fun PostsGrid(
    posts: List<com.Groupe15.SocialApp.models.Post>,
    isLoading: Boolean,
    emptyLabel: String
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text(emptyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val rows = posts.chunked(3)
        Column {
            rows.forEach { rowPosts ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowPosts.forEach { post ->
                        AsyncImage(
                            model = post.imageUrls.firstOrNull(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    }
                    repeat(3 - rowPosts.size) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(count: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProfileTabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListBottomSheet(
    title: String,
    users: List<User>,
    isLoading: Boolean,
    showActionButton: Boolean,
    actionLabel: String,
    onActionClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Aucun utilisateur", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                items(users, key = { it.id }) { followUser ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(followUser.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = followUser.profileImageUrl.ifBlank { null },
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = followUser.displayName.ifBlank { followUser.username },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "@${followUser.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (showActionButton) {
                            OutlinedButton(
                                onClick = { onActionClick(followUser.id) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}