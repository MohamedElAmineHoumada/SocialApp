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
import com.Groupe15.SocialApp.ui.theme.SocialAppTheme
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
                SocialAppTheme {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        val posts by viewModel.posts.observeAsState(emptyList())
                        val post = posts.find { it.postId == postId }
                        if (post != null) {
                            ShareScreen(
                                viewModel = viewModel,
                                post = post,
                                onDismiss = { dismiss() }
                            )
                        } else {
                            // Post introuvable dans la liste actuelle (ex: changement d'onglet) : on ferme proprement
                            dismiss()
                        }
                    }
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
    post: com.Groupe15.SocialApp.models.Post,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recentContacts by viewModel.recentContacts.observeAsState(emptyList())
    val postId = post.postId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Partager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fermer",
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Partager en story — déjà fonctionnel
        ShareActionButton(
            title = "Partager en story",
            iconRes = R.drawable.ic_add_circle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            onClick = {
                viewModel.shareToStory(post)
                Toast.makeText(context, "Ajouté à la story", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ✅ MODIFIÉ : invite à choisir un contact ci-dessous au lieu de "Bientôt disponible"
        ShareActionButton(
            title = "Envoyer en message",
            iconRes = R.drawable.ic_send,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = true,
            onClick = {
                if (recentContacts.isEmpty()) {
                    Toast.makeText(context, "Aucun contact disponible", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Choisis un contact ci-dessous", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CONTACTS RÉCENTS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (recentContacts.isEmpty()) {
            Text(
                text = "Aucun contact pour l'instant",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(recentContacts) { user ->
                    // ✅ MODIFIÉ : envoie réellement le lien du post à ce contact
                    RecentContactItem(user = user, onClick = {
                        viewModel.sendPostToChat(user.id, post)
                        Toast.makeText(context, "Envoyé à ${user.displayName.ifBlank { user.username }}", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    })
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActionIconItem(label = "Copier le lien", iconRes = R.drawable.ic_link, onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Post Link", "https://socialapp.com/post/$postId")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Lien copié", Toast.LENGTH_SHORT).show()
                onDismiss()
            })
            ActionIconItem(label = "Enregistrer", iconRes = R.drawable.ic_bookmark_outline, onClick = {
                viewModel.toggleSavePost(postId)
                Toast.makeText(context, "Publication enregistrée", Toast.LENGTH_SHORT).show()
                onDismiss()
            })
            ActionIconItem(label = "Email", iconRes = android.R.drawable.ic_dialog_email, onClick = {
                Toast.makeText(context, "Bientôt disponible", Toast.LENGTH_SHORT).show()
                onDismiss()
            })
            ActionIconItem(label = "Plus", iconRes = R.drawable.ic_more_horizontal, onClick = {
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "https://socialapp.com/post/$postId")
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Partager via"))
                onDismiss()
            })
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
        border = if (border) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (containerColor == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = if (contentColor == MaterialTheme.colorScheme.onPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            style = MaterialTheme.typography.bodySmall,
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
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}