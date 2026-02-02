package com.example.xcelrent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val db = FirebaseFirestore.getInstance()
    var car by remember { mutableStateOf<Car?>(null) }
    var isLoadingCar by remember { mutableStateOf(true) }
    var currentStep by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    
    // Trip Details
    var pickupDate by remember { mutableStateOf(pickup) }
    var returnDate by remember { mutableStateOf(returnDateArg) }
    var pickupTime by remember { mutableStateOf("10:00 AM") }
    var returnTime by remember { mutableStateOf("10:00 AM") }
    
    // Service Selections
    var driveType by remember { mutableStateOf("Self-Drive") }
    var serviceType by remember { mutableStateOf("Pick-up") }
    
    // Locations
    var deliveryAddress by remember { mutableStateOf("") }
    var returnAddress by remember { mutableStateOf("") }
    
    // Step 3: Payment
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var paymentProofUrl by remember { mutableStateOf("") }
    
    // Step 4: Credentials
    var driversLicenseUrl by remember { mutableStateOf("") }
    var ltoQrUrl by remember { mutableStateOf("") }
    var proofOfBillingUrl by remember { mutableStateOf("") }
    var selfieWithIdUrl by remember { mutableStateOf("") }
    var secondValidIdUrl by remember { mutableStateOf("") }
    
    // User Data
    var user by remember { mutableStateOf<User?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()

    val totalDays = remember(pickupDate, returnDate) { calculateDays(pickupDate, returnDate) }

    LaunchedEffect(carId) {
        if (carId != null) {
            db.collection("cars").document(carId).get().addOnSuccessListener { snapshot ->
                car = snapshot.toObject(Car::class.java)
                isLoadingCar = false
            }.addOnFailureListener {
                isLoadingCar = false
            }
        }
        
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
                    IconButton(onClick = { if (currentStep > 1) currentStep -= 1 else navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (isBooking || isLoadingCar) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SportRed)
            }
        } else {
            val currentCar = car ?: return@Scaffold
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Expanded Step Indicator
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StepIndicator(step = 1, currentStep = currentStep, label = "Vehicle")
                    StepIndicator(step = 2, currentStep = currentStep, label = "Renter")
                    StepIndicator(step = 3, currentStep = currentStep, label = "Payment")
                    StepIndicator(step = 4, currentStep = currentStep, label = "Verify")
                    StepIndicator(step = 5, currentStep = currentStep, label = "Summary")
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                when (currentStep) {
                    1 -> VehicleDetailsStep(currentCar) { currentStep = 2 }
                    2 -> RenterInfoStep(
                        user, currentCar, pickupDate, returnDate, pickupTime, returnTime, driveType, serviceType, deliveryAddress, returnAddress,
                        { pickupDate = it }, { returnDate = it }, { pickupTime = it }, { returnTime = it }, { driveType = it }, { serviceType = it }, { deliveryAddress = it }, { returnAddress = it },
                        { currentStep = 3 }
                    )
                    3 -> PaymentMethodStep { method, proof -> 
                        selectedPaymentMethod = method
                        paymentProofUrl = proof
                        currentStep = 4 
                    }
                    4 -> CredentialsStep(
                        driversLicenseUrl, ltoQrUrl, proofOfBillingUrl, selfieWithIdUrl, secondValidIdUrl,
                        { driversLicenseUrl = it }, { ltoQrUrl = it }, { proofOfBillingUrl = it }, { selfieWithIdUrl = it }, { secondValidIdUrl = it },
                        { currentStep = 5 }
                    )
                    5 -> BookingSummaryStep(
                        currentCar, user, pickupDate, returnDate, pickupTime, returnTime, driveType, serviceType, 
                        if (serviceType == "Delivery") deliveryAddress else currentCar.location, returnAddress, 
                        totalDays, selectedPaymentMethod!!
                    ) {
                        isBooking = true
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val bookingId = db.collection("bookings").document().id
                            val reservationFee = 500.0
                            val totalPrice = currentCar.price * totalDays
                            
                            val booking = Booking(
                                id = bookingId, userId = currentUser.uid, carId = currentCar.id, carModel = currentCar.model,
                                plateNumber = currentCar.plateNumber.ifEmpty { "ABC 1234" },
                                pickupLocation = if (serviceType == "Pick-up") currentCar.location else deliveryAddress,
                                deliveryLocation = returnAddress,
                                pickupDate = pickupDate, returnDate = returnDate, pickupTime = pickupTime, returnTime = returnTime,
                                driveType = driveType, serviceType = serviceType,
                                paymentMethod = selectedPaymentMethod?.name ?: "", paymentProofUrl = paymentProofUrl,
                                reservationFee = reservationFee, totalPrice = totalPrice, remainingBalance = totalPrice - reservationFee,
                                status = "Pending", timestamp = Timestamp.now(), imageUrl = currentCar.imageUrl,
                                driversLicenseUrl = driversLicenseUrl, ltoQrUrl = ltoQrUrl,
                                proofOfBillingUrl = proofOfBillingUrl, selfieWithIdUrl = selfieWithIdUrl, secondValidIdUrl = secondValidIdUrl
                            )

                            db.collection("bookings").document(bookingId).set(booking)
                                .addOnSuccessListener {
                                    isBooking = false
                                    Toast.makeText(context, "Booking successful!", Toast.LENGTH_LONG).show()
                                    navController.navigate("mytrips") { popUpTo("home") { inclusive = false } }
                                }
                                .addOnFailureListener {
                                    isBooking = false
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialsStep(
    dl: String, lto: String, pob: String, selfie: String, sid: String,
    onDl: (String) -> Unit, onLto: (String) -> Unit, onPob: (String) -> Unit, onSelfie: (String) -> Unit, onSid: (String) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text("Verify Identity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
        Text("Please upload clear photos of the following documents to proceed.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        UploadItem(label = "Driver's License", isUploaded = dl.isNotEmpty()) { onDl("dummy_dl_url") }
        UploadItem(label = "LTO Portal QR Code (Screenshot)", isUploaded = lto.isNotEmpty()) { onLto("dummy_lto_url") }
        UploadItem(label = "Proof of Billing (Electricity/Water)", isUploaded = pob.isNotEmpty()) { onPob("dummy_pob_url") }
        UploadItem(label = "Selfie with ID", isUploaded = selfie.isNotEmpty()) { onSelfie("dummy_selfie_url") }
        UploadItem(label = "Secondary Valid ID", isUploaded = sid.isNotEmpty()) { onSid("dummy_sid_url") }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            enabled = dl.isNotEmpty() && lto.isNotEmpty() && pob.isNotEmpty() && selfie.isNotEmpty() && sid.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SportRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Next: Final Summary", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UploadItem(label: String, isUploaded: Boolean, onUpload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isUploaded) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
            .clickable { onUpload() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isUploaded) Icons.Filled.CheckCircle else Icons.Filled.CloudUpload,
            contentDescription = null,
            tint = if (isUploaded) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = if (isUploaded) Color(0xFF2E7D32) else Color.Black)
        if (isUploaded) {
            Text("Uploaded", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        } else {
            Text("Upload", fontSize = 12.sp, color = SportRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StepIndicator(step: Int, currentStep: Int, label: String) {
    val isActive = step <= currentStep
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isActive) SportRed else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(step.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(label, fontSize = 9.sp, color = if (isActive) SportRed else Color.Gray, fontFamily = InterFamily)
    }
}

@Composable
fun VehicleDetailsStep(car: Car, onNext: () -> Unit) {
    Column {
        Text("Vehicle Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        InfoCardItem("Plate Number", car.plateNumber.ifEmpty { "ABC 1234" }, Icons.Filled.CreditCard)
        InfoCardItem("Location", car.location, Icons.Filled.LocationOn)
        InfoCardItem("Coding Day", "Varies by Plate", Icons.Filled.EventBusy)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SportRed), shape = RoundedCornerShape(16.dp)) {
            Text("Next: Renter Information", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenterInfoStep(
    user: User?, car: Car, pickupDate: String, returnDate: String, pickupTime: String, returnTime: String, driveType: String, serviceType: String, deliveryAddress: String, returnAddress: String,
    onPickupDate: (String) -> Unit, onReturnDate: (String) -> Unit, onPickupTime: (String) -> Unit, onReturnTime: (String) -> Unit, onDriveType: (String) -> Unit, onServiceType: (String) -> Unit, onDelivery: (String) -> Unit, onReturnLoc: (String) -> Unit,
    onNext: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

    var showPickupDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }
    var showPickupTimePicker by remember { mutableStateOf(false) }
    var showReturnTimePicker by remember { mutableStateOf(false) }

    val pickupDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try { sdf.parse(pickupDate)?.time } catch (_: Exception) { null }
    )
    val returnDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try { sdf.parse(returnDate)?.time } catch (_: Exception) { null }
    )

    val pTimeCal = Calendar.getInstance().apply { 
        try { time = timeSdf.parse(pickupTime) ?: time } catch (_: Exception) {}
    }
    val pickupTimePickerState = rememberTimePickerState(
        initialHour = pTimeCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = pTimeCal.get(Calendar.MINUTE)
    )

    val rTimeCal = Calendar.getInstance().apply { 
        try { time = timeSdf.parse(returnTime) ?: time } catch (_: Exception) {}
    }
    val returnTimePickerState = rememberTimePickerState(
        initialHour = rTimeCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = rTimeCal.get(Calendar.MINUTE)
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.DarkGray,
        focusedBorderColor = SportRed,
        unfocusedBorderColor = Color.LightGray,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = SportRed
    )
    val textStyle = TextStyle(color = Color.Black, fontSize = 14.sp)

    // Date Pickers
    if (showPickupDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showPickupDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickupDatePickerState.selectedDateMillis?.let { onPickupDate(sdf.format(Date(it))) }
                    showPickupDatePicker = false
                }) { Text("OK", color = SportRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPickupDatePicker = false }) { Text("Cancel", color = Color.Gray) }
            }
        ) { DatePicker(state = pickupDatePickerState) }
    }

    if (showReturnDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    returnDatePickerState.selectedDateMillis?.let { onReturnDate(sdf.format(Date(it))) }
                    showReturnDatePicker = false
                }) { Text("OK", color = SportRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDatePicker = false }) { Text("Cancel", color = Color.Gray) }
            }
        ) { DatePicker(state = returnDatePickerState) }
    }


    Column {
        Text("Renter Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${user?.firstName} ${user?.lastName}", fontWeight = FontWeight.Bold, color = Color.Black)
                Text("${user?.email} | ${user?.contactNum}", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Service Options", fontWeight = FontWeight.Bold, color = Color.Black)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionButton("Self-Drive", driveType == "Self-Drive", Modifier.weight(1f)) { onDriveType("Self-Drive") }
            OptionButton("With Driver", driveType == "With Driver", Modifier.weight(1f)) { onDriveType("With Driver") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionButton("Pick-up", serviceType == "Pick-up", Modifier.weight(1f)) { onServiceType("Pick-up") }
            OptionButton("Delivery", serviceType == "Delivery", Modifier.weight(1f)) { onServiceType("Delivery") }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Schedule & Times", fontWeight = FontWeight.Bold, color = Color.Black)
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                OutlinedTextField(
                    value = pickupDate, onValueChange = {}, 
                    label = { Text("Pick-up Date", color = Color.DarkGray) }, 
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    colors = textFieldColors, textStyle = textStyle
                )
                Box(modifier = Modifier.matchParentSize().clickable { showPickupDatePicker = true })
            }
            Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                OutlinedTextField(
                    value = returnDate, onValueChange = {}, 
                    label = { Text("Return Date", color = Color.DarkGray) }, 
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    colors = textFieldColors, textStyle = textStyle
                )
                Box(modifier = Modifier.matchParentSize().clickable { showReturnDatePicker = true })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                OutlinedTextField(
                    value = pickupTime, onValueChange = {}, 
                    label = { Text("Pick-up Time", color = Color.DarkGray) }, 
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    trailingIcon = { Icon(Icons.Filled.AccessTime, null, tint = SportRed) },
                    colors = textFieldColors, textStyle = textStyle
                )
                Box(modifier = Modifier.matchParentSize().clickable { showPickupTimePicker = true })
            }
            Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                OutlinedTextField(
                    value = returnTime, onValueChange = {}, 
                    label = { Text("Return Time", color = Color.DarkGray) }, 
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    trailingIcon = { Icon(Icons.Filled.AccessTime, null, tint = SportRed) },
                    colors = textFieldColors, textStyle = textStyle
                )
                Box(modifier = Modifier.matchParentSize().clickable { showReturnTimePicker = true })
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Locations", fontWeight = FontWeight.Bold, color = Color.Black)
        if (serviceType == "Pick-up") {
            InfoCardItem("Pick-up Point", car.location, Icons.Filled.LocationOn)
        } else {
            OutlinedTextField(
                value = deliveryAddress, onValueChange = onDelivery, 
                label = { Text("Delivery Address", color = Color.DarkGray) }, 
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors, textStyle = textStyle
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = returnAddress, onValueChange = onReturnLoc, 
            label = { Text("Return Location", color = Color.DarkGray) }, 
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors, textStyle = textStyle
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, enabled = returnAddress.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SportRed), shape = RoundedCornerShape(16.dp)) {
            Text("Next: Payment", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PaymentMethodStep(onPaymentConfirmed: (PaymentMethod, String) -> Unit) {
    val methods = listOf(PaymentMethod("GCash"), PaymentMethod("Maya"), PaymentMethod("BDO"))
    var showQrFor by remember { mutableStateOf<PaymentMethod?>(null) }
    var proofUploaded by remember { mutableStateOf(false) }
    Column {
        Text("Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Reservation Fee: ₱500.00", color = SportRed, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        methods.forEach { method ->
            PaymentMethodItem(method, showQrFor == method) { showQrFor = it }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (showQrFor != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(160.dp).background(Color.LightGray)) { Text("QR", Modifier.align(Alignment.Center)) }
                Button(onClick = { proofUploaded = true }, modifier = Modifier.padding(top = 16.dp)) { Text("Upload Receipt") }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onPaymentConfirmed(showQrFor!!, "receipt_url") }, enabled = proofUploaded, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SportRed)) {
            Text("Confirm & Verify Identity")
        }
    }
}

@Composable
fun BookingSummaryStep(car: Car, user: User?, pickupDate: String, returnDate: String, pickupTime: String, returnTime: String, driveType: String, serviceType: String, pickupLoc: String, returnLoc: String, days: Int, payment: PaymentMethod, onComplete: () -> Unit) {
    val total = car.price * days
    Column {
        Text("Final Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryRow("User", "${user?.firstName} ${user?.lastName}")
                SummaryRow("Vehicle", car.model)
                SummaryRow("Type", "$driveType ($serviceType)")
                SummaryRow("Schedule", "$pickupDate ($pickupTime) to $returnDate ($returnTime)")
                SummaryRow("Pick-up", pickupLoc)
                SummaryRow("Return", returnLoc)
                SummaryRow("Payment", payment.name)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SummaryRow("Total Price", "₱${String.format(Locale.US, "%,.2f", total)}")
                SummaryRow("Balance", "₱${String.format(Locale.US, "%,.2f", total - 500)}", SportRed)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SportRed.copy(alpha = 0.05f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = SportRed)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Owner: ${car.ownerContact}", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SportRed)) {
            Text("Complete Booking", fontWeight = FontWeight.Bold)
        }
    }
}

// Helpers
@Composable
fun OptionButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) SportRed else Color(0xFFF5F5F5)).clickable { onClick() }.border(1.dp, if (selected) SportRed else Color.LightGray, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else Color.Black, fontSize = 13.sp)
    }
}

@Composable
fun PaymentMethodItem(method: PaymentMethod, selected: Boolean, onSelect: (PaymentMethod) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) SportRed.copy(alpha = 0.1f) else Color(0xFFF5F5F5)).clickable { onSelect(method) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = { onSelect(method) }, colors = RadioButtonDefaults.colors(selectedColor = SportRed))
        Text(method.name, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun SummaryRow(l: String, v: String, c: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(l, color = Color.Gray, fontSize = 13.sp)
        Text(v, fontWeight = FontWeight.Bold, color = c, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
}

@Composable
fun InfoCardItem(l: String, v: String, i: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8F8F8)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(i, null, tint = SportRed, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column { 
            Text(l, fontSize = 10.sp, color = Color.Gray)
            Text(v, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp) 
        }
    }
}

fun calculateDays(s: String, e: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val d1 = sdf.parse(s)
        val d2 = sdf.parse(e)
        val diff = d2!!.time - d1!!.time
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        if (days < 1) 1 else days
    } catch (_: Exception) { 1 }
}

data class PaymentMethod(val name: String)
