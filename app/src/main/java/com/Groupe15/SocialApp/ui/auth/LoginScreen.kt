package com.Groupe15.SocialApp.ui.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.LoginViewModel

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginClick: (String, String) -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.observeAsState(AuthState.Idle)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo and Brand
        Image(
            painter = painterResource(id = R.drawable.logo_afn),
            contentDescription = "Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "AFN",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF6C47FF),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ravi de vous revoir ! Connectez-vous pour continuer.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Form
        CustomLoginTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Adresse e-mail",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomLoginTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mot de passe",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(email, password)
                    }
                }
            )
        )


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text("Mot de passe oublié ?", color = Color(0xFF6C47FF), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            enabled = state !is AuthState.Loading && email.isNotBlank() && password.isNotBlank()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6C47FF), Color(0xFF9D47FF))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Se connecter", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Ou continuer avec", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SocialLoginButton(
                text = "Google",
                icon = R.drawable.ic_app_logo,
                modifier = Modifier.weight(1f),
                onClick = onGoogleClick
            )
            SocialLoginButton(
                text = "Facebook",
                icon = R.drawable.ic_people,
                modifier = Modifier.weight(1f),
                onClick = onFacebookClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Pas encore de compte ? ", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = "S'inscrire",
                color = Color(0xFF6C47FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }

        if (state is AuthState.Error) {
            Text(
                text = (state as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CustomLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.Gray) },
        leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = Color.Gray) },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF0F2FA),
            unfocusedContainerColor = Color(0xFFF0F2FA),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
fun SocialLoginButton(text: String, icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Unspecified)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.Black, fontWeight = FontWeight.Medium)
        }
    }
}
