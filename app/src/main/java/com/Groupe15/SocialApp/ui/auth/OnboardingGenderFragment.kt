package com.Groupe15.SocialApp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingGenderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    OnboardingGenderScreen(
                        onBack = { findNavController().navigateUp() },
                        onContinue = { findNavController().navigate(R.id.action_onboardingGender_to_interests) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingGenderScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var selectedGender by remember { mutableStateOf<String?>(null) } // "male" or "female"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Retour")
            }
            Image(
                painter = painterResource(id = R.drawable.logo_afn),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress
        LinearProgressIndicator(
            progress = { 0.85f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 8.dp),
            color = Color(0xFF6200EE),
            trackColor = Color(0xFFE0E0E0),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Étape 3 sur 3", color = Color(0xFF6200EE), fontSize = 12.sp)
            Text(text = "85% complété", color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quel est votre genre ?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = "Choisissez le genre qui vous définit le mieux.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Gender Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenderCard(
                label = "Femme",
                iconRes = R.drawable.ic_female, // Assumé existant basé sur le code précédent
                isSelected = selectedGender == "female",
                onClick = { selectedGender = "female" },
                modifier = Modifier.weight(1f)
            )
            GenderCard(
                label = "Homme",
                iconRes = R.drawable.ic_male, // Assumé existant basé sur le code précédent
                isSelected = selectedGender == "male",
                onClick = { selectedGender = "male" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            enabled = selectedGender != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EE),
                disabledContainerColor = Color(0xFFE0E0E0)
            )
        ) {
            Text(text = "Continuer →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GenderCard(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF6200EE)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF3E5F5) else Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isSelected) Color(0xFF6200EE) else Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF6200EE) else Color.Gray
            )
        }
    }
}
