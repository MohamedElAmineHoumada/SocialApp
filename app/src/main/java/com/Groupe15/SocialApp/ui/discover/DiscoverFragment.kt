package com.Groupe15.SocialApp.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiscoverFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    DiscoverScreen(
                        onNavigateToProfile = { uid ->
                            val bundle = Bundle().apply { putString("uid", uid) }
                            findNavController().navigate(R.id.profileFragment, bundle)
                        },
                        onNavigateToNotifications = { /* TODO */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("All", "Photography", "Tech", "Lifestyle", "Art", "Design")
    var selectedCategory by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AFN",
                        color = Color(0xFF4E33B3),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification),
                            contentDescription = "Notifications",
                            tint = Color(0xFF4E33B3)
                        )
                    }
                    IconButton(onClick = { onNavigateToProfile("") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_default_avatar),
                            contentDescription = "Profile",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FE))
            )
        },
        containerColor = Color(0xFFF8F9FE)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Search Bar
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Discover creators, art, and tech...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Categories
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4E33B3),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Who to follow
            SectionHeader(title = "Who to follow", onSeeAllClick = {})
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Placeholder creators
                items(5) {
                    CreatorCard(
                        name = "Alex Rivera",
                        role = "Tech Curator",
                        imageUrl = "",
                        onFollowClick = {}
                    )
                }
            }

            // Trending
            Text(
                text = "Trending",
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            
            // Staggered Grid for Trending
            // Note: Since this is inside a verticalScroll Column, we need to give it a fixed height or use it differently.
            // But usually, Discover has a lot of content. Let's use a simple Column with Rows or just disable nested scroll if possible.
            // Better: use the Column for everything and just a few rows for trending if we want to keep verticalScroll.
            // Or use LazyVerticalStaggeredGrid as the main container.
            
            TrendingGrid()
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = "See all",
            modifier = Modifier.clickable(onClick = onSeeAllClick),
            color = Color(0xFF4E33B3),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CreatorCard(name: String, role: String, imageUrl: String, onFollowClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = imageUrl.ifEmpty { R.drawable.ic_default_avatar },
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A))
            Text(text = role, fontSize = 12.sp, color = Color(0xFF9E9E9E))
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFollowClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E33B3))
            ) {
                Text("Follow", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TrendingGrid() {
    // Since we are in a verticalScroll, we can't use a Lazy grid directly without height.
    // For a "Discover" screen, we'll just show a few trending items in a Column of Rows for now
    // or use a fixed height.
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        val trendingItems = listOf(
            "Tech Review" to R.drawable.placeholder_cover,
            "Art Gallery" to R.drawable.placeholder_cover,
            "Travel Vlog" to R.drawable.placeholder_cover,
            "Cooking Tips" to R.drawable.placeholder_cover
        )
        
        Row(modifier = Modifier.fillMaxWidth()) {
            TrendingItem(modifier = Modifier.weight(1f), title = trendingItems[0].first, imageRes = trendingItems[0].second)
            TrendingItem(modifier = Modifier.weight(1f), title = trendingItems[1].first, imageRes = trendingItems[1].second)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TrendingItem(modifier = Modifier.weight(1f), title = trendingItems[2].first, imageRes = trendingItems[2].second)
            TrendingItem(modifier = Modifier.weight(1f), title = trendingItems[3].first, imageRes = trendingItems[3].second)
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun TrendingItem(modifier: Modifier = Modifier, title: String, imageRes: Int) {
    Card(
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
            
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_reels),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(
                onClick = {},
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_heart_outline),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
