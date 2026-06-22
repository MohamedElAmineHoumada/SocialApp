package com.Groupe15.SocialApp.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.models.Call
import com.Groupe15.SocialApp.models.CallStatus
import com.Groupe15.SocialApp.viewmodel.CallViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallViewModel,
    currentUserId: String,
    onBack: () -> Unit,
    onCallClick: (Call) -> Unit
) {
    val history by viewModel.callHistory.collectAsState()

    LaunchedEffect(currentUserId) {
        viewModel.loadCallHistory(currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique d'appels") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "Aucun historique d'appel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(history) { call ->
                    CallHistoryItem(
                        call = call,
                        currentUserId = currentUserId,
                        onClick = { onCallClick(call) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(call: Call, currentUserId: String, onClick: () -> Unit) {
    val isOutgoing = call.callerId == currentUserId
    val otherPartyName = if (isOutgoing) call.receiverName else call.callerName
    val otherPartyAvatar = if (isOutgoing) call.receiverAvatar else call.callerAvatar
    
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateString = sdf.format(call.timestamp.toDate())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = otherPartyAvatar.ifEmpty {
                "https://ui-avatars.com/api/?name=$otherPartyName&background=6C47FF&color=fff"
            },
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = otherPartyName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when {
                    isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                    call.status == CallStatus.MISSED.name || call.status == CallStatus.DECLINED.name -> Icons.AutoMirrored.Filled.CallMissed
                    else -> Icons.AutoMirrored.Filled.CallReceived
                }
                val iconColor = if (call.status == CallStatus.MISSED.name || call.status == CallStatus.DECLINED.name) Color.Red else Color.Gray
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateString,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        IconButton(onClick = onClick) {
            Icon(
                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Call Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
