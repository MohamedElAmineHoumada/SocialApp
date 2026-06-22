package com.Groupe15.SocialApp.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.viewmodel.FeedTab
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.Groupe15.SocialApp.viewmodel.FeedUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (Post) -> Unit,
    initialCommentsPostId: String? = null
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.observeAsState(FeedTab.FOLLOWING)
    val posts by viewModel.posts.observeAsState(emptyList())
    val stories by viewModel.stories.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val likedPostIds by viewModel.likedPostIds.observeAsState(emptySet())
    val savedPostIds by viewModel.savedPostIds.observeAsState(emptySet())

    var commentsPostId by remember { mutableStateOf<String?>(initialCommentsPostId) }
    var sharePost by remember { mutableStateOf<Post?>(null) }
    var storyViewerUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is FeedUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(posts) {
        if (posts.isNotEmpty()) {
            viewModel.refreshLikedState(posts.map { it.postId })
        }
    }

    // Pas de remember() — recalculé à chaque recomposition
    val storiesByUser = stories.groupBy { it.userId }

    // ── Story Viewer — affiché EN PREMIER (par-dessus tout) ──────────────
    if (storyViewerUserId != null) {
        val userStories = storiesByUser[storyViewerUserId].orEmpty()
        if (userStories.isNotEmpty()) {
            StoryViewerScreen(
                stories = userStories,
                onDeleteStory = { storyId -> viewModel.deleteStory(storyId) },
                onClose = { storyViewerUserId = null }
            )
        } else {
            LaunchedEffect(storyViewerUserId) { storyViewerUserId = null }
        }
        return
    }

    // ── Feed normal ───────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        FeedTabBar(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.selectTab(it) }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (posts.isEmpty()) {
                EmptyFeedState(tab = selectedTab)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        StoriesRow(
                            stories = stories,
                            // ✅ tap court → ouvre le viewer
                            onStoryClick = { userId -> storyViewerUserId = userId },
                            // ✅ tap long → navigue vers le profil
                            onProfileClick = { userId -> onNavigateToProfile(userId) }
                        )
                    }

                    itemsIndexed(posts, key = { index, post ->
                        post.postId.ifBlank { "post_$index" }
                    }) { _, post ->
                        PostCard(
                            post = post,
                            isLiked = post.postId in likedPostIds,
                            isSaved = post.postId in savedPostIds,
                            currentUserId = viewModel.currentUserId,
                            onLikeClick = { viewModel.toggleLike(post.postId) },
                            onCommentClick = { commentsPostId = post.postId },
                            onShareClick = { sharePost = post },
                            onSaveClick = { viewModel.toggleSavePost(post.postId) },
                            onAuthorClick = { onNavigateToProfile(post.authorUid) },
                            onMediaClick = { p -> onNavigateToPost(p.postId) },
                            onDeleteClick = { viewModel.deletePost(post.postId) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }

    // ── Commentaires ──────────────────────────────────────────────────────
    commentsPostId?.let { postId ->
        ModalBottomSheet(onDismissRequest = { commentsPostId = null }) {
            CommentsScreen(
                viewModel = viewModel,
                postId = postId,
                onDismiss = { commentsPostId = null },
                onNavigateToProfile = { uid ->
                    commentsPostId = null
                    onNavigateToProfile(uid)
                }
            )
        }
    }

    // ── Partage ───────────────────────────────────────────────────────────
    sharePost?.let { post ->
        ModalBottomSheet(onDismissRequest = { sharePost = null }) {
            ShareScreen(
                viewModel = viewModel,
                post = post,
                onDismiss = { sharePost = null }
            )
        }
    }
}

@Composable
private fun FeedTabBar(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        FeedTabItem(
            label = stringResource(R.string.following_tab),
            selected = selectedTab == FeedTab.FOLLOWING,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(FeedTab.FOLLOWING) }
        )
        FeedTabItem(
            label = stringResource(R.string.foryou_tab),
            selected = selectedTab == FeedTab.FOR_YOU,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(FeedTab.FOR_YOU) }
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FeedTabItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
        )
    }
}

@Composable
private fun EmptyFeedState(tab: FeedTab) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (tab == FeedTab.FOLLOWING) stringResource(R.string.no_posts)
            else stringResource(R.string.no_suggestions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (tab == FeedTab.FOLLOWING)
                stringResource(R.string.follow_to_see_posts)
            else stringResource(R.string.check_back_later_content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * [onStoryClick]   → tap court sur la bulle  → ouvre le StoryViewerScreen
 * [onProfileClick] → tap long sur la bulle   → navigue vers profile/{userId}
 */
@Composable
private fun StoriesRow(
    stories: List<Story>,
    onStoryClick: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    if (stories.isEmpty()) return

    val uniqueStories: List<Story> = stories
        .groupBy { it.userId }
        .values
        .mapNotNull { it.maxByOrNull { s -> s.timestamp?.seconds ?: 0L } }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(
            uniqueStories,
            key = { it.userId.ifBlank { "story_${uniqueStories.indexOf(it)}" } }
        ) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .pointerInput(story.userId) {
                        detectTapGestures(
                            onTap = { onStoryClick(story.userId) },
                            onLongPress = { onProfileClick(story.userId) }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF6C47FF),
                                    Color(0xFFE91E8C),
                                    Color(0xFFFFC107),
                                    Color(0xFF6C47FF)
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = story.userProfileUrl.ifBlank { null },
                        contentDescription = story.username,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.username,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.clickable { onProfileClick(story.userId) }
                )
            }
        }
    }
}
