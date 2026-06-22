package com.Groupe15.SocialApp.ui.notifications

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

    // Global toggle
    var pauseAll by remember { mutableStateOf(prefs.getBoolean("pause_all", false)) }

    // Push notification types (mapped to our notification types: like, comment, follow_request, follow_accept)
    var notifyLikes by remember { mutableStateOf(prefs.getBoolean("notify_likes", true)) }
    var notifyComments by remember { mutableStateOf(prefs.getBoolean("notify_comments", true)) }
    var notifyFollowRequests by remember { mutableStateOf(prefs.getBoolean("notify_follow_requests", true)) }
    var notifyFollowAccepted by remember { mutableStateOf(prefs.getBoolean("notify_follow_accepted", true)) }
    var notifyMessages by remember { mutableStateOf(prefs.getBoolean("notify_messages", true)) }

    fun save(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Section : Notifications push ──────────────────────────────
            SectionHeader(stringResource(R.string.push_notifications))

            // Pause toutes les notifications
            NotifToggleItem(
                title = stringResource(R.string.pause_all),
                subtitle = stringResource(R.string.pause_all_desc),
                checked = pauseAll,
                onCheckedChange = { pauseAll = it; save("pause_all", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Section : Publications & interactions ─────────────────────
            SectionHeader(stringResource(R.string.posts_interactions))

            NotifToggleItem(
                title = stringResource(R.string.likes),
                subtitle = stringResource(R.string.likes_desc),
                checked = notifyLikes && !pauseAll,
                enabled = !pauseAll,
                onCheckedChange = { notifyLikes = it; save("notify_likes", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            NotifToggleItem(
                title = stringResource(R.string.comments_title),
                subtitle = stringResource(R.string.comments_desc),
                checked = notifyComments && !pauseAll,
                enabled = !pauseAll,
                onCheckedChange = { notifyComments = it; save("notify_comments", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Section : Abonnements & abonnés ──────────────────────────
            SectionHeader(stringResource(R.string.follow_sub_section))

            NotifToggleItem(
                title = stringResource(R.string.follow_requests),
                subtitle = stringResource(R.string.follow_requests_desc),
                checked = notifyFollowRequests && !pauseAll,
                enabled = !pauseAll,
                onCheckedChange = { notifyFollowRequests = it; save("notify_follow_requests", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            NotifToggleItem(
                title = stringResource(R.string.follow_accepted),
                subtitle = stringResource(R.string.follow_accepted_desc),
                checked = notifyFollowAccepted && !pauseAll,
                enabled = !pauseAll,
                onCheckedChange = { notifyFollowAccepted = it; save("notify_follow_accepted", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Section : Messages ────────────────────────────────────────
            SectionHeader(stringResource(R.string.nav_messages))

            NotifToggleItem(
                title = stringResource(R.string.nav_messages),
                subtitle = stringResource(R.string.messages_desc),
                checked = notifyMessages && !pauseAll,
                enabled = !pauseAll,
                onCheckedChange = { notifyMessages = it; save("notify_messages", it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NotifToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}
