package com.Groupe15.SocialApp.ui.feed

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Post
import com.Groupe15.SocialApp.models.Story
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    FeedScreen(
                        viewModel = viewModel,
                        onNavigateToDiscover = { findNavController().navigate(R.id.discoverFragment) },
                        onNavigateToProfile = { uid ->
                            val bundle = Bundle().apply { putString("uid", uid) }
                            findNavController().navigate(R.id.action_feed_to_profile, bundle)
                        },
                        onCommentClick = { postId ->
                            CommentsBottomSheet.newInstance(postId)
                                .show(parentFragmentManager, "comments")
                        },
                        onShareClick = { postId ->
                            ShareBottomSheet.newInstance(postId)
                                .show(parentFragmentManager, "share")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    val posts by viewModel.posts.observeAsState(initial = emptyList<Post>())
    val stories by viewModel.stories.observeAsState(initial = emptyList<Story>())
    val isLoading by viewModel.isLoading.observeAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToDiscover) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                    }
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(painterResource(R.drawable.ic_notification), contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Simplified refresh logic as we don't have a PullToRefreshBox in basic M3 yet or might need specific setup
            // For now, using a simple LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Stories Row
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(stories) { story ->
                            StoryItem(story = story, onClick = { /* Handle story click */ })
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                }

                if (posts.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.8f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_posts),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    items(posts, key = { it.postId }) { post ->
                        PostItem(
                            post = post,
                            currentUserId = viewModel.currentUserId,
                            onLike = { viewModel.toggleLike(post.postId) },
                            onComment = { onCommentClick(post.postId) },
                            onShare = { onShareClick(post.postId) },
                            onFollow = { viewModel.followUser(post.authorUid) },
                            onProfile = { onNavigateToProfile(post.authorUid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryItem(story: Story, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(2.dp, Color(0xFF6200EE), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = story.userProfileUrl.ifEmpty { R.drawable.ic_default_avatar },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (story.isCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6200EE))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (story.isCurrentUser) stringResource(R.string.your_story) else story.username,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
fun PostItem(
    post: Post,
    currentUserId: String?,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onFollow: () -> Unit,
    onProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.authorProfileUrl.ifEmpty { R.drawable.ic_default_avatar },
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfile),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorUsername,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onProfile)
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            post.getCreatedAtMillis(),
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                if (post.authorUid != currentUserId) {
                    Text(
                        text = stringResource(R.string.follow),
                        color = Color(0xFF6200EE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(onClick = onFollow)
                            .padding(8.dp)
                    )
                }
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
            }

            // Content
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp
                )
            }

            // Image
            if (post.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Actions
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder, // Logic for filled vs border needed
                        contentDescription = "Like",
                        tint = Color.Gray
                    )
                }
                Text(text = "${post.likesCount}", fontSize = 13.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = onComment) {
                    Icon(painterResource(R.drawable.ic_comment), contentDescription = "Comment", tint = Color.Gray)
                }
                Text(text = "${post.commentsCount}", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.Gray)
                }
            }
        }
    }
}
