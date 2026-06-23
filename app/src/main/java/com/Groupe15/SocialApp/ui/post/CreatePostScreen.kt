package com.Groupe15.SocialApp.ui.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.viewmodel.CreatePostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onPickImages: () -> Unit
) {
    var postText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val isPosting by viewModel.isPosting.collectAsState()
    val postSuccess by viewModel.postSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val visibility by viewModel.visibility.collectAsState()

    var showVisibilityMenu by remember { mutableStateOf(false) }

    val canPublish = (postText.isNotBlank() || selectedImages.isNotEmpty() || selectedVideo != null) && !isPosting

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.setVideo(it) }
    }

    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            onBack()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer une publication") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.createPost(postText) },
                        enabled = canPublish,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Publier")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // User info and Visibility selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AssistChip(
                        onClick = { showVisibilityMenu = true },
                        label = { Text(visibility) },
                        leadingIcon = {
                            val icon = when (visibility) {
                                "Public" -> Icons.Default.Public
                                "Friends" -> Icons.Default.Group
                                "FriendsOfFriends" -> Icons.Default.PeopleOutline
                                else -> Icons.Default.Lock
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenu(
                        expanded = showVisibilityMenu,
                        onDismissRequest = { showVisibilityMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Public") },
                            onClick = { viewModel.setVisibility("Public"); showVisibilityMenu = false },
                            leadingIcon = { Icon(Icons.Default.Public, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Friends") },
                            onClick = { viewModel.setVisibility("Friends"); showVisibilityMenu = false },
                            leadingIcon = { Icon(Icons.Default.Group, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Friends of Friends") },
                            onClick = { viewModel.setVisibility("FriendsOfFriends"); showVisibilityMenu = false },
                            leadingIcon = { Icon(Icons.Default.PeopleOutline, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Only Me") },
                            onClick = { viewModel.setVisibility("OnlyMe"); showVisibilityMenu = false },
                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                        )
                    }
                }
            }

            TextField(
                value = postText,
                onValueChange = { postText = it },
                placeholder = { Text("Quoi de neuf ? #Hashtag...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(modifier = Modifier.size(100.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removeImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (selectedVideo != null) {
                Box(modifier = Modifier.size(100.dp).padding(bottom = 12.dp)) {
                    // In a real app, you'd show a thumbnail here
                    Surface(
                        color = Color.Black.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Video selected", fontSize = 10.sp)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.setVideo(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Photos")
                }
                
                OutlinedButton(
                    onClick = {
                        videoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.VideoOnly
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Vidéo")
                }
            }
        }
    }
}