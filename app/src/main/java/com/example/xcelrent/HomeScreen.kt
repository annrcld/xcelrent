package com.example.xcelrent

import androidx.compose.animation.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.xcelrent.ui.theme.InterFamily
import com.example.xcelrent.ui.theme.SportRed
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

sealed class BottomBarScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomBarScreen("home", "Home", Icons.Filled.Home)
    object MyTrips : BottomBarScreen("mytrips", "My Trips", Icons.Filled.Commute)
    object Profile : BottomBarScreen("profile", "Profile", Icons.Filled.Person)
}

val carList = listOf(
    Car("car_001", "Toyota Camry", 2500.0, "4-door, A/C", "https://images.hgmsites.net/med/2023-toyota-camry-se-auto-natl-angular-front-exterior-view_100857360_m.jpg", "Live", "NDS 1234", "Quezon City, 1100 Metro Manila", "+63 917 123 4567"),
    Car("car_002", "Honda CR-V", 3500.0, "5-door, SUV", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTFb_P9pn8AGyKRVw66bk28SMPjQ3EHIzGePQ&s", "Live", "GHT 5678", "Makati City, 1200 Metro Manila", "+63 918 234 5678"),
    Car("car_003", "BMW 3 Series", 5500.0, "Luxury, Sport", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQoE5_w-VuOKWtlS9i1xM_NUPbZV__Usy8rLg&s", "Live", "BMR 999", "BGC, Taguig, 1634 Metro Manila", "+63 919 345 6789"),
    Car("car_004", "Ford Mustang", 8000.0, "2-door, Coupe", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQhCfUi1d8kENke9r9oRkrIvTD_0jObZkYBcA&s", "Live", "FRD 001", "Ortigas, Pasig City, 1600", "+63 920 456 7890")
)

@Composable
fun HomeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var allCars by remember { mutableStateOf<List<Car>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Car>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var pickupDateTime by remember { mutableStateOf(Calendar.getInstance()) }
    var returnDateTime by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }) }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val pickupStr = sdf.format(pickupDateTime.time)
    val returnStr = sdf.format(returnDateTime.time)

    LaunchedEffect(Unit) {
        db.collection("cars").get().addOnSuccessListener { snapshot ->
            allCars = snapshot.toObjects(Car::class.java)
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    Scaffold(
        topBar = { HomeTopBar() },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).background(Color.White)) {
            SearchCarCard(pickupDateTime, returnDateTime, { pickupDateTime = it }, { returnDateTime = it }, { searchResults = allCars.filter { it.status == "Live" }.shuffled() })
            Spacer(modifier = Modifier.height(32.dp))
            WhyChooseUsSection()
            Spacer(modifier = Modifier.height(32.dp))
            PromoBanner()
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportRed)
                }
            } else {
                AnimatedContent(targetState = searchResults, label = "") { results ->
                    CarListSection(navController, results ?: allCars, if (results != null) "Available Cars" else "Top Rated", pickupStr, returnStr)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            FaqSection()
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val screens = listOf(BottomBarScreen.Home, BottomBarScreen.MyTrips, BottomBarScreen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        screens.forEach { screen ->
            NavigationBarItem(
                label = { Text(text = screen.title, fontFamily = InterFamily, fontWeight = FontWeight.Medium) },
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true } },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = SportRed, selectedTextColor = SportRed, unselectedIconColor = Color.Gray, indicatorColor = Color.White)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Your Location", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = InterFamily)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = SportRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manila, Philippines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, maxLines = 1, color = Color.Black)
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.Gray)
            }
        }
        BadgedBox(badge = { Badge(containerColor = SportRed) { Text("3", color = Color.White) } }) {
            Icon(Icons.Filled.Notifications, "Notifications", tint = Color.Black, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun SearchCarCard(pickup: Calendar, ret: Calendar, onPickup: (Calendar) -> Unit, onReturn: (Calendar) -> Unit, onSearch: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DateTimeSelector("Pickup", pickup, Modifier.weight(1f), onPickup)
                Spacer(modifier = Modifier.width(16.dp))
                DateTimeSelector("Return", ret, Modifier.weight(1f), onReturn)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSearch, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SportRed), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Search, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp))
                Text("Search for a Car", style = MaterialTheme.typography.titleMedium, fontFamily = InterFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelector(label: String, dateTime: Calendar, modifier: Modifier = Modifier, onDateTimeSelected: (Calendar) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val tempDatePickerState = rememberDatePickerState(initialSelectedDateMillis = dateTime.timeInMillis)
    val tempTimePickerState = rememberTimePickerState(initialHour = dateTime.get(Calendar.HOUR_OF_DAY), initialMinute = dateTime.get(Calendar.MINUTE), is24Hour = false)
    val dateFormatter = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    Column(modifier = modifier.clickable { showDatePicker = true }) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, null, tint = SportRed, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(dateFormatter.format(dateTime.time), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = Color.Black)
                    Text(timeFormatter.format(dateTime.time), style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = InterFamily)
                }
            }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false; showTimePicker = true }) { Text("OK", fontWeight = FontWeight.Bold, color = SportRed) } }) {
            DatePicker(state = tempDatePickerState)
        }
    }
    if (showTimePicker) {
        MinimalTimePickerDialog(onDismiss = { showTimePicker = false }, onConfirm = {
            showTimePicker = false; val calendar = Calendar.getInstance()
            tempDatePickerState.selectedDateMillis?.let { calendar.timeInMillis = it }
            calendar.set(Calendar.HOUR_OF_DAY, tempTimePickerState.hour); calendar.set(Calendar.MINUTE, tempTimePickerState.minute)
            onDateTimeSelected(calendar)
        }, state = tempTimePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalTimePickerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, state: TimePickerState) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp), color = Color.Black)
                TimePicker(state = state, colors = TimePickerDefaults.colors(selectorColor = SportRed, timeSelectorSelectedContainerColor = SportRed))
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    TextButton(onClick = onConfirm) { Text("OK", fontWeight = FontWeight.Bold, color = SportRed) }
                }
            }
        }
    }
}

