package com.example.xcelrent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.xcelrent.ui.theme.InterFamily
import com.example.xcelrent.ui.theme.SportRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun MyTripsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(auth.currentUser) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    bookings = snapshot.toObjects(Booking::class.java)
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "My Trips",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFamily
                )
                Text(
                    "Manage your active and past bookings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontFamily = InterFamily
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SportRed)
            }
        } else if (bookings.isEmpty()) {
            EmptyTripsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bookings) { booking ->
                    BookingListItem(booking = booking)
                }
            }
        }
    }
}

@Composable
fun BookingListItem(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = booking.imageUrl,
                    contentDescription = booking.carModel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = booking.carModel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFamily
                        )
                        StatusBadge(status = booking.status)
                    }
                    Text(
                        text = "Plate: ${booking.plateNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontFamily = InterFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₱${booking.totalPrice}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SportRed,
                        fontFamily = InterFamily
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TripInfoItem(label = "Pickup", value = booking.pickupLocation)
                TripInfoItem(label = "Delivery", value = booking.deliveryLocation)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Balance:", fontSize = 12.sp, color = Color.Gray)
                    Text("₱${booking.remainingBalance}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Confirmed" -> Color(0xFF4CAF50)
        "On-going" -> Color(0xFF2196F3)
        "Pending" -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFamily
        )
    }
}

@Composable
fun TripInfoItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontFamily = InterFamily)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
    }
}

@Composable
fun EmptyTripsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No trips found",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray,
            fontFamily = InterFamily
        )
        Text(
            "Your booked cars will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            fontFamily = InterFamily
        )
    }
}
