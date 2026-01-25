package com.example.xcelrent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.xcelrent.ui.theme.InterFamily

// --- Screen and Data Definitions ---

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen("home", "Home", Icons.Filled.Home)
    object Favorites : BottomBarScreen("favorites", "Favorites", Icons.Filled.Favorite)
    object Profile : BottomBarScreen("profile", "Profile", Icons.Filled.Person)
}

val carList = listOf(
    Car("car_001", "Toyota Camry", 55.0, "4-door, A/C", "https://example.com/camry.png", "Live"),
    Car("car_002", "Honda CR-V", 70.0, "5-door, SUV", "https://example.com/crv.png", "Live"),
    Car("car_003", "BMW 3 Series", 95.0, "Luxury, Sport", "https://example.com/bmw.png", "Live"),
    Car("car_004", "Ford Mustang", 120.0, "2-door, Coupe", "https://example.com/mustang.png", "Live")
)

val SportRed = Color(0xFFE53935)

// --- Main Screen Composable ---

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = { HomeTopBar() },
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.White)
        ) {
            SearchCarCard()
            Spacer(modifier = Modifier.height(32.dp))
            RecommendationSection(navController)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// --- UI Components ---

@Composable
fun BottomNavigationBar(navController: NavController) {
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Favorites,
        BottomBarScreen.Profile
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                label = {
                    Text(text = screen.title, fontFamily = InterFamily, fontWeight = FontWeight.Medium)
                },
                icon = {
                    Icon(imageVector = screen.icon, contentDescription = screen.title)
                },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SportRed,
                    selectedTextColor = SportRed,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Your Location",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontFamily = InterFamily
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = SportRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Manila, Philippines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFamily
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
        BadgedBox(badge = { Badge(containerColor = SportRed) { Text("3") } }) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SearchCarCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DateTimeSelector(label = "Pickup", date = "Mon, 29 Jan", time = "10:00 AM", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                DateTimeSelector(label = "Return", date = "Thu, 01 Feb", time = "10:00 AM", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { /* TODO: Handle Search Click */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search for a Car",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DateTimeSelector(label: String, date: String, time: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = SportRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFamily
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontFamily = InterFamily
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationSection(navController: NavController) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recommendation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFamily
            )
            TextButton(onClick = { /* TODO: Navigate to list */ }) {
                Text(
                    text = "See all",
                    color = SportRed,
                    fontFamily = InterFamily
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(carList) { car ->
                RecommendationCarCard(car = car) {
                    navController.navigate("details/${car.id}")
                }
            }
        }
    }
}

@Composable
fun RecommendationCarCard(car: Car, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = car.imageUrl,
                    contentDescription = car.model,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = car.model,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFamily
            )
            Text(
                text = car.specs,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontFamily = InterFamily
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${car.price}/day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SportRed,
                    fontFamily = InterFamily
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(rememberNavController())
}
