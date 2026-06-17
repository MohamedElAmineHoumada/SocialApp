package com.Groupe15.SocialApp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Groupe15.SocialApp.viewmodel.LoginViewModel

// Couleurs du thème, reprises de la maquette
private val GradientStart = Color(0xFF6C63FF)
private val GradientEnd = Color(0xFF8E7CFF)
private val FieldBackground = Color(0xFFF4F4F8)
private val SubtitleGray = Color(0xFF8A8A9A)

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

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo : carré arrondi avec dégradé
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AFN",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2A2A3A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Welcome back to your creative community.",
            fontSize = 13.sp,
            color = SubtitleGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Champ Email
        StyledTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email address"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Champ Password
        StyledTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPasswordClick) {
                Text("Forgot Password?", fontSize = 13.sp, color = GradientStart)
            }
        }

        if (state is AuthState.Error) {
            Text(
                text = (state as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bouton Login avec dégradé
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                .clickable(enabled = state !is AuthState.Loading) {
                    viewModel.login(email, password)
                },
            contentAlignment = Alignment.Center
        ) {
            if (state is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Séparateur "Continue with"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFE5E5EA))
            Text(
                text = "  Continue with  ",
                fontSize = 12.sp,
                color = SubtitleGray
            )
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFE5E5EA))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons sociaux
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SocialButton(
                text = "Google",
                modifier = Modifier.weight(1f),
                onClick = onGoogleClick
            )
            SocialButton(
                text = "Facebook",
                modifier = Modifier.weight(1f),
                onClick = onFacebookClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Lien vers inscription
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", fontSize = 13.sp, color = SubtitleGray)
            Text(
                "Sign Up",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GradientStart,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = SubtitleGray, fontSize = 14.sp) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (isPassword) {
            KeyboardOptions(keyboardType = KeyboardType.Password)
        } else {
            KeyboardOptions(keyboardType = KeyboardType.Email)
        },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldBackground,
            unfocusedContainerColor = FieldBackground,
            disabledContainerColor = FieldBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}

@Composable
private fun SocialButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FieldBackground)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 14.sp, color = Color(0xFF2A2A3A))
    }
}