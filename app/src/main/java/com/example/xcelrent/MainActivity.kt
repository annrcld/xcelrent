package com.example.xcelrent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                    composable("admin_home") { AdminHomeScreen(navController) }
                    composable("profile") { ProfileScreen(navController) }
                    composable("mytrips") { MyTripsScreen(navController) }
                    composable(
                        route = "details/{carId}?pickup={pickup}&return={return}",
                        arguments = listOf(
                            navArgument("carId") { type = NavType.StringType },
                            navArgument("pickup") { type = NavType.StringType; defaultValue = "" },
                            navArgument("return") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val carId = backStackEntry.arguments?.getString("carId")
                        val pickup = backStackEntry.arguments?.getString("pickup") ?: ""
                        val returnDate = backStackEntry.arguments?.getString("return") ?: ""
                        CarDetailsScreen(carId, pickup, returnDate, navController)
                    }
                    composable(
                        route = "booking_process/{carId}?pickup={pickup}&return={return}",
                        arguments = listOf(
                            navArgument("carId") { type = NavType.StringType },
                            navArgument("pickup") { type = NavType.StringType; defaultValue = "" },
                            navArgument("return") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val carId = backStackEntry.arguments?.getString("carId")
                        val pickup = backStackEntry.arguments?.getString("pickup") ?: ""
                        val returnDate = backStackEntry.arguments?.getString("return") ?: ""
                        BookingProcessScreen(carId, pickup, returnDate, navController)
                    }
                }
            }
        }
    }
}
