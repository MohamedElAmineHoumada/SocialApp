package com.Groupe15.SocialApp.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.Story
import com.google.firebase.auth.FirebaseAuth

private const val STORY_DURATION_MS = 5_000


@Composable
fun StoryViewerScreen(
    stories: List<Story>,
    initialIndex: Int = 0,
    onDeleteStory: (String) -> Unit = {},
    onClose: () -> Unit
) {
    if (stories.isEmpty()) {
        onClose()
        return
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val safeInitial = initialIndex.coerceIn(0, stories.lastIndex)
    var currentIndex by remember { mutableIntStateOf(safeInitial) }
    val story = stories[currentIndex]

    var showDeleteDialog by remember { mutableStateOf(false) }
    val progress = remember(currentIndex) { Animatable(0f) }

    LaunchedEffect(currentIndex, showDeleteDialog) {
        if (!showDeleteDialog) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ((1f - progress.value) * STORY_DURATION_MS).toInt(),
                    easing = LinearEasing
                )
            )
            if (currentIndex < stories.lastIndex) {
                currentIndex++
            } else {
                onClose()
            }
        } else {
            progress.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Image ─────────────────────────────────────────────────────────
        AsyncImage(
            model = story.displayMediaUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // ── Tap gauche / droite ───────────────────────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(currentIndex) {
                        detectTapGestures {
                            if (currentIndex > 0) currentIndex-- else onClose()
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(currentIndex) {
                        detectTapGestures {
                            if (currentIndex < stories.lastIndex) currentIndex++
                            else onClose()
                        }
                    }
            )
        }

        // ── Overlay haut : barres + header ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Barres de progression
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header : avatar + nom + heure + bouton fermer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = story.userProfileUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    story.timestamp?.let { ts ->
                        val minutes = ((System.currentTimeMillis() / 1000 - ts.seconds) / 60).toInt()
                        val label = when {
                            minutes < 1  -> "À l'instant"
                            minutes < 60 -> "Il y a $minutes min"
                            else         -> "Il y a ${minutes / 60} h"
                        }
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                if (story.userId == currentUserId) {
                    IconButton(onClick = {
                        showDeleteDialog = true
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Supprimer la story") },
                text = { Text("Voulez-vous vraiment supprimer cette story ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteStory(story.storyId)
                            if (stories.size > 1) {
                                if (currentIndex < stories.lastIndex) {
                                    // Pas besoin de changer d'index, la liste va se réduire
                                } else {
                                    currentIndex--
                                }
                            } else {
                                onClose()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }

        // ── Texte superposé (optionnel) ───────────────────────────────────
        story.text?.takeIf { it.isNotBlank() }?.let { txt ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = txt,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}