package com.Groupe15.SocialApp.ui.messages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.Message
import com.Groupe15.SocialApp.models.MessageType
import com.Groupe15.SocialApp.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

private val VioletStart = Color(0xFF6C47FF)
private val VioletEnd   = Color(0xFF4B6FE4)
private val OnlineGreen = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentUserId: String,
    otherUserId: String,
    userName: String,
    userAvatar: String = "",
    onBack: () -> Unit,
    onCall: (Boolean, String) -> Unit,
    onShowToast: (String) -> Unit,
    onHistoryClick: () -> Unit = {},
    callViewModel: com.Groupe15.SocialApp.viewmodel.CallViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val messages by viewModel.messages.observeAsState(emptyList())
    val currentUserDoc by viewModel.currentUser.observeAsState()
    val otherUser by viewModel.otherUser.observeAsState()
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showGifPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            onShowToast("Permissions accordées. Lancement de l'appel...")
        } else {
            onShowToast("Permissions micro/caméra refusées")
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.sendMessage("Sent an image", type = MessageType.IMAGE, imageUrl = it.toString())
        }
    }

    // Initialisation du chat
    LaunchedEffect(currentUserId, otherUserId) {
        viewModel.initChat(currentUserId, otherUserId)
    }

    // reverseLayout=true → index 0 = dernier message (en bas)
    // donc on scroll à 0 à chaque nouveau message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val displayAvatar = otherUser?.profileImageUrl ?: userAvatar
            ChatTopBar(
                userName = userName,
                userAvatar = displayAvatar,
                onBack = onBack,
                onCall = { isVideo, avatar ->
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.CAMERA
                        )
                    )
                    callViewModel.startCall(
                        callerId = currentUserId,
                        callerName = currentUserDoc?.username ?: "Moi",
                        callerAvatar = currentUserDoc?.profileImageUrl ?: "",
                        receiverId = otherUserId,
                        receiverName = userName,
                        receiverAvatar = avatar,
                        isVideo = isVideo
                    )
                    onCall(isVideo, avatar)
                },
                onHistoryClick = onHistoryClick,
                onDeleteChat = {
                    showDeleteDialog = true
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = textState,
                onTextChange = { textState = it },
                onSend = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                    }
                },
                onAddClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onEmojiClick = { 
                    Log.d("ChatInput", "Button clicked: Emoji")
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showEmojiPicker = true
                },
                onGifClick = { 
                    Log.d("ChatInput", "Button clicked: GIF")
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showGifPicker = true
                },
                onMicClick = { 
                    if (!isRecording) {
                        Log.d("ChatInput", "Starting recording")
                        isRecording = true
                        onShowToast("Enregistrement démarré...")
                    } else {
                        Log.d("ChatInput", "Stopping recording")
                        isRecording = false
                        viewModel.sendMessage("Voice message", type = MessageType.VOICE)
                        onShowToast("Message vocal envoyé")
                    }
                },
                isRecording = isRecording,
                onShowToast = onShowToast
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 12.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isCurrentUser = message.senderId == currentUserId
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la conversation") },
            text = { Text("Êtes-vous sûr de vouloir supprimer cette conversation ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteChat {
                            onBack()
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

    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            EmojiPickerContent(
                onEmojiSelected = { emoji ->
                    textState += emoji
                    showEmojiPicker = false
                }
            )
        }
    }

    if (showGifPicker) {
        Log.d("ChatScreen", "Rendering ModalBottomSheet")
        ModalBottomSheet(
            onDismissRequest = { 
                Log.d("ChatScreen", "ModalBottomSheet onDismissRequest")
                showGifPicker = false 
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            GifPickerContent(
                onGifSelected = { gifUrl ->
                    Log.d("ChatScreen", "GIF selected: $gifUrl")
                    viewModel.sendMessage("Sent a GIF", type = MessageType.GIF, imageUrl = gifUrl)
                    showGifPicker = false
                }
            )
        }
    }
}

// ── Emoji Picker Content ──────────────────────────────────────────────────
@Composable
fun EmojiPickerContent(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf(
        "😊", "😂", "🤣", "❤️", "😍", "😒", "😭", "😘", "😩", "😔",
        "😎", "🤓", "🥳", "😡", "😱", "🤔", "🤫", "🤥", "😴", "🤢",
        "🔥", "✨", "⭐", "🌈", "🍎", "🍕", "🍔", "🍦", "⚽", "🏀",
        "🚗", "✈️", "🏠", "💻", "📱", "💡", "🎉", "🎁", "🚩", "🏁"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Text(
            text = "Choose an Emoji",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 28.sp)
                }
            }
        }
    }
}

