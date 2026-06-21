package com.Groupe15.SocialApp.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val VioletStart = Color(0xFF6C47FF)
private val VioletEnd = Color(0xFF4B6FE4)

@Composable
fun CallScreen(
    userName: String,
    userAvatar: String,
    isVideoCall: Boolean = false,
    onHangUp: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(!isVideoCall) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // Background Image with Blur (Simulated)
        AsyncImage(
            model = userAvatar.ifEmpty { "https://ui-avatars.com/api/?name=$userName&background=6C47FF&color=fff" },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.3f,
            contentScale = ContentScale.Crop
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // User Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 60.dp)
            ) {
                AsyncImage(
                    model = userAvatar.ifEmpty { "https://ui-avatars.com/api/?name=$userName&background=6C47FF&color=fff" },
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isVideoCall) "Appel vidéo en cours..." else "Appel vocal en cours...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Button
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color.Black else Color.White
                    )
                }

                // Hang Up Button
                IconButton(
                    onClick = onHangUp,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Raccrocher",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Video Toggle Button
                IconButton(
                    onClick = { isVideoOff = !isVideoOff },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isVideoOff) Color.White else Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = if (isVideoOff) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
