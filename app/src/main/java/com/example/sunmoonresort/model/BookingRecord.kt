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
) {
    fun getBookingId(): String = bookingId
    fun getRoomNumber(): Int = roomNumber
    fun getGuestName(): String = guestName
    fun getContactNumber(): String = contactNumber
    fun getRoomType(): String = roomType
    fun getBill(): Bill = bill
    fun getDaysStayed(): Int = daysStayed
    fun getCheckInDate(): String = checkInDate
    fun getCheckOutDate(): String = checkOutDate
    fun getStatus(): BookingStatus = status
}

