package com.example.sunmoonresort.model

data class Room(
    val number: Int,
    val roomType: RoomType,
    val booked: Boolean = false,
    val natureView: Boolean = false,
    val balcony: Boolean = false,
) {

    override fun toString(): String =
        "Room{number=$number, type=$roomType, booked=$booked, seaView=$natureView, balcony=$balcony}"
}

