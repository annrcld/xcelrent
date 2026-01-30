package com.example.xcelrent

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.xcelrent.ui.theme.InterFamily
import com.example.xcelrent.ui.theme.SportRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsScreen(carId: String?, pickup: String, returnDate: String, navController: NavController) {
    val car = carList.find { it.id == carId } ?: return

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    var pickupDateTime by remember { 
        mutableStateOf(
            try { 
                val cal = Calendar.getInstance()
                cal.time = sdf.parse(pickup)!!
                cal
            } catch (e: Exception) { 
                Calendar.getInstance() 
            }
        )
    }
    
    var returnDateTime by remember { 
        mutableStateOf(
            try { 
                val cal = Calendar.getInstance()
                cal.time = sdf.parse(returnDate)!!
                cal
            } catch (e: Exception) { 
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(car.model, fontFamily = InterFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.model,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                error = painterResource(id = R.drawable.ic_launcher_foreground)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = car.model,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFamily
                    )
                    Text(
                        text = car.specs,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        fontFamily = InterFamily
                    )
                }
                Text(
                    text = "₱${car.price.toInt()}/day",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SportRed,
                    fontFamily = InterFamily
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date Selection in Details Screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DateTimeSelectorSmall("Pickup", pickupDateTime, Modifier.weight(1f)) { pickupDateTime = it }
                    Spacer(modifier = Modifier.width(16.dp))
                    DateTimeSelectorSmall("Return", returnDateTime, Modifier.weight(1f)) { returnDateTime = it }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Specifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecItem(icon = Icons.Filled.AirlineSeatReclineNormal, label = "4 Seats", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                SpecItem(icon = Icons.Filled.LocalGasStation, label = "Petrol", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                SpecItem(icon = Icons.Filled.Settings, label = "Automatic", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Features", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
            Spacer(modifier = Modifier.height(16.dp))
            FeatureItem("Bluetooth Connectivity")
            FeatureItem("Rear View Camera")
            FeatureItem("GPS Navigation")
            FeatureItem("Sunroof")

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { 
                    val pStr = sdf.format(pickupDateTime.time)
                    val rStr = sdf.format(returnDateTime.time)
                    navController.navigate("booking_process/${car.id}?pickup=$pStr&return=$rStr") 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Proceed to Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelectorSmall(
    label: String,
    dateTime: Calendar,
    modifier: Modifier = Modifier,
    onDateTimeSelected: (Calendar) -> Unit
) {
    // Reuse the same logic as HomeScreen but maybe more compact if needed
    // For now, let's just make it clickable and show dialogs. 
    // I will use DateTimeSelector from HomeScreen logic but simplified.
    
    var showDatePicker by remember { mutableStateOf(false) }
    val tempDatePickerState = rememberDatePickerState(initialSelectedDateMillis = dateTime.timeInMillis)
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(modifier = modifier.clickable { showDatePicker = true }) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontFamily = InterFamily)
        Text(dateFormatter.format(dateTime.time), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    showDatePicker = false
                    val calendar = Calendar.getInstance()
                    tempDatePickerState.selectedDateMillis?.let { calendar.timeInMillis = it }
                    onDateTimeSelected(calendar)
                }) {
                    Text("OK", color = SportRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = tempDatePickerState)
        }
    }
}

@Composable
fun SpecItem(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = SportRed)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontFamily = InterFamily)
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontFamily = InterFamily)
    }
}