@Composable
fun WhyChooseUsSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Why Choose Us?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoCard(Icons.Filled.AttachMoney, "No Hidden Fees", "Prices are final.", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            InfoCard(Icons.Filled.SupportAgent, "24/7 Support", "We are always here.", Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            InfoCard(Icons.Filled.EventAvailable, "Free Cancellation", "Change of plans?", Modifier.weight(1f))
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Icon(imageVector = icon, contentDescription = title, tint = SportRed); Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun PromoBanner() {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(120.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SportRed), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Get 20% Off Your First Ride!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Use code: XCELFIRST", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
            Icon(Icons.Filled.NewReleases, "Discount", tint = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableCard(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = Color.Black)
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Color.Black)
            }
            AnimatedVisibility(visible = expanded) { Text(answer, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
fun FaqSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Frequently Asked Questions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        ExpandableCard("How do I extend my rental?", "You can extend your rental through the app. Go to your current rental details.")
        Spacer(modifier = Modifier.height(16.dp))
        ExpandableCard("Is insurance included?", "Yes, basic insurance is included in the daily price.")
    }
}

@Composable
fun CarListSection(navController: NavController, cars: List<Car>, title: String, pickupStr: String, returnStr: String) {
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        cars.forEach { car -> CarListItem(car) { navController.navigate("details/${car.id}?pickup=$pickupStr&return=$returnStr") } }
    }
}

@Composable
fun CarListItem(car: Car, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = car.imageUrl, contentDescription = car.model, contentScale = ContentScale.Fit, modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(car.model, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AirlineSeatReclineNormal, null, modifier = Modifier.size(14.dp), tint = Color.Gray); Spacer(modifier = Modifier.width(4.dp))
                    Text(car.specs.ifEmpty { "4 Seaters" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text("See Details", style = MaterialTheme.typography.labelMedium, color = SportRed, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₱${car.price.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SportRed)
                Text("/day", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() { HomeScreen(rememberNavController()) }
