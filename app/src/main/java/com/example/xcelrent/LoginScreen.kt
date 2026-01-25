package com.example.xcelrent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.xcelrent.ui.theme.InterFamily
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val sportRed = Color(0xFFE53935)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp, vertical = 60.dp)
    ) {
        // 1. Brand Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Xcelrent",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = ".",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = sportRed
            )
        }

        Spacer(modifier = Modifier.height(100.dp))

        // 2. Heading
        Text(
            text = "Welcome Back.",
            style = MaterialTheme.typography.displayMedium,
            fontFamily = InterFamily,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF212121)
        )
        Text(
            text = "Log in to continue.",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = InterFamily,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 3. Input Fields (Reusing your professional AccountTextField style)
        AccountTextField(
            value = email,
            onValue = { email = it },
            label = "Email Address"
        )

        AccountTextField(
            value = password,
            onValue = { password = it },
            label = "Password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 4. Login Button
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnSuccessListener {
                            isLoading = false
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            Toast.makeText(context, "Login Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = sportRed),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Footer
        Text(
            text = "Don't have an account? Sign Up",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { navController.navigate("create_account") },
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = InterFamily,
            color = Color.DarkGray
        )
    }
}