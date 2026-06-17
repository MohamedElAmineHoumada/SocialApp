package com.Groupe15.SocialApp.ui.auth

import com.Groupe15.SocialApp.ui.auth.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Groupe15.SocialApp.viewmodel.RegisterViewModel
import com.Groupe15.SocialApp.viewmodel.*
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterClick: (String, String, String) -> Unit,
    onLoginClick: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.observeAsState(AuthState.Idle)
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Créer un compte", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nom d'utilisateur") },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.register(email, password, username) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading &&
                    email.isNotBlank() && password.isNotBlank() && username.isNotBlank()
        ) {
            if (state is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("S'inscrire")
            }
        }

        if (state is AuthState.Error) {
            Text(
                text = (state as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onLoginClick) {
            Text("Déjà un compte ? Se connecter")
        }
    }
}