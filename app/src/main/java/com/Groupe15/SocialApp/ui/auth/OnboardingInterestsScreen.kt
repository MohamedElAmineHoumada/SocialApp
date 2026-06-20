package com.Groupe15.SocialApp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Interest(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val isFullWidth: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingInterestsScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val interests = listOf(
        Interest("design", "Design", Icons.Default.Palette),
        Interest("tech", "Tech", Icons.Default.Devices),
        Interest("lifestyle", "Lifestyle", Icons.Default.AutoAwesome, isFullWidth = true),
        Interest("photography", "Photography", Icons.Default.CameraAlt),
        Interest("art", "Art", Icons.Default.Brush),
        Interest("gaming", "Gaming", Icons.Default.Gamepad, isFullWidth = true),
        Interest("music", "Musique", Icons.Default.MusicNote),
        Interest("travel", "Voyage", Icons.Default.Flight)
    )

    var searchQuery by remember { mutableStateOf("") }
    val selectedInterests = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ETAPE 3 SUR 3",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Text(
                text = "100% complet",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress Bar
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF6C47FF),
            trackColor = Color(0xFFF0EFFF),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Centres d'intérêt",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Personnalisez votre flux en choisissant au moins 3 sujets qui vous passionnent.",
            fontSize = 15.sp,
            color = Color.Gray,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("Rechercher un sujet...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF8F9FF),
                unfocusedContainerColor = Color(0xFFF8F9FF),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Interests Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(
                items = interests.filter { it.name.contains(searchQuery, ignoreCase = true) },
                span = { interest ->
                    GridItemSpan(if (interest.isFullWidth) 2 else 1)
                }
            ) { interest ->
                InterestCard(
                    interest = interest,
                    isSelected = selectedInterests.contains(interest.id),
                    onClick = {
                        if (selectedInterests.contains(interest.id)) {
                            selectedInterests.remove(interest.id)
                        } else {
                            selectedInterests.add(interest.id)
                        }
                    }
                )
            }
        }

        // Bottom Action
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            enabled = selectedInterests.size >= 3
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (selectedInterests.size >= 3) 
                                listOf(Color(0xFF6C47FF), Color(0xFF9D47FF))
                            else 
                                listOf(Color.LightGray, Color.LightGray)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Terminer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun InterestCard(
    interest: Interest,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (interest.isFullWidth) 110.dp else 130.dp)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF6C47FF) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF8F7FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Icon
            Icon(
                imageVector = interest.icon,
                contentDescription = null,
                tint = Color(0xFF6C47FF),
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopStart)
            )

            // Selection indicator (radio button style)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF6C47FF) else Color(0xFFD1D5DB),
                        shape = CircleShape
                    )
                    .background(if (isSelected) Color(0xFF6C47FF) else Color.Transparent)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Name
            Text(
                text = interest.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF6C47FF) else Color.Black,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
