package com.example.xcelrent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.xcelrent.ui.theme.InterFamily
import com.example.xcelrent.ui.theme.SportRed
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

sealed class AdminTab(val title: String, val icon: ImageVector) {
    object Bookings : AdminTab("Bookings", Icons.AutoMirrored.Filled.Assignment)
    object Inventory : AdminTab("Inventory", Icons.Default.DirectionsCar)
    object Users : AdminTab("Users", Icons.Default.People)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableStateOf<AdminTab>(AdminTab.Bookings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.title, fontFamily = InterFamily, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("login") {
                            popUpTo("admin_home") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf(AdminTab.Bookings, AdminTab.Inventory, AdminTab.Users)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title, fontFamily = InterFamily) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SportRed,
                            selectedTextColor = SportRed,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.White
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F8F8)
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                AdminTab.Bookings -> AdminBookingsSection(db)
                AdminTab.Inventory -> AdminInventorySection(db)
                AdminTab.Users -> AdminUsersSection(db)
            }
        }
    }
}

@Composable
fun AdminBookingsSection(db: FirebaseFirestore) {
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    fun fetchBookings() {
        isLoading = true
        db.collection("bookings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                bookings = querySnapshot.toObjects(Booking::class.java)
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) { fetchBookings() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SportRed)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AdminDashboardSummary(bookings)
            }
            items(bookings) { booking ->
                AdminBookingCard(booking) { newStatus ->
                    db.collection("bookings").document(booking.id)
                        .update("status", newStatus)
                        .addOnSuccessListener { fetchBookings() }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardSummary(bookings: List<Booking>) {
    val totalRevenue = bookings.filter { it.status == "Completed" || it.status == "On-going" }.sumOf { it.totalPrice }
    val activeBookings = bookings.count { it.status == "On-going" || it.status == "Confirmed" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdminSummaryStatCard("Revenue", "₱${String.format(Locale.US, "%,.0f", totalRevenue)}", Icons.Default.Payments, Modifier.weight(1f))
        AdminSummaryStatCard("Active", "$activeBookings", Icons.Default.DirectionsCar, Modifier.weight(1f))
    }
}

@Composable
fun AdminSummaryStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = SportRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AdminInventorySection(db: FirebaseFirestore) {
    var cars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var carToEdit by remember { mutableStateOf<Car?>(null) }
    val context = LocalContext.current

    fun fetchCars() {
        isLoading = true
        db.collection("cars")
            .get()
            .addOnSuccessListener { querySnapshot ->
                cars = querySnapshot.toObjects(Car::class.java)
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Failed to load inventory: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) {
        fetchCars()
    }

    if (showAddDialog) {
        AdminCarDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { car ->
                db.collection("cars").document(car.id).set(car)
                    .addOnSuccessListener {
                        showAddDialog = false
                        fetchCars() // Refresh the list
                        Toast.makeText(context, "Car added to inventory", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    if (carToEdit != null) {
        AdminCarDialog(
            car = carToEdit,
            onDismiss = { carToEdit = null },
            onConfirm = { updatedCar ->
                db.collection("cars").document(updatedCar.id).set(updatedCar)
                    .addOnSuccessListener {
                        carToEdit = null
                        fetchCars() // Refresh the list
                        Toast.makeText(context, "Car updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error updating car: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SportRed)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Vehicle", fontWeight = FontWeight.Bold)
                }
            }
            items(cars) { car ->
                AdminCarCard(
                    car = car,
                    onEdit = { carToEdit = car },
                    onDelete = {
                        db.collection("cars").document(car.id).delete()
                            .addOnSuccessListener { fetchCars() }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminUsersSection(db: FirebaseFirestore) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").get().addOnSuccessListener {
            users = it.toObjects(User::class.java)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SportRed)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                AdminUserCard(user)
            }
        }
    }
}

@Composable
fun AdminCarCard(car: Car, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(car.model, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
                Text("₱${String.format(Locale.US, "%,.0f", car.price)}/day", color = SportRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(car.plateNumber, fontSize = 12.sp, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(SportRed.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(user.firstName.take(1), color = SportRed, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
                Text(user.contactNum, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AdminBookingCard(booking: Booking, onUpdateStatus: (String) -> Unit) {
    var showDocs by remember { mutableStateOf(false) }

    if (showDocs) {
        AdminBookingDocsDialog(booking) { showDocs = false }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = booking.imageUrl,
                    contentDescription = booking.carModel,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(booking.carModel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
                    Text("Plate: ${booking.plateNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    AdminStatusBadge(booking.status)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

            AdminBookingDetailRow("Renter ID", booking.userId)
            AdminBookingDetailRow("Schedule", "${booking.pickupDate} to ${booking.returnDate}")
            AdminBookingDetailRow("Type", "${booking.driveType} | ${booking.serviceType}")
            AdminBookingDetailRow("Total", "₱${String.format(Locale.US, "%,.2f", booking.totalPrice)}")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDocs = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View Docs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                when (booking.status) {
                    "Pending" -> {
                        Button(onClick = { onUpdateStatus("Confirmed") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp)) {
                            Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(onClick = { onUpdateStatus("Cancelled") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SportRed), shape = RoundedCornerShape(8.dp)) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    "Confirmed" -> {
                        Button(onClick = { onUpdateStatus("On-going") }, modifier = Modifier.weight(1.5f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), shape = RoundedCornerShape(8.dp)) {
                            Text("Mark On-going", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    "On-going" -> {
                        Button(onClick = { onUpdateStatus("Completed") }, modifier = Modifier.weight(1.5f), colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(8.dp)) {
                            Text("Mark Completed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBookingDocsDialog(booking: Booking, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Verification Documents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { AdminDocItem("Driver's License", booking.driversLicenseUrl) }
                    item { AdminDocItem("LTO Portal QR", booking.ltoQrUrl) }
                    item { AdminDocItem("Proof of Billing", booking.proofOfBillingUrl) }
                    item { AdminDocItem("Selfie with ID", booking.selfieWithIdUrl) }
                    item { AdminDocItem("Secondary ID", booking.secondValidIdUrl) }
                    item { AdminDocItem("Payment Proof", booking.paymentProofUrl) }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SportRed)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AdminDocItem(label: String, url: String) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        if (url.startsWith("dummy") || url.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No image provided", color = Color.LightGray)
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun AdminStatusBadge(status: String) {
    val bgColor = when (status) {
        "Pending" -> Color(0xFFFFB300).copy(alpha = 0.1f)
        "Confirmed" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        "On-going" -> Color(0xFF2196F3).copy(alpha = 0.1f)
        "Completed" -> Color(0xFF757575).copy(alpha = 0.1f)
        else -> Color.Red.copy(alpha = 0.1f)
    }
    val txtColor = when (status) {
        "Pending" -> Color(0xFFFFB300)
        "Confirmed" -> Color(0xFF4CAF50)
        "On-going" -> Color(0xFF2196F3)
        "Completed" -> Color(0xFF757575)
        else -> Color.Red
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = txtColor
        )
    }
}

@Composable
fun AdminBookingDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontFamily = InterFamily)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black, fontFamily = InterFamily)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCarDialog(car: Car? = null, onDismiss: () -> Unit, onConfirm: (Car) -> Unit) {
    var model by remember { mutableStateOf(car?.model ?: "") }
    var price by remember { mutableStateOf(car?.price?.toString() ?: "") }
    var plate by remember { mutableStateOf(car?.plateNumber ?: "") }
    var location by remember { mutableStateOf(car?.location ?: "") }
    var imageUrl by remember { mutableStateOf(car?.imageUrl ?: "") }
    
    // Dropdown States
    // Parse specs if editing: e.g. "Automatic • 5 Seats • Sedan"
    val initialTransmission = car?.specs?.split(" • ")?.getOrNull(0) ?: "Automatic"
    val initialSeaters = car?.specs?.split(" • ")?.getOrNull(1)?.removeSuffix(" Seats") ?: "5"
    val initialType = car?.specs?.split(" • ")?.getOrNull(2) ?: "Sedan"

    var vehicleType by remember { mutableStateOf(initialType) }
    var seaters by remember { mutableStateOf(initialSeaters) }
    var transmission by remember { mutableStateOf(initialTransmission) }

    val vehicleTypes = listOf("Sedan", "SUV", "Van")
    val transmissionTypes = listOf("Manual", "Automatic")
    
    val seaterOptions = when (vehicleType) {
        "Sedan" -> listOf("4", "5")
        "SUV" -> listOf("7", "8")
        "Van" -> listOf("10", "11", "12", "13", "14", "15")
        else -> listOf("5")
    }

    // Update seaters if current selection is not in the new options
    LaunchedEffect(vehicleType) {
        if (seaters !in seaterOptions) {
            seaters = seaterOptions.first()
        }
    }

    val isFormValid = model.isNotBlank() && 
                      price.isNotBlank() && 
                      plate.isNotBlank() && 
                      location.isNotBlank() && 
                      imageUrl.isNotBlank()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Gray,
        cursorColor = Color.Black,
        focusedBorderColor = SportRed,
        unfocusedBorderColor = Color.LightGray
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            LazyColumn(modifier = Modifier.padding(24.dp)) {
                item {
                    Text(if (car == null) "Add New Vehicle" else "Edit Vehicle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model Name") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Daily Price") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Plate Number") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)

                    Spacer(Modifier.height(16.dp))
                    Text("Vehicle Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))

                    // Vehicle Type Dropdown
                    AdminDropdownField("Vehicle Type", vehicleType, vehicleTypes, textFieldColors) { vehicleType = it }
                    
                    // Seater Dropdown (Dependent)
                    AdminDropdownField("Seaters", seaters, seaterOptions, textFieldColors) { seaters = it }
                    
                    // Transmission Dropdown
                    AdminDropdownField("Transmission", transmission, transmissionTypes, textFieldColors) { transmission = it }

                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel", color = Color.Gray) }
                        Button(
                            onClick = {
                                if (isFormValid) {
                                    val finalSpecs = "$transmission • $seaters Seats • $vehicleType"
                                    val finalCar = Car(
                                        id = car?.id ?: "CAR_${System.currentTimeMillis()}",
                                        model = model,
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        specs = finalSpecs,
                                        plateNumber = plate,
                                        location = location,
                                        imageUrl = imageUrl,
                                        status = car?.status ?: "Live"
                                    )
                                    onConfirm(finalCar)
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SportRed,
                                disabledContainerColor = Color.LightGray
                            )
                        ) { Text(if (car == null) "Add Car" else "Update Car", color = Color.White) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDropdownField(label: String, selected: String, options: List<String>, colors: TextFieldColors, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = colors,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.Black) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
