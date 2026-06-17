package com.Groupe15.SocialApp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onShowToast: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListItem(
                headlineContent = { Text("Notification") },
                supportingContent = { Text("Gérer les alertes de l'application") },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Divider()
            ListItem(
                headlineContent = { Text("Sécurité & Confidentialité") },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Divider()

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onAccountDeleted,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Supprimer le compte", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}