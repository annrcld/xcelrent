package com.example.xcelrent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.xcelrent.ui.theme.InterFamily

data class Booking(
    val id: String,
    val carModel: String,
    val totalPrice: Double,
    val status: String,
    val imageUrl: String
)

val sampleBookings = listOf(
    Booking("booking1", "Toyota Camry", 220.0, "Confirmed", "https://images.hgmsites.net/med/2023-toyota-camry-se-auto-natl-angular-front-exterior-view_100857360_m.jpg"),
    Booking("booking2", "Honda CR-V", 350.0, "On-going", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTFb_P9pn8AGyKRVw66bk28SMPjQ3EHIzGePQ&s"),
    Booking("booking3", "BMW 3 Series", 570.0, "Completed", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQoE5_w-VuOKWtlS9i1xM_NUPbZV__Usy8rLg&s")
)

@Composable
fun MyTripsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "My Trips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFamily
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            items(sampleBookings) { booking ->
                BookingListItem(booking = booking)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun BookingListItem(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = booking.carModel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFamily
            )
            Text(
                text = "Total: $${booking.totalPrice}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontFamily = InterFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: ${booking.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (booking.status == "Confirmed") SportRed else if (booking.status == "On-going") Color(0xFF4CAF50) else Color.Gray,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFamily
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyTripsScreenPreview() {
    MyTripsScreen(rememberNavController())
}