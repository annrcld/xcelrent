package com.example.xcelrent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.xcelrent.ui.theme.*

@Composable
fun LandingScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp, vertical = 60.dp) // Large vertical padding
    ) {
        // 1. Minimalist Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Xcelrent",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Text(
                text = ".",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFE53935)
            )
        }

        // MASSIVE GAP: This creates the "Premium" website feel
        Spacer(modifier = Modifier.height(140.dp))

        // 2. High-Impact Taglines (from your website)
        Text(
            text = "Simply Excellent.",
            style = MaterialTheme.typography.displayLarge,
            color = Color(0xFF212121)
        )
        Text(
            text = "Truly Affordable.",
            style = MaterialTheme.typography.displayLarge,
            color = Color(0xFFE53935)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 3. Readable Sub-headline
        Text(
            text = "Unlock your next adventure. Reliable rides ready for wherever the road takes you.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(1f)) // Pushes button to the very bottom

        // 4. Large Action Button
        Button(
            onClick = { navController.navigate("create_account") },
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp), // Extra tall for premium feel
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Already have an account? Log In",
            modifier = Modifier.align(Alignment.CenterHorizontally).clickable { navController.navigate("login") },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}
