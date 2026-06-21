package com.Groupe15.SocialApp.ui.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    // ✅ Bouton Publier actif si TEXTE seul, IMAGE seule, ou les deux
    val canPublish = (postText.isNotBlank() || selectedImages.isNotEmpty()) && !isPosting

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
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
            TextField(
                value = postText,
                onValueChange = { postText = it },
                placeholder = { Text("Quoi de neuf ?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(modifier = Modifier.size(80.dp)) {
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
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Retirer",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ✅ "Audience" entièrement retiré. Seul "Ajouter Photo" reste, pleine largeur.
            OutlinedButton(
                onClick = {
                    imagePickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                    onPickImages()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ajouter Photo")
            }
        }
    }
}