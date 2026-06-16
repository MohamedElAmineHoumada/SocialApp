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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.models.Interest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingInterestsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    OnboardingInterestsScreen(
                        onBack = { findNavController().navigateUp() },
                        onFinish = { findNavController().navigate(R.id.action_interests_to_feed) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingInterestsScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    val interests = remember {
        mutableStateListOf(
            Interest("1", "Musique", R.drawable.ic_mic),
            Interest("2", "Photographie", R.drawable.ic_image_add),
            Interest("3", "Voyage", R.drawable.ic_globe),
            Interest("4", "Technologie", R.drawable.ic_settings),
            Interest("5", "Sport", R.drawable.ic_people),
            Interest("6", "Art", R.drawable.ic_grid),
            Interest("7", "Cuisine", R.drawable.ic_info),
            Interest("8", "Mode", R.drawable.ic_tag)
        )
    }

    val selectedCount = interests.count { it.isSelected }
    val isFinishEnabled = selectedCount >= 3

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
            progress = { 1.0f },
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
            Text(text = "Étape finale", color = Color(0xFF6200EE), fontSize = 12.sp)
            Text(text = "100% complété", color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Vos centres d'intérêt",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = "Choisissez au moins 3 sujets qui vous intéressent pour personnaliser votre flux.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Interests Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(interests) { interest ->
                InterestItem(
                    interest = interest,
                    onClick = {
                        val index = interests.indexOf(interest)
                        interests[index] = interest.copy(isSelected = !interest.isSelected)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            enabled = isFinishEnabled,
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
            Text(
                text = if (isFinishEnabled) "Terminer" else "Choisir encore ${3 - selectedCount}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InterestItem(interest: Interest, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (interest.isSelected) BorderStroke(2.dp, Color(0xFF6200EE)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (interest.isSelected) Color(0xFFF3E5F5) else Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (interest.isSelected) 4.dp else 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = interest.iconRes),
                    contentDescription = null,
                    tint = if (interest.isSelected) Color(0xFF6200EE) else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = interest.name,
                    fontSize = 14.sp,
                    fontWeight = if (interest.isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (interest.isSelected) Color(0xFF6200EE) else Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            
            if (interest.isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .background(Color(0xFF6200EE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
