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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onMediaClick: (Post) -> Unit = {}
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // ── Header : avatar + username + visibility + menu ────────────────────────────
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
                Column {
                    Text(text = post.authorUsername, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (post.visibility) {
                            "Public" -> Icons.Default.Public
                            "Friends" -> Icons.Default.Group
                            "FriendsOfFriends" -> Icons.Default.PeopleOutline
                            else -> Icons.Default.Lock
                        }
                        Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(post.visibility, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = { /* TODO: menu options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
            }
        }

        // ---- Légende ----
        if (post.content.isNotBlank()) {
            Text(
                text = buildAnnotatedCaption(post.authorUsername, post.content),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // ---- Media (Carousel or Video) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { onMediaClick(post) }
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
            if (post.videoUrl.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    VideoPlayer(videoUrl = post.videoUrl)
                }
            } else if (post.imageUrls.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { post.imageUrls.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                ) { page ->
                    AsyncImage(
                        model = post.imageUrls[page],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (post.imageUrls.size > 1) {
                    // Pager Indicator
                    Row(
                        Modifier
                            .height(20.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(post.imageUrls.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(6.dp)
                            )
                        }
                    }
                }
            }

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

@Composable
fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun buildAnnotatedCaption(username: String, content: String) =
    androidx.compose.ui.text.buildAnnotatedString {
        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
            append(username)
        }
        append("  ")
        
        val hashtagRegex = "#(\\w+)".toRegex()
        var lastIndex = 0
        
        hashtagRegex.findAll(content).forEach { result ->
            append(content.substring(lastIndex, result.range.first))
            pushStringAnnotation(tag = "HASHTAG", annotation = result.value)
            withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF6C47FF), fontWeight = FontWeight.Bold)) {
                append(result.value)
            }
            pop()
            lastIndex = result.range.last + 1
        }
        
        if (lastIndex < content.length) {
            append(content.substring(lastIndex))
        }
    }

@Composable
fun AnimatedLikeButton(isLiked: Boolean, onClick: () -> Unit) {
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
