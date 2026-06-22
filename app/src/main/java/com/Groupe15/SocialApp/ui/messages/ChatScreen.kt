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
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
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
import com.Groupe15.SocialApp.util.AudioRecorder
import com.Groupe15.SocialApp.util.AudioPlayer
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
    onCall: (Boolean) -> Unit,
    onShowToast: (String) -> Unit
) {
    val messages by viewModel.messages.observeAsState(emptyList())
    val otherUser by viewModel.otherUser.observeAsState()
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer(context) }

    LaunchedEffect(Unit) {
        viewModel.setAudioRecorder(recorder)
    }

    var showGifPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onShowToast("Camera captured (uploading logic coming soon)")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (recordAudioGranted) {
            onShowToast("Permission micro accordée. Appuyez à nouveau pour enregistrer.")
        } else if (cameraGranted) {
            cameraLauncher.launch(null)
        } else if (locationGranted) {
            onShowToast("Permission localisation accordée.")
        } else if (isRecording) {
            isRecording = false
            onShowToast("Permission micro requise")
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle single permission result if needed
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.sendMessage("Sent an image", type = MessageType.IMAGE, imageUrl = it.toString())
        }
    }

    // Initialisation du chat et marquage comme lu
    LaunchedEffect(currentUserId, otherUserId, messages) {
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
            ChatTopBar(
                userName = userName,
                userAvatar = userAvatar,
                isOnline = otherUser?.isOnline ?: false,
                onBack = onBack,
                onCall = onCall
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
                onCameraClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        cameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
                    }
                },
                onLocationClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        onShowToast("Location feature coming soon")
                    } else {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION))
                    }
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
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!isRecording) {
                        if (hasPermission) {
                            if (viewModel.startRecording(context.cacheDir)) {
                                isRecording = true
                            } else {
                                onShowToast("Erreur lors du démarrage de l'enregistrement")
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                        }
                    } else {
                        isRecording = false
                        viewModel.stopRecording { file ->
                            file?.let { viewModel.sendVoiceMessage(it) }
                        }
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
                    isCurrentUser = message.senderId == currentUserId,
                    onPlayAudio = { url -> player.playFile(url) }
                )
            }
        }
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
    isOnline: Boolean,
    onBack: () -> Unit,
    onCall: (Boolean) -> Unit
) {
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

                Box {
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
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(OnlineGreen)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isOnline) "Active now" else "Offline",
                        color = if (isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { onCall(false) }) {
                    Icon(Icons.Default.Call, contentDescription = "Appel vocal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onCall(true) }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Appel vidéo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Plus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        }
    }
}

@Composable
fun MessageBubble(
    message: Message, 
    isCurrentUser: Boolean,
    onPlayAudio: (String) -> Unit = {}
) {
    val timeLabel = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            when (message.type) {
                MessageType.IMAGE, MessageType.GIF -> {
                    ImageMessageBubble(
                        imageUrl = message.imageUrl,
                        caption = message.text,
                        isCurrentUser = isCurrentUser
                    )
                }
                MessageType.VOICE -> {
                    VoiceMessageBubble(
                        isCurrentUser = isCurrentUser,
                        audioUrl = message.imageUrl,
                        onPlay = onPlayAudio
                    )
                }
                MessageType.TEXT -> {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isCurrentUser) 18.dp else 4.dp,
                                    bottomEnd = if (isCurrentUser) 4.dp else 18.dp
                                )
                            )
                            .background(
                                if (isCurrentUser)
                                    Brush.linearGradient(listOf(VioletStart, VioletEnd))
                                else
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = if (isCurrentUser) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(2.dp))
                    if (message.isRead) {
                        Text(
                            text = "Seen",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (message.isDelivered) {
                        Icon(
                            imageVector = Icons.Outlined.DoneAll,
                            contentDescription = "Delivered",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Sent",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Voice Message Bubble ──────────────────────────────────────────────────
@Composable
private fun VoiceMessageBubble(
    isCurrentUser: Boolean,
    audioUrl: String,
    onPlay: (String) -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCurrentUser)
                    Brush.linearGradient(listOf(VioletStart, VioletEnd))
                else
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { 
                isPlaying = !isPlaying
                onPlay(audioUrl) 
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Waveform visualization
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(15) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height((4..16).random().dp)
                        .background(
                            if (isCurrentUser) Color.White.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}

// ── Image Bubble ──────────────────────────────────────────────────────────
@Composable
private fun ImageMessageBubble(
    imageUrl: String,
    caption: String,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrentUser)
                    Brush.linearGradient(listOf(VioletStart, VioletEnd))
                else
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
            )
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentScale = ContentScale.Crop
        )
        if (caption.isNotBlank()) {
            Text(
                text = caption,
                color = if (isCurrentUser) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
    onCameraClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
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
                            text = { Text("Camera") },
                            onClick = {
                                showAddOptions = false
                                onCameraClick()
                            },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Location") },
                            onClick = {
                                showAddOptions = false
                                onLocationClick()
                            },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Files") },
                            onClick = {
                                showAddOptions = false
                                onShowToast("Files feature coming soon")
                            },
                            leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) }
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
