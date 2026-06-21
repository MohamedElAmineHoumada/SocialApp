package com.Groupe15.SocialApp.ui.profile

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onShowToast: () -> Unit,
    onAccountDeleted: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var isDarkMode by remember {
        mutableStateOf(prefs.getBoolean("dark_mode", false))
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Dialog confirmation suppression
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le compte") },
            text = { Text("Cette action est irréversible. Toutes vos données seront supprimées.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                        onAccountDeleted()
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }

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
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Dark mode
            ListItem(
                headlineContent = { Text("Mode sombre") },
                supportingContent = { Text(if (isDarkMode) "Activé" else "Désactivé") },
                leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            isDarkMode = enabled
                            prefs.edit().putBoolean("dark_mode", enabled).apply()
                            AppCompatDelegate.setDefaultNightMode(
                                if (enabled) AppCompatDelegate.MODE_NIGHT_YES
                                else AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Notifications
            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text("Gérer les alertes") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Confidentialité
            ListItem(
                headlineContent = { Text("Sécurité & Confidentialité") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Déconnexion
            ListItem(
                headlineContent = { Text("Se déconnecter") },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
                colors = ListItemDefaults.colors(
                    headlineColor = MaterialTheme.colorScheme.error
                ),
                trailingContent = {
                    TextButton(onClick = {
                        viewModel.signOut()
                        onLoggedOut()
                    }) {
                        Text("Déconnexion", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Suppression compte
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Supprimer le compte", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}