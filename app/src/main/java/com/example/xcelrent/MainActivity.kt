package com.example.xcelrent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.xcelrent.ui.theme.XcelrentTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        setContent {
            XcelrentTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "landing") {
                    composable("landing") { LandingScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("create_account") { CreateAccountScreen(navController) }
                    composable("home") { HomeScreen(navController) }
                    composable("profile") { ProfileScreen(navController) }
                    composable("mytrips") { MyTripsScreen(navController) }
                    composable("details/{carId}") { backStackEntry ->
                        val carId = backStackEntry.arguments?.getString("carId")
                        CarDetailsScreen(carId, navController)
                    }
                    composable("booking_process/{carId}") { backStackEntry ->
                        val carId = backStackEntry.arguments?.getString("carId")
                        BookingProcessScreen(carId, navController)
                    }
                }
            }
        }
    }
}
