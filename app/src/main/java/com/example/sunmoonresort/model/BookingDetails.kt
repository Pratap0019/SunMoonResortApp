package com.example.sunmoonresort.model

data class BookingDetails(
    val bookingId: String,
    val guest: Guest,
    val bill: Bill,
    val daysStayed: Int,
    val checkInDate: String,
    val checkOutDate: String,
    var status: BookingStatus = BookingStatus.CONFIRMED,
) {
    fun getBookingId(): String = bookingId
    fun getGuest(): Guest = guest
    fun getBill(): Bill = bill
    fun getDaysStayed(): Int = daysStayed
    fun getCheckInDate(): String = checkInDate
    fun getCheckOutDate(): String = checkOutDate
    fun getStatus(): BookingStatus = status
    fun setStatus(newStatus: BookingStatus) {
        status = newStatus
    }
}

