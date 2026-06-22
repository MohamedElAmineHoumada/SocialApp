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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.viewmodel.FeedTab
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (Post) -> Unit
) {
    val selectedTab by viewModel.selectedTab.observeAsState(FeedTab.FOLLOWING)
    val posts by viewModel.posts.observeAsState(emptyList())
    val stories by viewModel.stories.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val likedPostIds by viewModel.likedPostIds.observeAsState(emptySet())
    val savedPostIds by viewModel.savedPostIds.observeAsState(emptySet())

    var commentsPostId by remember { mutableStateOf<String?>(null) }
    var sharePost by remember { mutableStateOf<Post?>(null) }
    var storyViewerUserId by remember { mutableStateOf<String?>(null) }

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
                            onLikeClick = { viewModel.toggleLike(post.postId) },
                            onCommentClick = { commentsPostId = post.postId },
                            onShareClick = { sharePost = post },
                            onSaveClick = { viewModel.toggleSavePost(post.postId) },
                            onAuthorClick = { onNavigateToProfile(post.authorUid) }
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

// ─────────────────────────────────────────────────────────────────────────────
// Tab bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedTabBar(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        FeedTabItem(
            label = "Following",
            selected = selectedTab == FeedTab.FOLLOWING,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(FeedTab.FOLLOWING) }
        )
        FeedTabItem(
            label = "For You",
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
                .background(
                    if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// État vide
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyFeedState(tab: FeedTab) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (tab == FeedTab.FOLLOWING) "Aucune publication pour l'instant"
            else "Pas encore de suggestions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (tab == FeedTab.FOLLOWING)
                "Suis des comptes pour voir leurs publications ici."
            else "Revenez plus tard, du contenu vous sera proposé.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stories Row
// ─────────────────────────────────────────────────────────────────────────────

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
                    // ✅ pointerInput pour gérer tap court ET tap long sur la même zone
                    .pointerInput(story.userId) {
                        detectTapGestures(
                            onTap = { onStoryClick(story.userId) },
                            onLongPress = { onProfileClick(story.userId) }
                        )
                    }
            ) {
                // Anneau dégradé autour de l'avatar
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
                // ✅ clic sur le nom → profil aussi
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

// ─────────────────────────────────────────────────────────────────────────────
// PostCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PostCard(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // ── Header : avatar + username + menu ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAuthorClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.authorProfileUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = post.authorUsername, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { /* TODO: menu options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
        }

        // ---- ✅ Légende AVANT l'image ----
        if (post.content.isNotBlank()) {
            Text(
                text = buildAnnotatedCaption(post.authorUsername, post.content),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // ---- Image avec double-tap pour liker ----
        if (post.imageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .pointerInput(post.postId) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isLiked) onLikeClick()
                                showHeartBurst = true
                                scope.launch {
                                    delay(600)
                                    showHeartBurst = false
                                }
                            }
                        )
                    }
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartBurst,
                    enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                    exit = scaleOut(targetScale = 1.4f) + fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }
        }

        // ---- Actions : like, comment, share, save ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedLikeButton(isLiked = isLiked, onClick = onLikeClick)
            IconButton(onClick = onCommentClick) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Commenter")
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Outlined.Send, contentDescription = "Partager")
            }
            Spacer(modifier = Modifier.weight(1f))
            // ✅ Bouton Save fonctionnel : icône remplie quand sauvegardé
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Enregistrer",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface )
            }
        }

        if (post.likesCount > 0) {
            Text(
                text = "${post.likesCount} j'aime",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        if (post.commentsCount > 0) {
            Text(
                text = "Voir les ${post.commentsCount} commentaires",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clickable(onClick = onCommentClick)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun buildAnnotatedCaption(username: String, content: String) =
    androidx.compose.ui.text.buildAnnotatedString {
        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
            append(username)
        }
        append("  ")
        append(content)
    }

@Composable
private fun AnimatedLikeButton(isLiked: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isLiked) {
        scale.animateTo(1.3f, animationSpec = tween(120))
        scale.animateTo(1f, animationSpec = tween(120))
    }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Aimer",
            tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(26.dp)
                .scale(scale.value)
        )
    }
}