package com.Groupe15.SocialApp.ui.post

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.viewmodel.CreatePostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onPickImages: () -> Unit,
    onShowAudiencePicker: () -> Unit
) {
    var postText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val isPosting by viewModel.isPosting.collectAsState()
    val postSuccess by viewModel.postSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()

    // Dès que la publication réussit, on revient en arrière automatiquement
    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            onBack()
        }
    }

    // Affiche l'erreur si la publication échoue
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
                        enabled = postText.isNotBlank() && !isPosting,
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
                Text(
                    text = "${selectedImages.size} image(s) sélectionnée(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPickImages, modifier = Modifier.weight(1f)) {
                    Text("Ajouter Photo")
                }
                OutlinedButton(onClick = onShowAudiencePicker, modifier = Modifier.weight(1f)) {
                    Text("Audience")
                }
            }
        }
    }
}