package com.Groupe15.SocialApp.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (Post) -> Unit
) {
    val posts by viewModel.posts.observeAsState(emptyList())
    val stories by viewModel.stories.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    var commentsPostId by remember { mutableStateOf<String?>(null) }
    var sharePostId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                StoriesRow(stories = stories)
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            items(posts) { post ->
                PostCard(
                    post = post,
                    onLikeClick = { viewModel.toggleLike(post.postId) },
                    onCommentClick = { commentsPostId = post.postId },
                    onShareClick = { sharePostId = post.postId },
                    onAuthorClick = { onNavigateToProfile(post.authorUid) }
                )
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

    sharePostId?.let { postId ->
        ModalBottomSheet(onDismissRequest = { sharePostId = null }) {
            ShareScreen(
                viewModel = viewModel,
                postId = postId,
                onDismiss = { sharePostId = null }
            )
        }
    }
}

@Composable
private fun StoriesRow(stories: List<Story>) {
    if (stories.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(stories) { story ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(onClick = onAuthorClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.authorProfileUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = post.authorUsername, fontWeight = FontWeight.Bold)
        }

        if (post.imageUrl.isNotBlank()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLikeClick) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Aimer")
            }
            IconButton(onClick = onCommentClick) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Commenter")
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Send, contentDescription = "Partager")
            }
        }

        Text(
            text = "${post.likesCount} j'aime",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        if (post.content.isNotBlank()) {
            Text(
                text = post.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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