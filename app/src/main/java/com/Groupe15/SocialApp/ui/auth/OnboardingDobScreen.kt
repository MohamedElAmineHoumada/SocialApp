package com.Groupe15.SocialApp.ui.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.Groupe15.SocialApp.R
import com.Groupe15.SocialApp.viewmodel.OnboardingViewModel
import java.util.Calendar

@Composable
fun OnboardingDobScreen(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var day by remember { mutableStateOf(15) }
    var month by remember { mutableStateOf(5) }
    var year by remember { mutableStateOf(1998) }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isAgeValid = remember(day, month, year) {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        
        // Correct month/day check
        if (today.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH) && 
             today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))) {
            age--
        }
        age >= 13
    }

    val months = listOf(
        stringResource(R.string.mon_jan),
        stringResource(R.string.mon_feb),
        stringResource(R.string.mon_mar),
        stringResource(R.string.mon_apr),
        stringResource(R.string.mon_may),
        stringResource(R.string.mon_jun),
        stringResource(R.string.mon_jul),
        stringResource(R.string.mon_aug),
        stringResource(R.string.mon_sep),
        stringResource(R.string.mon_oct),
        stringResource(R.string.mon_nov),
        stringResource(R.string.mon_dec)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Image(
                painter = painterResource(id = R.drawable.logo_afn),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(48.dp)) // Placeholder for symmetry
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Bar Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f) // Step 2 of 2
                        .fillMaxHeight()
                        .background(Color(0xFF6C47FF), RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.step_x_of_y, 2, 2),
                    fontSize = 12.sp,
                    color = Color(0xFF6C47FF),
                    fontWeight = FontWeight.Bold
                )
                Text(text = stringResource(R.string.completed_100), fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.birth_date),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.birth_date_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Date Picker Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFF0F0F0))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selected Date Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DatePartDisplay(stringResource(R.string.day_label), day.toString().padStart(2, '0'))
                    Text(
                        ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C47FF),
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    DatePartDisplay(stringResource(R.string.month_label), month.toString().padStart(2, '0'))
                    Text(
                        ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6C47FF),
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    DatePartDisplay(stringResource(R.string.year_label), year.toString())
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Wheel Pickers (Simplified with buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WheelColumn(
                        value = day.toString(),
                        onUp = { if (day < 31) day++ },
                        onDown = { if (day > 1) day-- }
                    )
                    WheelColumn(
                        value = months[month - 1],
                        onUp = { if (month < 12) month++ },
                        onDown = { if (month > 1) month-- }
                    )
                    WheelColumn(
                        value = year.toString(),
                        onUp = { if (year < 2024) year++ },
                        onDown = { if (year > 1920) year-- }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Age requirement info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = if (showError && !isAgeValid) Color(0xFFFFEBEE) else Color(0xFFF4F6FF),
                        shape = RoundedCornerShape(20.dp),
                        border = if (showError && !isAgeValid) BorderStroke(1.dp, Color.Red) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = if (showError && !isAgeValid) Color.Red else Color(0xFF6C47FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.min_age_warning),
                                fontSize = 12.sp,
                                color = if (showError && !isAgeValid) Color.Red else Color(0xFF4A4A4A)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showError && !isAgeValid) {
                        Text(
                            text = stringResource(R.string.age_requirement_error),
                            color = Color.Red,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Social Proof
        Surface(
            color = Color(0xFFF8F8FF),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarStack()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.join_community_desc),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Continue Button
        Button(
            onClick = {
                if (isAgeValid) {
                    val birthDate = "$day/${month.toString().padStart(2, '0')}/$year"
                    viewModel.saveBirthDate(birthDate, onContinue)
                } else {
                    showError = true
                    Toast.makeText(context, context.getString(R.string.min_age_warning), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF6C47FF),
                                Color(0xFF9D47FF)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.continue_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.why_ask_this),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DatePartDisplay(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 32.sp, color = Color(0xFF6C47FF), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WheelColumn(value: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.Gray)
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        IconButton(onClick = onDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun AvatarStack() {
    Row {
        val avatars = listOf(
            "https://i.pravatar.cc/150?u=1",
            "https://i.pravatar.cc/150?u=2",
            "https://i.pravatar.cc/150?u=3"
        )
        avatars.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = (index * -8).dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = (3 * -8).dp)
                .clip(CircleShape)
                .background(Color(0xFF6C47FF))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("+2k", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
