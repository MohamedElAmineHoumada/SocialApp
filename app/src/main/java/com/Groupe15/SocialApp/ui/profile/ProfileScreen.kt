@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.Groupe15.SocialApp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    followViewModel: FollowViewModel,
    targetUid: String,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onMessage: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isOwnProfile by viewModel.isOwnProfile.collectAsState()
    val followState by followViewModel.followState.collectAsState()

    LaunchedEffect(targetUid) {
        viewModel.loadProfile(targetUid, currentUid = "")
        if (targetUid.isNotEmpty()) {
            followViewModel.checkIsFollowing(targetUid)
        }
    }

    val user = currentUser

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(user?.username ?: "") },
            actions = {
                if (isOwnProfile) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres"
                        )
                    }
                }
            }
        )

        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(24.dp))

                    ProfileStat(count = user.postsCount, label = "Publications")
                    Spacer(modifier = Modifier.width(16.dp))
                    ProfileStat(count = user.followersCount, label = "Abonnés")
                    Spacer(modifier = Modifier.width(16.dp))
                    ProfileStat(count = user.followingCount, label = "Abonnements")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = user.displayName, fontWeight = FontWeight.Bold)
                if (user.bio.isNotBlank()) {
                    Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)
                }
                if (user.website.isNotBlank()) {
                    Text(
                        text = user.website,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isOwnProfile) {
                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Modifier le profil")
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val isFollowing = (followState as? FollowState.IsFollowing)?.isFollowing == true ||
                                followState is FollowState.FollowSuccess
                        val isLoading = followState is FollowState.Loading

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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isFollowing) "Abonné(e)" else "Suivre")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onMessage(user.username) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Message")
                        }
                    }
                }

                if (followState is FollowState.Error) {
                    Text(
                        text = (followState as FollowState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}