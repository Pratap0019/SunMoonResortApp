package com.example.sunmoonresort.model

data class BookingDetails(
    val bookingId: String,
    val guest: Guest,
    val bill: Bill,
    val daysStayed: Int,
    val checkInDate: String,
    val checkOutDate: String,
    var status: BookingStatus = BookingStatus.CONFIRMED,
)

