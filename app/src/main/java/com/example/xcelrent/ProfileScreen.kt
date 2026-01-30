package com.example.xcelrent

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.xcelrent.ui.theme.InterFamily
import com.example.xcelrent.ui.theme.SportRed
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import com.example.xcelrent.ui.theme.InterFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    // Form States
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") } // This will track the editable email
    var contactNum by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    LaunchedEffect(auth.currentUser) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val fetchedUser = document.toObject(User::class.java)
                    user = fetchedUser
                    fetchedUser?.let {
                        firstName = it.firstName ?: ""
                        lastName = it.lastName ?: ""
                        email = it.email ?: currentUser.email ?: ""
                        contactNum = it.contactNum ?: ""
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    fun handleUpdateProfile() {
        val currentUser = auth.currentUser ?: return

        // 1. Basic Validation
        if (currentPassword.isEmpty()) {
            Toast.makeText(context, "Identity verification required to save changes.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Password Match Validation (Only if they want to change it)
        if (newPassword.isNotEmpty() && newPassword != confirmNewPassword) {
            Toast.makeText(context, "New passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)

        // STEP 1: Re-authenticate to prove identity
        currentUser.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
            if (reAuthTask.isSuccessful) {

                val tasks = mutableListOf<com.google.android.gms.tasks.Task<Void>>()

                // STEP 2: Handle Email Change (Verify before update)
                if (email != currentUser.email) {
                    // This sends a verification link to the NEW email
                    // The email only actually changes in Auth once they click the link
                    tasks.add(currentUser.verifyBeforeUpdateEmail(email))
                }

                // STEP 3: Handle Password Change (Optional)
                if (newPassword.isNotEmpty()) {
                    tasks.add(currentUser.updatePassword(newPassword))
                }

                // STEP 4: Run all Auth tasks, then update Firestore
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    val updates = mutableMapOf<String, Any>(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email, // We keep the target email in Firestore for display
                        "contactNum" to contactNum
                    )

                    db.collection("users").document(currentUser.uid).update(updates)
                        .addOnSuccessListener {
                            isLoading = false
                            isEditing = false
                            currentPassword = "" // Clear sensitive fields
                            newPassword = ""
                            confirmNewPassword = ""

                            val msg = if (email != currentUser.email)
                                "Profile saved. Please verify your new email address."
                            else "Profile updated successfully."
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Sync error. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                isLoading = false
                Toast.makeText(context, "Verification failed. Check your current password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Account", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                actions = {
                    TextButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                            // Reset local state to original user data if cancelled
                            user?.let {
                                firstName = it.firstName ?: ""
                                lastName = it.lastName ?: ""
                                email = it.email ?: ""
                                contactNum = it.contactNum ?: ""
                            }
                        } else {
                            isEditing = true
                        }
                    }) {
                        Text(if (isEditing) "Cancel" else "Edit", color = if (isEditing) Color.Gray else SportRed)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SportRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeader(firstName, lastName, email)

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (isEditing) {
                        Text("Personal Information", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                        EditField(label = "First Name", value = firstName) { firstName = it }
                        EditField(label = "Last Name", value = lastName) { lastName = it }
                        EditField(label = "Email Address", value = email) { email = it }
                        EditField(label = "Phone Number", value = contactNum) { contactNum = it }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Security", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                        Text("Enter your current password to save any changes.", style = MaterialTheme.typography.bodySmall, color = SportRed)
                        EditField(label = "Current Password", value = currentPassword, isPassword = true) { currentPassword = it }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Change Password (Optional)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        EditField(label = "New Password", value = newPassword, isPassword = true) { newPassword = it }
                        EditField(label = "Confirm New Password", value = confirmNewPassword, isPassword = true) { confirmNewPassword = it }

                        Button(
                            onClick = { handleUpdateProfile() },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // ... Rest of the UI (InfoSection, Sign Out, etc.) remains same ...
                        InfoSection(title = "Contact Details") {
                            InfoRow(icon = Icons.Outlined.Person, label = "Full Name", value = "$firstName $lastName")
                            InfoRow(icon = Icons.Outlined.Email, label = "Email", value = email)
                            InfoRow(icon = Icons.Outlined.Phone, label = "Phone", value = contactNum.ifEmpty { "Not set" })
                        }

                        // ... SocialRows and Sign Out ...
                        Spacer(modifier = Modifier.height(16.dp))
                        InfoSection(title = "Support") {
                            SocialRow("Facebook") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/xcelrent"))) }
                            SocialRow("TikTok") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tiktok.com/@xcelrent"))) }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(
                            onClick = {
                                auth.signOut()
                                navController.navigate("landing") { popUpTo(0) }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SportRed)
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SportRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sign Out",
                                color = SportRed, // Changed text color
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun EditField(label: String, value: String, isPassword: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedTextColor = Color.Black
        )
    )
}

@Composable
fun ProfileHeader(firstName: String, lastName: String, email: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstName.take(1).uppercase(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "$firstName $lastName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 0.5.dp
        ) {
            Column { content() }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SocialRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)