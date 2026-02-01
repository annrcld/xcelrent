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
    val pickupDate: String = "",
    val returnDate: String = "",
    val pickupTime: String = "",
    val returnTime: String = "",
    val driveType: String = "Self-Drive", // Self-Drive or With Driver
    val serviceType: String = "Pick-up", // Pick-up or Delivery
    val paymentMethod: String = "",
    val paymentProofUrl: String = "",
    val reservationFee: Double = 500.0,
    val totalPrice: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val status: String = "Pending", // Pending, Confirmed, On-going, Completed, Cancelled
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String = "",
    // Credentials
    val driversLicenseUrl: String = "",
    val ltoQrUrl: String = "",
    val proofOfBillingUrl: String = "",
    val selfieWithIdUrl: String = "",
    val secondValidIdUrl: String = ""
)
