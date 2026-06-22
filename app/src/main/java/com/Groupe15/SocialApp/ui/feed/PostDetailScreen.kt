package com.Groupe15.SocialApp.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: FeedViewModel,
    postId: String,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    LaunchedEffect(postId) {
        viewModel.selectPost(postId)
    }

    val post by viewModel.selectedPost.observeAsState()
    val likedPostIds by viewModel.likedPostIds.observeAsState(emptySet())
    val savedPostIds by viewModel.savedPostIds.observeAsState(emptySet())
    var sharePost by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publication", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (post == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        PostCard(
                            post = post!!,
                            isLiked = post!!.postId in likedPostIds,
                            isSaved = post!!.postId in savedPostIds,
                            currentUserId = viewModel.currentUserId,
                            onLikeClick = { viewModel.toggleLike(post!!.postId) },
                            onCommentClick = { /* Scroll to comments maybe? */ },
                            onShareClick = { sharePost = post },
                            onSaveClick = { viewModel.toggleSavePost(post!!.postId) },
                            onAuthorClick = { onNavigateToProfile(post!!.authorUid) },
                            onDeleteClick = {
                                viewModel.deletePost(post!!.postId)
                                onBack()
                            }
                        )
                    }
                    
                    item {
                        CommentsScreen(
                            viewModel = viewModel,
                            postId = postId,
                            onDismiss = { /* No-op here as it's not a sheet */ },
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }
                }
            }
        }
    }

    sharePost?.let { p ->
        ModalBottomSheet(onDismissRequest = { sharePost = null }) {
            ShareScreen(
                viewModel = viewModel,
                post = p,
                onDismiss = { sharePost = null }
            )
        }
    }
}
