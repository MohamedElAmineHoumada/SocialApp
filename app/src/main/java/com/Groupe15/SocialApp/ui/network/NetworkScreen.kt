package com.Groupe15.SocialApp.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.SuggestionUser


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    followRequests: List<FollowRequest>,
    suggestions: List<SuggestionUser>,
    onAcceptRequest: (String) -> Unit = {},
    onDeclineRequest: (String) -> Unit = {},
    onFollowUser: (String) -> Unit = {},
    onSeeAllSuggestions: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { NetworkTopBar(onSearchClick, onNotificationsClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (followRequests.isNotEmpty()) {
                item { SectionHeader(title = "Follow Requests", count = followRequests.size) }
                items(followRequests, key = { it.id }) { request ->
                    FollowRequestCard(
                        request = request,
                        onAccept = { onAcceptRequest(request.id) },
                        onDecline = { onDeclineRequest(request.id) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "People You May Know",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(suggestions.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { user ->
                        SuggestionCard(
                            user = user,
                            modifier = Modifier.weight(1f),
                            onFollow = { onFollowUser(user.id) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            item {
                TextButton(
                    onClick = onSeeAllSuggestions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir toutes les suggestions →")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkTopBar(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("AFN", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Rechercher")
            }
        }
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (count != null && count > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FollowRequestCard(
    request: FollowRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(name = request.name, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${request.role} • ${request.mutualFriends} mutuals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDecline,
                modifier = Modifier
                    .size(34.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Refuser",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onAccept,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Accepter",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    user: SuggestionUser,
    modifier: Modifier = Modifier,
    onFollow: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box {
                Avatar(name = user.name, size = 56.dp)
                if (user.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = user.role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${user.mutualFriends} mutual friends",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onFollow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Text("Follow")
            }
        }
    }
}

@Composable
private fun Avatar(name: String, size: Dp) {
    val initials = remember(name) {
        name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / 2.6).sp
        )
    }
}

// ---------- Preview avec données factices (même contenu que la maquette) ----------

private val previewFollowRequests = listOf(
    FollowRequest(id = "1", name = "Julian Rivers", role = "Digital Curator", mutualFriends = 14),
    FollowRequest(id = "2", name = "Elena Soros", role = "Product Designer", mutualFriends = 8)
)

private val previewSuggestions = listOf(
    SuggestionUser(id = "1", name = "Marcus Chen", role = "Software Lead", mutualFriends = 22, isOnline = true),
    SuggestionUser(id = "2", name = "Aria Vane", role = "Art Director", mutualFriends = 5),
    SuggestionUser(id = "3", name = "David Miller", role = "Data Scientist", mutualFriends = 18),
    SuggestionUser(id = "4", name = "Sarah Oh", role = "Brand Strategist", mutualFriends = 31)
)

@Preview(showBackground = true)
@Composable
private fun NetworkScreenPreview() {
    MaterialTheme {
        NetworkScreen(
            followRequests = previewFollowRequests,
            suggestions = previewSuggestions
        )
    }
}