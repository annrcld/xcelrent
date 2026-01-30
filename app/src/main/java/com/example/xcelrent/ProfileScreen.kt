package com.example.xcelrent

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.xcelrent.ui.theme.InterFamily
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch user data from Firestore
    LaunchedEffect(auth.currentUser) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        user = document.toObject(User::class.java)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontFamily = InterFamily, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Integrating the Bottom Navigation Bar here
            BottomNavigationBar(navController = navController)
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Not logged in", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        navController.navigate("landing") {
                            popUpTo(0)
                        }
                    }) {
                        Text("Go to Landing Page")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header
                ProfileHeader(user)

                Spacer(modifier = Modifier.height(24.dp))

                // Account Section
                ProfileSection(title = "Account") {
                    ProfileItem(icon = Icons.Filled.Person, text = "${user?.firstName} ${user?.lastName}")
                    ProfileItem(icon = Icons.Filled.Email, text = user?.email ?: "No email")
                    ProfileItem(icon = Icons.Filled.Phone, text = user?.contactNum ?: "No contact number")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Contact Us Section
                ProfileSection(title = "Contact Us") {
                    val fbIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/xcelrentcarrental"))
                    val tiktokIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@xcelrent"))

                    SocialProfileItem(text = "Facebook", onClick = { context.startActivity(fbIntent) })
                    SocialProfileItem(text = "TikTok", onClick = { context.startActivity(tiktokIntent) })
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout Button Fixed
                Button(
                    onClick = {
                        auth.signOut()
                        // Navigate to landing and clear everything to prevent back-navigation to profile
                        navController.navigate("landing") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SportRed), // Ensure SportRed is defined in your theme
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }

                // Extra spacer for scrolling clarity above bottom nav
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user?.firstName?.take(1)?.uppercase() ?: "",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${user?.firstName} ${user?.lastName}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user?.email ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ProfileItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SportRed)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SocialProfileItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "Go", modifier = Modifier.size(16.dp), tint = Color.Gray)
    }
}