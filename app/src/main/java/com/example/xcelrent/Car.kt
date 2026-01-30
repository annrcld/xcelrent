package com.example.xcelrent

data class Car(
    val id: String = "",
    val model: String = "",
    val price: Double = 0.0,
    val specs: String = "",
    val imageUrl: String = "",
    val status: String = "Live",
    val plateNumber: String = "",
    val location: String = "",
    val ownerContact: String = "+63 912 345 6789"
)