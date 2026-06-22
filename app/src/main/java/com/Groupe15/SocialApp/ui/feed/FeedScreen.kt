package com.Groupe15.SocialApp.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
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
    val savedPostIds by viewModel.savedPostIds.observeAsState(emptySet()) // ✅ NOUVEAU

    var commentsPostId by remember { mutableStateOf<String?>(null) }
    var sharePost by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(posts) {
        if (posts.isNotEmpty()) {
            viewModel.refreshLikedState(posts.map { it.postId })
        }
    }

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
                    item { StoriesRow(stories = stories) }

                    itemsIndexed(posts, key = { index, post ->
                        post.postId.ifBlank { "post_$index" }
                    }) { _, post ->
                        PostCard(
                            post = post,
                            isLiked = post.postId in likedPostIds,
                            isSaved = post.postId in savedPostIds, // ✅ NOUVEAU
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

    commentsPostId?.let { postId ->
        ModalBottomSheet(onDismissRequest = { commentsPostId = null }) {
            CommentsScreen(
                viewModel = viewModel,
                postId = postId,
                onDismiss = { commentsPostId = null }
            )
        }
    }

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

@Composable
private fun StoriesRow(stories: List<Story>) {
    if (stories.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(stories, key = { it.storyId.ifBlank { "story_${stories.indexOf(it)}" } }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = story.userProfileUrl,
                        contentDescription = story.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = story.username, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean, // ✅ NOUVEAU
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
        // ---- Header : avatar + username + menu ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).clickable(onClick = onAuthorClick),
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
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
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
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = stringResource(R.string.comment))
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Outlined.Send, contentDescription = stringResource(R.string.share))
            }
            Spacer(modifier = Modifier.weight(1f))
            // ✅ Bouton Save fonctionnel : icône remplie quand sauvegardé
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.save),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (post.likesCount > 0) {
            Text(
                text = stringResource(R.string.likes_count, post.likesCount),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        if (post.commentsCount > 0) {
            Text(
                text = stringResource(R.string.view_comments, post.commentsCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clickable(onClick = onCommentClick)
            )
        }
    }
}

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
            modifier = Modifier.size(26.dp).scale(scale.value)
        )
    }
}