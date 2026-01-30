package com.example.xcelrent

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val userId: String = "",
    val carId: String = "",
    val carModel: String = "",
    val plateNumber: String = "",
    val pickupLocation: String = "",
    val deliveryLocation: String = "",
    val paymentMethod: String = "",
    val reservationFee: Double = 500.0,
    val totalPrice: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val status: String = "Pending", // Pending, Confirmed, On-going, Completed, Cancelled
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String = ""
)