// ── GIF Picker Content ────────────────────────────────────────────────────
@Composable
fun GifPickerContent(onGifSelected: (String) -> Unit) {
    val gifs = listOf(
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueGZ3bmZ6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKSjP6VT5fEdVMA/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueGZ3bmZ6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/l0HlIDZpxk4T6B2qA/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueGZ3bmZ6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKVUn7iM8FMEU24/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueGZ3bmZ6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKMGpxvfSdfV9S0/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueGZ3bmZ6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/l41lTfuxS8pXhYy08/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJueGZ3bmZ6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6Z3B6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKDkDbIDJieKbVm/giphy.gif"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Text(
            text = "Choose a GIF",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(gifs) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onGifSelected(url) },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────
@Composable
private fun ChatTopBar(
    userName: String,
    userAvatar: String,
    onBack: () -> Unit,
    onCall: (Boolean, String) -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                AsyncImage(
                    model = userAvatar.ifEmpty {
                        "https://ui-avatars.com/api/?name=$userName&background=6C47FF&color=fff"
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Active now",
                        color = OnlineGreen,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { onCall(false, userAvatar) }) {
                    Icon(Icons.Default.Call, contentDescription = "Appel vocal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onCall(true, userAvatar) }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Appel vidéo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Historique d'appels") },
                            onClick = {
                                showMenu = false
                                onHistoryClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer la conversation") },
                            onClick = {
                                showMenu = false
                                onDeleteChat()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────
@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean) {
    val timeLabel = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate())
    }

    val containerColor = if (isCurrentUser) Color.Transparent else Color(0xFFE9E9EB)
    val contentColor = if (isCurrentUser) Color.White else Color(0xFF1C1C1E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            val bubbleShape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isCurrentUser) 20.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 20.dp
            )

            Surface(
                shape = bubbleShape,
                color = containerColor,
                modifier = Modifier.then(
                    if (isCurrentUser) Modifier.background(
                        Brush.linearGradient(listOf(VioletStart, VioletEnd)),
                        bubbleShape
                    ) else Modifier
                )
            ) {
                when (message.type) {
                    MessageType.IMAGE, MessageType.GIF -> {
                        ImageMessageBubbleContent(
                            imageUrl = message.imageUrl,
                            caption = message.text,
                            isCurrentUser = isCurrentUser
                        )
                    }
                    MessageType.VOICE -> {
                        VoiceMessageBubbleContent(isCurrentUser = isCurrentUser)
                    }
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            color = contentColor,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Heure et Statut en dessous de la bulle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = timeLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = VioletStart,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceMessageBubbleContent(isCurrentUser: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = if (isCurrentUser) Color.White else Color(0xFF1C1C1E),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(15) {
                val height = (4..16).random().dp
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(height)
                        .background(
                            if (isCurrentUser) Color.White.copy(alpha = 0.8f) else Color(0xFF1C1C1E).copy(alpha = 0.3f),
                            CircleShape
                        )
                        .padding(horizontal = 1.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "0:12",
            color = if (isCurrentUser) Color.White else Color(0xFF1C1C1E),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ImageMessageBubbleContent(
    imageUrl: String,
    caption: String,
    isCurrentUser: Boolean
) {
    Column {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth
        )
        if (caption.isNotBlank() && caption != "Sent an image" && caption != "Sent a GIF") {
            Text(
                text = caption,
                color = if (isCurrentUser) Color.White else Color(0xFF1C1C1E),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
            )
        }
    }
}


// ── Input Bar ─────────────────────────────────────────────────────────────
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAddClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onGifClick: () -> Unit,
    onMicClick: () -> Unit,
    onShowToast: (String) -> Unit,
    isRecording: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add button (Plus)
                var showAddOptions by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showAddOptions = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = VioletStart,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showAddOptions,
                        onDismissRequest = { showAddOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Images") },
                            onClick = {
                                showAddOptions = false
                                onAddClick()
                            },
                            leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Files") },
                            onClick = {
                                showAddOptions = false
                                onShowToast("Files feature coming soon")
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                    }
                }

                // Input field group (Emoji + TextField + GIF)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEmojiClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = "Emoji",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (isRecording) {
                            Text(
                                "Enregistrement...",
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            TextField(
                                value = text,
                                onValueChange = onTextChange,
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "Type a message...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor   = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                                    cursorColor             = VioletStart
                                ),
                                singleLine = false,
                                maxLines = 4
                            )
                        }

                        IconButton(onClick = onGifClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Gif,
                                contentDescription = "GIF",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send or Mic button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (text.isNotBlank())
                                Brush.linearGradient(listOf(VioletStart, VioletEnd))
                            else if (isRecording)
                                Brush.linearGradient(listOf(Color.Red, Color(0xFFFF5252)))
                            else
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                        )
                        .clickable {
                            if (text.isNotBlank()) onSend() else onMicClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (text.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
