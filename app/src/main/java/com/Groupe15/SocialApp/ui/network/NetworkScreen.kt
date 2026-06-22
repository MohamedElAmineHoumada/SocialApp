package com.Groupe15.SocialApp.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.FollowRequest
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.SuggestionUser
import com.Groupe15.SocialApp.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
    onSeeAllSuggestions: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
    val followRequests by viewModel.followRequests.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val trendingPosts by viewModel.trendingPosts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    NetworkContent(
        followRequests = followRequests,
        suggestions = suggestions,
        trendingPosts = trendingPosts,
        searchQuery = searchQuery,
        selectedCategory = selectedCategory,
        isLoadingMore = isLoadingMore,
        categories = viewModel.categories,
        onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
        onCategorySelect = { viewModel.onCategorySelect(it) },
        onFollowUser = { viewModel.onFollowUser(it) },
        onAcceptRequest = { viewModel.onAcceptRequest(it) },
        onDeclineRequest = { viewModel.onDeclineRequest(it) },
        onNavigateToNotifications = onNavigateToNotifications,
        onLoadMore = { viewModel.loadTrendingPosts() },
        onSeeAllSuggestions = onSeeAllSuggestions,
        onUserClick = onUserClick,
        onPostClick = onPostClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkContent(
    followRequests: List<FollowRequest>,
    suggestions: List<SuggestionUser>,
    trendingPosts: List<Post>,
    searchQuery: String,
    selectedCategory: String,
    isLoadingMore: Boolean,
    categories: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onFollowUser: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLoadMore: () -> Unit,
    onSeeAllSuggestions: () -> Unit,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Detect when we reach the end of the list
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItemsCount - 2 // Load more when 2 items from the end
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMore) {
            onLoadMore()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Network",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelect
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Follow Requests section
            if (searchQuery.isEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Follow Requests",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (followRequests.isNotEmpty()) {
                                TextButton(onClick = { /* Handle see all requests */ }) {
                                    Text("See all", color = Color(0xFF6C47FF))
                                }
                            }
                        }

                        if (followRequests.isEmpty()) {
                            Text(
                                "No pending requests",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(followRequests) { request ->
                                    FollowRequestCard(
                                        request = request,
                                        onAccept = { onAcceptRequest(request.id) },
                                        onDecline = { onDeclineRequest(request.id) },
                                        onClick = { onUserClick(request.id) } // assuming id is userId here
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Who to follow section
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (searchQuery.isEmpty()) "Who to follow" else "Search results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (searchQuery.isEmpty()) {
                            TextButton(onClick = onSeeAllSuggestions) {
                                Text("See all", color = Color(0xFF6C47FF))
                            }
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(suggestions) { user ->
                            WhoToFollowCard(
                                user = user,
                                onFollow = { onFollowUser(user.id) },
                                onClick = { onUserClick(user.id) }
                            )
                        }
                    }
                }
            }

            // Trending Section
            item {
                Text(
                    "Trending",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Trending Grid
            items(trendingPosts.chunked(2)) { rowPosts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPosts.forEach { post ->
                        TrendingPostCard(
                            post = post,
                            modifier = Modifier.weight(1f),
                            onClick = { onPostClick(post.postId) }
                        )
                    }
                    if (rowPosts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF6C47FF),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text("Discover creators, art, and tech...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true
    )
}

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6C47FF),
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

@Composable
fun FollowRequestCard(
    request: FollowRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.width(160.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .border(2.dp, Color(0xFF6C47FF), CircleShape)
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = request.avatarUrl ?: "https://ui-avatars.com/api/?name=${request.name}&background=random",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                request.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                request.role,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
                Button(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun WhoToFollowCard(
    user: SuggestionUser,
    onFollow: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .border(2.dp, Color(0xFF6C47FF), CircleShape)
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = user.avatarUrl ?: "https://ui-avatars.com/api/?name=${user.name}&background=random",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                user.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                user.role,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFollow,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Follow", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TrendingPostCard(
    post: Post,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.8f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Optional: overlay for text if any
            if (post.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        post.content,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
            }
        }
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
        NetworkContent(
            followRequests = previewFollowRequests,
            suggestions = previewSuggestions,
            trendingPosts = listOf(
                Post(postId = "1", authorUsername = "user1", content = "Trending post 1"),
                Post(postId = "2", authorUsername = "user2", content = "Trending post 2")
            ),
            searchQuery = "",
            selectedCategory = "All",
            isLoadingMore = false,
            categories = listOf("All", "AI", "Tech"),
            onSearchQueryChange = {},
            onCategorySelect = {},
            onFollowUser = {},
            onAcceptRequest = {},
            onDeclineRequest = {},
            onNavigateToNotifications = {},
            onLoadMore = {},
            onSeeAllSuggestions = {},
            onUserClick = {},
            onPostClick = {}
        )
    }
}