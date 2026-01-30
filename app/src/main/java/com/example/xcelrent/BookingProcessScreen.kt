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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingProcessScreen(carId: String?, pickup: String, returnDateArg: String, navController: NavController) {
    val car = carList.find { it.id == carId } ?: return
    var currentStep by remember { mutableStateOf(1) }
    val context = LocalContext.current
    
    // Trip Details
    var pickupDate by remember { mutableStateOf(pickup) }
    var returnDate by remember { mutableStateOf(returnDateArg) }
    var pickupTime by remember { mutableStateOf("10:00 AM") }
    var returnTime by remember { mutableStateOf("10:00 AM") }
    
    // Service Selections
    var driveType by remember { mutableStateOf("Self-Drive") } // "Self-Drive" or "With Driver"
    var serviceType by remember { mutableStateOf("Pick-up") } // "Pick-up" or "Delivery"
    
    // Locations
    var deliveryAddress by remember { mutableStateOf("") }
    var returnAddress by remember { mutableStateOf("") }
    
    // Step 3: Payment
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var paymentProofUrl by remember { mutableStateOf("") }
    
    // User Data
    var user by remember { mutableStateOf<User?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Calculate Days
    val totalDays = remember(pickupDate, returnDate) {
        calculateDays(pickupDate, returnDate)
    }

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
                    2 -> RenterInfoStep(
                        user = user, 
                        car = car,
                        pickupDate = pickupDate,
                        returnDate = returnDate,
                        pickupTime = pickupTime,
                        returnTime = returnTime,
                        driveType = driveType,
                        serviceType = serviceType,
                        deliveryAddress = deliveryAddress,
                        returnAddress = returnAddress,
                        onPickupDateChange = { pickupDate = it },
                        onReturnDateChange = { returnDate = it },
                        onPickupTimeChange = { pickupTime = it },
                        onReturnTimeChange = { returnTime = it },
                        onDriveTypeChange = { driveType = it },
                        onServiceTypeChange = { serviceType = it },
                        onDeliveryAddressChange = { deliveryAddress = it },
                        onReturnAddressChange = { returnAddress = it },
                        onNext = { currentStep = 3 }
                    )
                    3 -> PaymentMethodStep(onPaymentConfirmed = { method, proof -> 
                        selectedPaymentMethod = method
                        paymentProofUrl = proof
                        currentStep = 4 
                    })
                    4 -> BookingSummaryStep(
                        car = car, 
                        user = user, 
                        pickupDate = pickupDate,
                        returnDate = returnDate,
                        pickupTime = pickupTime,
                        returnTime = returnTime,
                        driveType = driveType,
                        serviceType = serviceType,
                        deliveryAddress = if (serviceType == "Delivery") deliveryAddress else car.location,
                        returnAddress = returnAddress,
                        totalDays = totalDays,
                        payment = selectedPaymentMethod!!
                    ) {
                        isBooking = true
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val bookingId = db.collection("bookings").document().id
                            val reservationFee = 500.0
                            val totalPrice = car.price * totalDays
                            
                            val booking = Booking(
                                id = bookingId,
                                userId = currentUser.uid,
                                carId = car.id,
                                carModel = car.model,
                                plateNumber = car.plateNumber.ifEmpty { "ABC 1234" },
                                pickupLocation = if (serviceType == "Pick-up") car.location else deliveryAddress,
                                deliveryLocation = returnAddress,
                                pickupDate = pickupDate,
                                returnDate = returnDate,
                                pickupTime = pickupTime,
                                returnTime = returnTime,
                                driveType = driveType,
                                serviceType = serviceType,
                                paymentMethod = selectedPaymentMethod?.name ?: "",
                                paymentProofUrl = paymentProofUrl,
                                reservationFee = reservationFee,
                                totalPrice = totalPrice,
                                remainingBalance = totalPrice - reservationFee,
                                status = "Pending", 
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

fun calculateDays(start: String, end: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date1 = sdf.parse(start)
        val date2 = sdf.parse(end)
        val diff = date2!!.time - date1!!.time
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        if (days < 1) 1 else days
    } catch (e: Exception) {
        1
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
        Text("Vehicle Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenterInfoStep(
    user: User?, 
    car: Car,
    pickupDate: String, returnDate: String,
    pickupTime: String, returnTime: String,
    driveType: String, serviceType: String,
    deliveryAddress: String, returnAddress: String,
    onPickupDateChange: (String) -> Unit, onReturnDateChange: (String) -> Unit,
    onPickupTimeChange: (String) -> Unit, onReturnTimeChange: (String) -> Unit,
    onDriveTypeChange: (String) -> Unit, onServiceTypeChange: (String) -> Unit,
    onDeliveryAddressChange: (String) -> Unit, onReturnAddressChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column {
        Text("Renter Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Name: ${user?.firstName} ${user?.lastName}", fontWeight = FontWeight.Medium, color = Color.Black)
                Text("Email: ${user?.email}", color = Color.Gray)
                Text("Contact: ${user?.contactNum}", color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Service Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionButton(label = "Self-Drive", selected = driveType == "Self-Drive", modifier = Modifier.weight(1f)) { onDriveTypeChange("Self-Drive") }
            OptionButton(label = "With Driver", selected = driveType == "With Driver", modifier = Modifier.weight(1f)) { onDriveTypeChange("With Driver") }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionButton(label = "Pick-up", selected = serviceType == "Pick-up", modifier = Modifier.weight(1f)) { onServiceTypeChange("Pick-up") }
            OptionButton(label = "Delivery", selected = serviceType == "Delivery", modifier = Modifier.weight(1f)) { onServiceTypeChange("Delivery") }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Trip Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            var showPickupPicker by remember { mutableStateOf(false) }
            val pickupDateState = rememberDatePickerState()

            OutlinedTextField(
                value = pickupDate, onValueChange = {},
                readOnly = true, label = { Text("Pick-up Date") },
                modifier = Modifier.weight(1f).padding(end = 4.dp).clickable { showPickupPicker = true },
                enabled = false, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.LightGray, disabledLabelColor = Color.Gray),
                trailingIcon = { Icon(Icons.Filled.CalendarToday, null, tint = SportRed) }
            )

            if (showPickupPicker) {
                DatePickerDialog(
                    onDismissRequest = { showPickupPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showPickupPicker = false
                            pickupDateState.selectedDateMillis?.let { onPickupDateChange(sdf.format(Date(it))) }
                        }) { Text("OK", color = SportRed) }
                    },
                    dismissButton = { TextButton(onClick = { showPickupPicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = pickupDateState) }
            }

            var showReturnPicker by remember { mutableStateOf(false) }
            val returnDateState = rememberDatePickerState()

            OutlinedTextField(
                value = returnDate, onValueChange = {},
                readOnly = true, label = { Text("Return Date") },
                modifier = Modifier.weight(1f).padding(start = 4.dp).clickable { showReturnPicker = true },
                enabled = false, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.LightGray, disabledLabelColor = Color.Gray),
                trailingIcon = { Icon(Icons.Filled.CalendarToday, null, tint = SportRed) }
            )

            if (showReturnPicker) {
                DatePickerDialog(
                    onDismissRequest = { showReturnPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showReturnPicker = false
                            returnDateState.selectedDateMillis?.let { onReturnDateChange(sdf.format(Date(it))) }
                        }) { Text("OK", color = SportRed) }
                    },
                    dismissButton = { TextButton(onClick = { showReturnPicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = returnDateState) }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = pickupTime, onValueChange = onPickupTimeChange,
                label = { Text("Pick-up Time") },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedBorderColor = SportRed, unfocusedBorderColor = Color.LightGray),
                trailingIcon = { Icon(Icons.Filled.AccessTime, null, tint = SportRed) }
            )
            OutlinedTextField(
                value = returnTime, onValueChange = onReturnTimeChange,
                label = { Text("Return Time") },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedBorderColor = SportRed, unfocusedBorderColor = Color.LightGray),
                trailingIcon = { Icon(Icons.Filled.AccessTime, null, tint = SportRed) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Locations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (serviceType == "Pick-up") {
            InfoCardItem(label = "Vehicle Fixed Location", value = car.location, icon = Icons.Filled.LocationOn)
        } else {
            OutlinedTextField(
                value = deliveryAddress, onValueChange = onDeliveryAddressChange,
                label = { Text("Delivery Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedBorderColor = SportRed, unfocusedBorderColor = Color.LightGray)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = returnAddress, onValueChange = onReturnAddressChange,
            label = { Text("Return/Drop-off Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedBorderColor = SportRed, unfocusedBorderColor = Color.LightGray)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            enabled = (serviceType == "Pick-up" || deliveryAddress.isNotEmpty()) && returnAddress.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Next: Payment Method", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OptionButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SportRed else Color(0xFFF5F5F5))
            .clickable { onClick() }
            .border(1.dp, if (selected) SportRed else Color.LightGray, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

data class PaymentMethod(val name: String)

@Composable
fun PaymentMethodStep(onPaymentConfirmed: (PaymentMethod, String) -> Unit) {
    val methods = listOf(PaymentMethod("GCash"), PaymentMethod("Maya"), PaymentMethod("BDO"), PaymentMethod("GoTyme"), PaymentMethod("MariBank"))
    var showQrFor by remember { mutableStateOf<PaymentMethod?>(null) }
    var proofUploaded by remember { mutableStateOf(false) }

    Column {
        Text("Select Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        Text("Reservation Fee: ₱500.00 (Deductible)", style = MaterialTheme.typography.bodyMedium, color = SportRed)
        Spacer(modifier = Modifier.height(24.dp))

        methods.forEach { method ->
            PaymentMethodItem(method = method, isSelected = showQrFor == method, onSelect = { showQrFor = it })
            Spacer(modifier = Modifier.height(12.dp))
        }

        AnimatedVisibility (visible = showQrFor != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(200.dp).background(Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("QR CODE FOR ${showQrFor?.name}", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { proofUploaded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.CloudUpload, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Payment Receipt")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { showQrFor?.let { onPaymentConfirmed(it, "dummy_url") } },
            enabled = showQrFor != null && proofUploaded,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Confirm Payment", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PaymentMethodItem(method: PaymentMethod, isSelected: Boolean, onSelect: (PaymentMethod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) SportRed.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
            .border(1.dp, if (isSelected) SportRed else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable { onSelect(method) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Payment, contentDescription = null, tint = SportRed)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(method.name, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
        RadioButton(selected = isSelected, onClick = { onSelect(method) }, colors = RadioButtonDefaults.colors(selectedColor = SportRed))
    }
}

@Composable
fun BookingSummaryStep(
    car: Car, user: User?,
    pickupDate: String, returnDate: String,
    pickupTime: String, returnTime: String,
    driveType: String, serviceType: String,
    deliveryAddress: String, returnAddress: String,
    totalDays: Int, payment: PaymentMethod,
    onComplete: () -> Unit
) {
    val totalPrice = car.price * totalDays
    val reservationFee = 500.0
    val remainingBalance = totalPrice - reservationFee

    Column {
        Text("Booking Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryRow("Vehicle", car.model)
                SummaryRow("Type", driveType)
                SummaryRow("Service", serviceType)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SummaryRow("Pick-up", "$pickupDate at $pickupTime")
                SummaryRow("Return", "$returnDate at $returnTime")
                SummaryRow("Pick-up Loc", deliveryAddress)
                SummaryRow("Return Loc", returnAddress)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SummaryRow("Total Price", "₱${String.format("%,.2f", totalPrice)}")
                SummaryRow("Paid (Res)", "₱${String.format("%,.2f", reservationFee)}", Color(0xFF4CAF50))
                SummaryRow("Balance", "₱${String.format("%,.2f", remainingBalance)}", SportRed)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SportRed.copy(alpha = 0.05f)), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = SportRed)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Owner: +63 912 345 6789", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Contact for immediate concerns", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SportRed), shape = RoundedCornerShape(16.dp)) {
            Text("Complete Booking", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, color = valueColor, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
}

@Composable
fun InfoCardItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8F8F8)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SportRed, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
        }
    }
}
