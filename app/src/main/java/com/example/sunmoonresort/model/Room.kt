package com.example.sunmoonresort.model

data class Room(
    val number: Int,
    val roomType: RoomType,
    val booked: Boolean = false,
    val natureView: Boolean = false,
    val balcony: Boolean = false,
) {
    fun getRoomNumber(): Int = number
    fun getRoomType(): RoomType = roomType
    fun isBooked(): Boolean = booked
    fun hasNatureView(): Boolean = natureView
    fun hasBalcony(): Boolean = balcony

    override fun toString(): String =
        "Room{number=$number, type=$roomType, booked=$booked, seaView=$natureView, balcony=$balcony}"
}

