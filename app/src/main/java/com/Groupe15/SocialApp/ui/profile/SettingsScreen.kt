package com.Groupe15.SocialApp.ui.profile

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.util.LanguageManager
import com.Groupe15.SocialApp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onShowToast: () -> Unit,
    onAccountDeleted: () -> Unit,
    onLoggedOut: () -> Unit = {},
    onNotifications: () -> Unit = {}   // ← nouveau paramètre
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var isDarkMode by remember {
        mutableStateOf(prefs.getBoolean("dark_mode", false))
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ✅ NOUVEAU : état du dialog de sélection de langue
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember {
        mutableStateOf(LanguageManager.getSavedLanguage(context) ?: LanguageManager.LANG_FRENCH)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                        onAccountDeleted()
                    }
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ✅ NOUVEAU : dialog de sélection de langue
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.change_language)) },
            text = {
                Column {
                    LanguageOption(
                        label = "Français",
                        code = LanguageManager.LANG_FRENCH,
                        selected = currentLanguage == LanguageManager.LANG_FRENCH,
                        onSelect = {
                            currentLanguage = LanguageManager.LANG_FRENCH
                            LanguageManager.setLanguage(context, LanguageManager.LANG_FRENCH)
                            showLanguageDialog = false
                        }
                    )
                    LanguageOption(
                        label = "English",
                        code = LanguageManager.LANG_ENGLISH,
                        selected = currentLanguage == LanguageManager.LANG_ENGLISH,
                        onSelect = {
                            currentLanguage = LanguageManager.LANG_ENGLISH
                            LanguageManager.setLanguage(context, LanguageManager.LANG_ENGLISH)
                            showLanguageDialog = false
                        }
                    )
                    LanguageOption(
                        label = "العربية",
                        code = LanguageManager.LANG_ARABIC,
                        selected = currentLanguage == LanguageManager.LANG_ARABIC,
                        onSelect = {
                            currentLanguage = LanguageManager.LANG_ARABIC
                            LanguageManager.setLanguage(context, LanguageManager.LANG_ARABIC)
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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

            // ✅ NOUVEAU : Langue de l'application
            ListItem(
                headlineContent = { Text(stringResource(R.string.change_language)) },
                supportingContent = { Text(languageDisplayName(currentLanguage)) },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Notifications ← maintenant cliquable
            ListItem(
                headlineContent = { Text(stringResource(R.string.notifications)) },
                supportingContent = { Text("Gérer les alertes") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier.clickable { onNotifications() }
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
                Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    code: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

/** Nom affiché pour chaque code de langue dans le sous-titre du ListItem. */
private fun languageDisplayName(code: String): String = when (code) {
    LanguageManager.LANG_FRENCH -> "Français"
    LanguageManager.LANG_ENGLISH -> "English"
    LanguageManager.LANG_ARABIC -> "العربية"
    else -> "Français"
}