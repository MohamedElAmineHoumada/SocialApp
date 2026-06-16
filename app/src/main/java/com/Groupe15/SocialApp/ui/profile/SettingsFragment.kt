package com.Groupe15.SocialApp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    SettingsScreen(
                        viewModel = authViewModel,
                        onBack = { findNavController().navigateUp() },
                        onShowToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                        onAccountDeleted = { requireActivity().finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onShowToast: (String) -> Unit,
    onAccountDeleted: () -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.authEvents.collect { event ->
            when (event) {
                is AuthViewModel.AuthEvent.PasswordResetSent -> onShowToast("Lien de réinitialisation envoyé")
                is AuthViewModel.AuthEvent.AccountDeleted -> onAccountDeleted()
                is AuthViewModel.AuthEvent.Error -> onShowToast(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SettingsSectionTitle(stringResource(R.string.account_section))
            SettingsCard {
                SettingsSwitchItem(
                    title = stringResource(R.string.private_account),
                    subtitle = stringResource(R.string.private_account_desc),
                    icon = Icons.Default.Lock,
                    checked = user?.isPrivate ?: false,
                    onCheckedChange = { viewModel.updatePrivacyStatus(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SettingsClickItem(
                    title = stringResource(R.string.reset_password),
                    icon = Icons.Default.Lock, // Utilisation de Lock faute de Key dans le pack de base
                    onClick = { showResetDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle(stringResource(R.string.preferences_section))
            SettingsCard {
                SettingsClickItem(
                    title = stringResource(R.string.change_language),
                    icon = Icons.Default.Settings, // Utilisation de Settings faute de Language dans le pack de base
                    trailingText = getCurrentLanguageName(),
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SettingsClickItem(
                    title = stringResource(R.string.notifications),
                    icon = Icons.Default.Notifications,
                    onClick = { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle(stringResource(R.string.danger_zone), color = Color.Red)
            SettingsCard(containerColor = Color(0xFFFFEBEE)) {
                SettingsClickItem(
                    title = stringResource(R.string.delete_account),
                    subtitle = stringResource(R.string.delete_account_desc),
                    icon = Icons.Default.Delete,
                    iconColor = Color.Red,
                    textColor = Color.Red,
                    onClick = { showDeleteDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "AFN v2.4.0",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_password)) },
            text = { Text(stringResource(R.string.reset_password_confirm, user?.email ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    user?.email?.let { viewModel.resetPassword(it) }
                    showResetDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount()
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.delete), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
    }
}

@Composable
fun SettingsSectionTitle(title: String, color: Color = Color.Gray) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(
    containerColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
        content = content
    )
}

@Composable
fun SettingsClickItem(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    trailingText: String? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = textColor)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = textColor)
        }
        if (trailingText != null) Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LanguageSelectionDialog(onDismiss: () -> Unit) {
    val languages = listOf("English" to "en", "Français" to "fr", "Español" to "es", "العربية" to "ar", "中文" to "zh")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_language)) },
        text = {
            Column {
                languages.forEach { (name, code) ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val appLocale = LocaleListCompat.forLanguageTags(code)
                                AppCompatDelegate.setApplicationLocales(appLocale)
                                onDismiss()
                            }
                            .padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun getCurrentLanguageName(): String {
    val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
    val language = currentLocale?.language ?: java.util.Locale.getDefault().language
    return when (language) {
        "en" -> "English"
        "fr" -> "Français"
        "es" -> "Español"
        "ar" -> "العربية"
        "zh" -> "中文"
        else -> "Français"
    }
}