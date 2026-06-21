package com.Groupe15.SocialApp.ui.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterClick: (String, String, String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onSuccess: () -> Unit   // → navigue vers onboardingWelcome (géré dans MainActivity)
) {
    val state by viewModel.state.observeAsState(AuthState.Idle)

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // ✅ Afficher l'écran de confirmation email après inscription réussie,
    //    AVANT de passer à l'onboarding (DOB → Genre → Centres d'intérêt)
    var showEmailSent by remember { mutableStateOf(false) }

    // Quand l'inscription réussit → montrer l'écran "vérifiez votre email"
    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            showEmailSent = true
        }
    }

    // ── Écran "Vérifiez votre email" ────────────────────────────────────────
    if (showEmailSent) {
        EmailVerificationUI(
            email = email,
            onCodeEntered = { _ ->
                // L'utilisateur a cliqué sur le lien → on continue vers l'onboarding
                onSuccess()
            },
            onBack = {
                // Retour au formulaire d'inscription
                showEmailSent = false
                viewModel.resetState()
            }
        )
        return
    }

    // ── Formulaire d'inscription ─────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Top Bar – bouton Retour vers Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onLoginClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Retour", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logo & titre
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                text = "Rejoignez la communauté des curateurs\nmodernes.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Champ Nom complet
        Text(text = "Nom complet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        CustomRegisterTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "John Doe",
            leadingIcon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Champ Email
        Text(text = "Adresse e-mail", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        CustomRegisterTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "name@example.com",
            leadingIcon = Icons.Default.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Champ Mot de passe
        Text(text = "Mot de passe", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        CustomRegisterTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "********",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Case à cocher CGU
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acceptTerms,
                onCheckedChange = { acceptTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6C47FF))
            )
            Text(
                text = buildAnnotatedString {
                    append("J'accepte les ")
                    withStyle(style = SpanStyle(color = Color(0xFF6C47FF), fontWeight = FontWeight.Bold)) {
                        append("Conditions d'utilisation")
                    }
                    append(" et la ")
                    withStyle(style = SpanStyle(color = Color(0xFF6C47FF), fontWeight = FontWeight.Bold)) {
                        append("Politique de confidentialité")
                    }
                    append(".")
                },
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Bouton S'inscrire → appelle register() directement (Firebase envoie le vrai email)
        Button(
            onClick = { onRegisterClick(email, password, fullName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            enabled = state !is AuthState.Loading
                    && acceptTerms
                    && email.isNotBlank()
                    && password.isNotBlank()
                    && fullName.isNotBlank()
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
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("S'inscrire", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Inscription rapide via réseaux sociaux
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = "Inscription rapide", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SocialRegisterButton(
                text = "Google",
                icon = R.drawable.ic_app_logo,
                modifier = Modifier.weight(1f),
                onClick = onGoogleClick
            )
            SocialRegisterButton(
                text = "Facebook",
                icon = R.drawable.ic_people,
                modifier = Modifier.weight(1f),
                onClick = onFacebookClick
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Lien vers Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Vous avez déjà un compte ? ", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = "Se connecter",
                color = Color(0xFF6C47FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Message d'erreur
        if (state is AuthState.Error) {
            Text(
                text = (state as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    }
}

// ── Écran intermédiaire : confirmation de l'envoi du lien Firebase ───────────
// Affiché après inscription réussie, avant de passer à l'onboarding.
// L'utilisateur doit cliquer sur le lien reçu par email, puis appuyer sur
// "J'ai vérifié" pour continuer vers Date de naissance → Genre → Centres d'intérêt.
@Composable
fun EmailVerificationUI(
    email: String,
    onCodeEntered: (String) -> Unit,  // gardé pour compatibilité avec MainActivity
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF6C47FF)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Vérifiez votre e-mail",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Un lien de vérification a été envoyé à :\n$email\n\nCliquez sur le lien dans cet e-mail pour activer votre compte, puis revenez ici.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        // ✅ Après avoir cliqué sur le lien → continuer vers l'onboarding
        Button(
            onClick = { onCodeEntered("") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C47FF))
        ) {
            Text(
                "J'ai vérifié mon e-mail — Continuer",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Modifier l'e-mail", color = Color.Gray)
        }
    }
}

// ── Composants réutilisables ─────────────────────────────────────────────────

@Composable
fun CustomRegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text(placeholder, color = Color.Gray) },
        leadingIcon = {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = Color.Gray)
        },
        trailingIcon = {
            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF0F2FA),
            unfocusedContainerColor = Color(0xFFF0F2FA),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        visualTransformation = if (isPassword && !isPasswordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true
    )
}

@Composable
fun SocialRegisterButton(
    text: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.Black, fontWeight = FontWeight.Medium)
        }
    }
}