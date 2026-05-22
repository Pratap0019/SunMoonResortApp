package com.example.sunmoonresort.model

data class BookingRecord(
    val bookingId: String,
    val roomNumber: Int,
    val guestName: String,
    val contactNumber: String,
    val roomType: String,
    val bill: Bill,
    val daysStayed: Int,
    val checkInDate: String,
    val checkOutDate: String,
    val status: BookingStatus,
)

