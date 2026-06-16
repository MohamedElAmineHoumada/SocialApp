package com.Groupe15.SocialApp.ui.feed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.User
import com.Groupe15.SocialApp.viewmodel.FeedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: FeedViewModel by activityViewModels()
    private var postId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString("post_id") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    ShareScreen(
                        viewModel = viewModel,
                        postId = postId,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(postId: String): ShareBottomSheet {
            return ShareBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("post_id", postId)
                }
            }
        }
    }
}

@Composable
fun ShareScreen(
    viewModel: FeedViewModel,
    postId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recentContacts by viewModel.recentContacts.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color.LightGray, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Share",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF5F5F5), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Share to Story Button
        ShareActionButton(
            title = "Share to Story",
            iconRes = R.drawable.ic_add_circle,
            containerColor = Color(0xFF6200EE),
            contentColor = Color.White,
            onClick = {
                val post = viewModel.posts.value?.find { it.postId == postId }
                if (post != null) {
                    viewModel.shareToStory(post)
                    Toast.makeText(context, "Added to story", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Send in Message Button
        ShareActionButton(
            title = "Send in Message",
            iconRes = R.drawable.ic_send,
            containerColor = Color.White,
            contentColor = Color.Black,
            border = true,
            onClick = {
                Toast.makeText(context, "Opening messages...", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RECENT CONTACTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(recentContacts) { user ->
                RecentContactItem(user = user, onClick = {
                    Toast.makeText(context, "Shared with ${user.displayName}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                })
            }
        }

        // Action Icons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActionIconItem(label = "Copy Link", iconRes = R.drawable.ic_link, onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Post Link", "https://socialapp.com/post/$postId")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                onDismiss()
            })
            ActionIconItem(label = "Save", iconRes = R.drawable.ic_bookmark_outline, onClick = {
                viewModel.toggleSavePost(postId)
                Toast.makeText(context, "Post saved", Toast.LENGTH_SHORT).show()
                onDismiss()
            })
            ActionIconItem(label = "Email", iconRes = android.R.drawable.ic_dialog_email, onClick = { onDismiss() })
            ActionIconItem(label = "More", iconRes = R.drawable.ic_more_horizontal, onClick = { onDismiss() })
        }
    }
}

@Composable
fun ShareActionButton(
    title: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    border: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (border) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (containerColor == Color.White) Color(0xFF6200EE) else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = if (contentColor == Color.White) Color.White else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RecentContactItem(user: User, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = user.profileImageUrl.ifEmpty { R.drawable.ic_default_avatar },
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = user.displayName,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
fun ActionIconItem(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFF0F2FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}
