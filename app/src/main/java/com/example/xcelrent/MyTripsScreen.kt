package com.example.xcelrent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

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
            Column(modifier = Modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)) {
                Text(
                    "My Trips",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFamily,
                    color = Color.Black
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
                    BookingListItem(
                        booking = booking,
                        onCancel = {
                            // Transaction to update both booking status and car availability
                            db.runTransaction { transaction ->
                                val bookingRef = db.collection("bookings").document(booking.id)
                                val carRef = db.collection("cars").document(booking.carId)
                                
                                transaction.update(bookingRef, "status", "Cancelled")
                                transaction.update(carRef, "status", "Live") // Returns vehicle to "Live" (Available) status
                            }.addOnSuccessListener {
                                Toast.makeText(context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingListItem(booking: Booking, onCancel: () -> Unit) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to cancel this booking? This action will release the vehicle for others.") },
            confirmButton = {
                TextButton(onClick = {
                    onCancel()
                    showCancelDialog = false
                }) {
                    Text("Confirm", color = SportRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No", color = Color.Gray)
                }
            }
        )
    }

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
                            fontFamily = InterFamily,
                            color = Color.Black
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
                        text = "₱${String.format("%,.2f", booking.totalPrice)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SportRed,
                        fontFamily = InterFamily
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            
            // Trip Details (Dates & Times)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TripDetailItem(label = "PICK-UP", date = booking.pickupDate, time = booking.pickupTime)
                TripDetailItem(label = "RETURN", date = booking.returnDate, time = booking.returnTime, alignEnd = true)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Locations and Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TripInfoRow(icon = Icons.Filled.DriveEta, label = "Mode", value = "${booking.driveType} (${booking.serviceType})")
                TripInfoRow(icon = Icons.Filled.LocationOn, label = if (booking.serviceType == "Pick-up") "Pick-up Point" else "Delivery Address", value = booking.pickupLocation)
                TripInfoRow(icon = Icons.Filled.KeyboardReturn, label = "Return Location", value = booking.deliveryLocation)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancellation logic: Only Pending or Confirmed bookings can be cancelled
                if (booking.status == "Pending" || booking.status == "Confirmed") {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("Cancel Booking", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Balance Summary / Contact
                Surface(
                    color = SportRed.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BALANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("₱${String.format("%,.0f", booking.remainingBalance)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SportRed)
                        }
                        IconButton(
                            onClick = { /* Handle call */ },
                            modifier = Modifier.size(32.dp).background(Color.White, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Filled.Phone, null, tint = SportRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripDetailItem(label: String, date: String, time: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(time, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun TripInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SportRed, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Confirmed" -> Color(0xFF4CAF50)
        "On-going" -> Color(0xFF2196F3)
        "Pending" -> Color(0xFFFF9800)
        "Completed" -> Color(0xFF9C27B0)
        "Cancelled" -> Color(0xFFF44336)
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
