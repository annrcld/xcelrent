package com.example.xcelrent

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.xcelrent.ui.theme.InterFamily
import com.example.xcelrent.ui.theme.SportRed
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingProcessScreen(carId: String?, navController: NavController) {
    val car = carList.find { it.id == carId } ?: return
    var currentStep by remember { mutableStateOf(1) }
    val context = LocalContext.current
    
    // Step 2: Renter Info & Locations
    var pickupLocation by remember { mutableStateOf("") }
    var deliveryLocation by remember { mutableStateOf("") }
    
    // Step 3: Payment
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
    // User Data
    var user by remember { mutableStateOf<User?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener {
                user = it.toObject(User::class.java)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Process", fontFamily = InterFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentStep > 1) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (isBooking) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SportRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Step Indicator
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StepIndicator(step = 1, currentStep = currentStep, label = "Vehicle")
                    StepIndicator(step = 2, currentStep = currentStep, label = "Renter")
                    StepIndicator(step = 3, currentStep = currentStep, label = "Payment")
                    StepIndicator(step = 4, currentStep = currentStep, label = "Summary")
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                when (currentStep) {
                    1 -> VehicleDetailsStep(car) { currentStep = 2 }
                    2 -> RenterInfoStep(user, pickupLocation, deliveryLocation, 
                        onPickupChange = { pickupLocation = it }, 
                        onDeliveryChange = { deliveryLocation = it },
                        onNext = { currentStep = 3 })
                    3 -> PaymentMethodStep(onPaymentSelected = { 
                        selectedPaymentMethod = it
                        currentStep = 4 
                    })
                    4 -> BookingSummaryStep(car, user, pickupLocation, deliveryLocation, selectedPaymentMethod!!) {
                        // Final Booking Logic
                        isBooking = true
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val bookingId = db.collection("bookings").document().id
                            val reservationFee = 500.0
                            val totalDays = 3 // This should ideally come from search dates
                            val totalPrice = car.price * totalDays
                            
                            val booking = Booking(
                                id = bookingId,
                                userId = currentUser.uid,
                                carId = car.id,
                                carModel = car.model,
                                plateNumber = car.plateNumber.ifEmpty { "ABC 1234" },
                                pickupLocation = pickupLocation,
                                deliveryLocation = deliveryLocation,
                                paymentMethod = selectedPaymentMethod?.name ?: "",
                                reservationFee = reservationFee,
                                totalPrice = totalPrice,
                                remainingBalance = totalPrice - reservationFee,
                                status = "Confirmed",
                                timestamp = Timestamp.now(),
                                imageUrl = car.imageUrl
                            )

                            db.collection("bookings").document(bookingId).set(booking)
                                .addOnSuccessListener {
                                    isBooking = false
                                    Toast.makeText(context, "Booking successful!", Toast.LENGTH_LONG).show()
                                    navController.navigate("mytrips") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                }
                                .addOnFailureListener {
                                    isBooking = false
                                    Toast.makeText(context, "Failed to book: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(step: Int, currentStep: Int, label: String) {
    val isActive = step <= currentStep
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isActive) SportRed else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(step.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(label, fontSize = 10.sp, color = if (isActive) SportRed else Color.Gray, fontFamily = InterFamily)
    }
}

@Composable
fun VehicleDetailsStep(car: Car, onNext: () -> Unit) {
    Column {
        Text("Vehicle Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
        Spacer(modifier = Modifier.height(16.dp))
        
        InfoCardItem(label = "Plate Number", value = car.plateNumber.ifEmpty { "ABC 1234" }, icon = Icons.Filled.CreditCard)
        InfoCardItem(label = "Location", value = car.location.ifEmpty { "Quezon City, Metro Manila" }, icon = Icons.Filled.LocationOn)
        InfoCardItem(label = "Coding Day", value = "Wednesday (Ends in 5 & 6)", icon = Icons.Filled.EventBusy)
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Next: Renter Information", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RenterInfoStep(user: User?, pickup: String, delivery: String, onPickupChange: (String) -> Unit, onDeliveryChange: (String) -> Unit, onNext: () -> Unit) {
    Column {
        Text("Renter Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Automatic User Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Name: ${user?.firstName} ${user?.lastName}", fontWeight = FontWeight.Medium)
                Text("Email: ${user?.email}", color = Color.Gray)
                Text("Contact: ${user?.contactNum}", color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Trip Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = pickup, onValueChange = onPickupChange,
            label = { Text("Pick-up Location") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = delivery, onValueChange = onDeliveryChange,
            label = { Text("Delivery/Return Location") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            enabled = pickup.isNotEmpty() && delivery.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Next: Payment Method", fontWeight = FontWeight.Bold)
        }
    }
}

data class PaymentMethod(val name: String)

@Composable
fun PaymentMethodStep(onPaymentSelected: (PaymentMethod) -> Unit) {
    val methods = listOf(
        PaymentMethod("GCash"),
        PaymentMethod("Maya"),
        PaymentMethod("BDO"),
        PaymentMethod("GoTyme"),
        PaymentMethod("MariBank")
    )
    
    var showQrFor by remember { mutableStateOf<PaymentMethod?>(null) }

    Column {
        Text("Select Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
        Text("Reservation Fee: ₱500.00 (Deductible)", style = MaterialTheme.typography.bodyMedium, color = SportRed)
        Spacer(modifier = Modifier.height(16.dp))
        
        methods.forEach { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showQrFor = method },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Payment, null, tint = SportRed)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(method.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
                }
            }
        }
    }

    if (showQrFor != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showQrFor = null }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pay with ${showQrFor?.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    // Mock QR Code
                    Box(modifier = Modifier.size(200.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.QrCode2, null, modifier = Modifier.size(150.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scan to pay ₱500.00", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onPaymentSelected(showQrFor!!) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SportRed)
                    ) {
                        Text("I have paid")
                    }
                }
            }
        }
    }
}

@Composable
fun BookingSummaryStep(car: Car, user: User?, pickup: String, delivery: String, payment: PaymentMethod, onConfirm: () -> Unit) {
    val reservationFee = 500.0
    val totalDays = 3
    val carTotal = car.price * totalDays
    val finalTotal = carTotal - reservationFee

    Column {
        Text("Booking Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
        Spacer(modifier = Modifier.height(16.dp))
        
        SummarySection("Vehicle", "${car.model} (${car.plateNumber.ifEmpty { "ABC 1234" }})")
        SummarySection("Renter", "${user?.firstName} ${user?.lastName}")
        SummarySection("Route", "$pickup → $delivery")
        SummarySection("Payment", "${payment.name} (₱500 Reservation paid)")
        
        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        PriceRow("Rental Fee ($totalDays days)", "₱${carTotal}")
        PriceRow("Reservation Fee", "-₱${reservationFee}", color = Color(0xFF4CAF50))
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PriceRow("Remaining Balance", "₱${finalTotal}", isBold = true)
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Confirm & Book Now", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoCardItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SportRed, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummarySection(title: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, fontSize = 12.sp, color = Color.Gray)
        Text(detail, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PriceRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Bold, color = color)
    }
}
