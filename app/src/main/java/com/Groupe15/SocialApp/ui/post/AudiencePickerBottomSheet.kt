package com.Groupe15.SocialApp.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.ui.theme.SocialAppTheme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AudiencePickerBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AudiencePickerBottomSheet"
    }

    var onAudienceSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SocialAppTheme {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        AudiencePickerScreen(
                            onSelected = {
                                onAudienceSelected?.invoke(it)
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun getTheme() = R.style.BottomSheetDialogTheme
}

@Composable
fun AudiencePickerScreen(onSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Who can see your post?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        AudienceOption(
            title = "Public",
            subtitle = "Anyone can see this post",
            iconRes = R.drawable.ic_globe,
            onClick = { onSelected("Public") }
        )

        AudienceOption(
            title = "Friends",
            subtitle = "Only your followers",
            iconRes = R.drawable.ic_people,
            onClick = { onSelected("Friends") }
        )

        AudienceOption(
            title = "Only Me",
            subtitle = "Only you can see this",
            iconRes = R.drawable.ic_lock,
            onClick = { onSelected("Only Me") }
        )
    }
}

@Composable
fun AudienceOption(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
