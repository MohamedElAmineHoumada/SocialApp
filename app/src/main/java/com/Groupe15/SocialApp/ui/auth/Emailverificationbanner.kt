package com.Groupe15.SocialApp.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun EmailVerificationBanner(
    onResend: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sent by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color(0xFF856404),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Email non vérifié",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF856404),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vérifiez votre boîte mail et cliquez sur le lien de confirmation pour activer votre compte.",
                color = Color(0xFF856404),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onResend()
                        sent = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF856404)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (sent) "Renvoyé !" else "Renvoyer l'email", fontSize = 12.sp)
                }
                Button(
                    onClick = onVerified,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF856404)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("J'ai vérifié", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}