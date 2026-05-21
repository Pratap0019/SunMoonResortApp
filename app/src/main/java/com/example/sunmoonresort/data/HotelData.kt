package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.BookingDetails
import com.example.sunmoonresort.model.Extras
import com.example.sunmoonresort.model.Room
import com.example.sunmoonresort.model.RoomType

object HotelData {

    val rooms: List<Room> = listOf(
        Room(101, RoomType.SINGLE, false, false, false),
        Room(102, RoomType.DOUBLE, false, false, true),
        Room(103, RoomType.SUITE, false, true, true),
        Room(104, RoomType.SINGLE, false, false, false),
        Room(105, RoomType.DOUBLE, false, false, true),
        Room(106, RoomType.SUITE, false, true, true),
        Room(201, RoomType.SINGLE, false, false, false),
        Room(202, RoomType.DOUBLE, false, false, true),
        Room(203, RoomType.SUITE, false, true, true),
        Room(204, RoomType.SINGLE, false, false, false),
        Room(205, RoomType.DOUBLE, false, false, true),
        Room(206, RoomType.SUITE, false, true, true),
        Room(301, RoomType.SINGLE, false, false, false),
        Room(302, RoomType.DOUBLE, false, false, true),
        Room(303, RoomType.SUITE, false, true, true),
        Room(304, RoomType.SINGLE, false, false, false),
        Room(305, RoomType.DOUBLE, false, false, true),
        Room(306, RoomType.SUITE, false, true, true),
    )

    val roomRates: Map<RoomType, Double> = mapOf(
        RoomType.SINGLE to 2000.0,
        RoomType.DOUBLE to 3500.0,
        RoomType.SUITE to 5000.0,
    )

    // WIFI and MINIFRIDGE are free, so they are intentionally excluded.
    val extrasRate: Map<Extras, Double> = mapOf(
        Extras.MATTRESS to 500.0,
        Extras.SPA to 1500.0,
        Extras.GymPASS to 500.0,
        Extras.PoolPASS to 500.0,
    )

    val petFeeRates: Map<String, Double> = mapOf(
        "Small (below 8kg)" to 200.0,
        "Medium (below 15kg)" to 350.0,
        "Large (above 15kg)" to 500.0,
    )

    // Stores confirmed bookings: room number -> booking list.
    val bookings: MutableMap<Int, MutableList<BookingDetails>> = mutableMapOf()

    fun findRoom(roomNumber: Int): Room? = rooms.firstOrNull { it.number == roomNumber }

    fun getRoomRate(roomType: RoomType): Double = roomRates[roomType] ?: 0.0

    fun getExtraRate(extra: Extras): Double = extrasRate[extra] ?: 0.0

    fun getPetFee(petCategory: String?): Double =
        petCategory?.let { petFeeRates[it] } ?: 0.0

    fun getBookingsForRoom(roomNumber: Int): List<BookingDetails> =
        bookings[roomNumber].orEmpty()
}

