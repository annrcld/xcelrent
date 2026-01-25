package com.example.xcelrent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    // Sample data for your project presentation
    val sampleCars = listOf(
        Car("1", "Tesla Model 3", 75.0, "Automatic | 5 Seats"),
        Car("2", "Porsche Taycan", 150.0, "Automatic | 4 Seats"),
        Car("3", "BMW M4", 120.0, "Manual | 4 Seats")
    )

    Scaffold(
        topBar = {
            Text(
                "Xcelrent",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Sport Red
                modifier = Modifier.padding(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Pure Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Text(
                    "Available for Rent",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(sampleCars) { car ->
                CarCard(car = car) {
                    navController.navigate("details/${car.id}")
                }
            }
        }
    }
}

@Composable
fun CarCard(car: Car, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Deep Grey
        )
    ) {
        Column {
            // Placeholder for Car Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF1E1E1E))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = car.model, // This matches the data class above
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text("★ 4.9", color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price highlighted in Sport Red
                Text(
                    text = "$${car.price}/day",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}