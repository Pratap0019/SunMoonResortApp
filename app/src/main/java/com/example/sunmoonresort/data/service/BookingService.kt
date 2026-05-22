package com.example.sunmoonresort.data.service

import com.example.sunmoonresort.data.HotelData
import com.example.sunmoonresort.data.SunMoonResort
import com.example.sunmoonresort.model.Bill
import com.example.sunmoonresort.model.BookingDetails
import com.example.sunmoonresort.model.BookingRecord
import com.example.sunmoonresort.model.BookingStatus
import com.example.sunmoonresort.model.Extras
import com.example.sunmoonresort.model.Guest
import com.example.sunmoonresort.model.Room
import com.example.sunmoonresort.model.RoomType
import java.util.UUID
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit

object BookingService {

    /**
     * Calculate availability by room type for a date range.
     */
    @Suppress("NewApi")
    fun getRoomTypeAvailability(checkIn: LocalDate?, checkOut: LocalDate?): Map<RoomType, Long> {
        val availability = mutableMapOf<RoomType, Long>()

        for (roomType in RoomType.entries) {
            availability[roomType] = 0L
        }

        HotelData.rooms
            .filter { room ->
                checkIn == null || checkOut == null || isRoomAvailableForRange(room.number, checkIn, checkOut)
            }
            .groupingBy { it.roomType }
            .eachCount()
            .forEach { (roomType, count) ->
                availability[roomType] = count.toLong()
            }

        return availability
    }

    /**
     * Check if a room is available for the given date range.
     */
    @Suppress("NewApi")
    fun isRoomAvailableForRange(roomNumber: Int, checkIn: LocalDate, checkOut: LocalDate): Boolean {
        val existingBookings = HotelData.bookings[roomNumber] ?: emptyList()
        return existingBookings
            .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.CHECKED_IN }
            .none { hasOverlap(it, checkIn, checkOut) }
    }

    /**
     * Check for date overlap between booking and requested range.
     *  Uses string-based date comparison to avoid API level issues.
     */
    private fun hasOverlap(booking: BookingDetails, requestedCheckIn: LocalDate, requestedCheckOut: LocalDate): Boolean {
        try {
            val existingCheckIn = LocalDate.parse(booking.checkInDate)
            val existingCheckOut = LocalDate.parse(booking.checkOutDate)
            return requestedCheckIn.isBefore(existingCheckOut) && requestedCheckOut.isAfter(existingCheckIn)
        } catch (e: Exception) {
            // Fallback to string comparison if LocalDate parsing fails
            return booking.checkInDate < requestedCheckOut.toString() &&
                    booking.checkOutDate > requestedCheckIn.toString()
        }
    }

    /**
     * Get first available room of a given type for a date range.
     */
    @Suppress("NewApi")
    fun getFirstAvailableRoom(roomType: RoomType, checkIn: LocalDate, checkOut: LocalDate): Room? {
        return HotelData.rooms
            .filter { it.roomType == roomType }
            .firstOrNull { isRoomAvailableForRange(it.number, checkIn, checkOut) }
    }

    /**
     * Calculate bill and return bill details.
     */
    fun calculateBill(
        roomNumber: Int,
        daysStayed: Int,
        selectedExtras: List<Extras>?,
        petWeight: Double?,
        spaSessions: Int?
    ): Bill {
        return SunMoonResort.calculateBill(roomNumber, daysStayed, selectedExtras, petWeight, spaSessions)
    }

    /**
     * Confirm and store a booking.
     */
    fun confirmBooking(
        roomNumber: Int,
        guestName: String,
        contactNumber: String,
        daysStayed: Int,
        checkIn: String,
        checkOut: String,
        bill: Bill
    ): String {
        val guest = Guest(guestName, contactNumber)
        val bookingId = UUID.randomUUID().toString()
        val bookingDetails = BookingDetails(
            bookingId = bookingId,
            guest = guest,
            bill = bill,
            daysStayed = daysStayed,
            checkInDate = checkIn,
            checkOutDate = checkOut,
            status = BookingStatus.CONFIRMED
        )
        HotelData.bookings.computeIfAbsent(roomNumber) { mutableListOf() }.add(bookingDetails)
        com.example.sunmoonresort.data.BookingLocalStore.saveBookings(HotelData.bookings)
        return bookingId
    }

    /**
     * Search bookings by mobile number.
     */
    fun searchBookingsByMobile(mobileNumber: String): List<BookingRecord> {
        val normalizedInput = normalizeToTenDigits(mobileNumber)
        return buildAllBookingRecords()
            .filter { normalizeToTenDigits(it.contactNumber) == normalizedInput }
            .sortedByDescending { it.checkInDate }
    }

    /**
     * Cancel a booking by ID if it's associated with the given mobile number.
     */
    fun cancelBooking(bookingId: String, mobileNumber: String): Result {
        val normalizedInput = normalizeToTenDigits(mobileNumber)

        val targetBooking = findBookingById(bookingId)
        if (targetBooking == null) {
            return Result.Error("Booking not found.")
        }

        val bookingMobile = normalizeToTenDigits(targetBooking.guest.contactNumber)
        if (bookingMobile != normalizedInput) {
            return Result.Error("You can cancel only bookings linked to the searched mobile number.")
        }

        if (targetBooking.status != BookingStatus.CONFIRMED) {
            return Result.Error("Only confirmed bookings can be cancelled.")
        }

        targetBooking.status = BookingStatus.CANCELLED
        com.example.sunmoonresort.data.BookingLocalStore.saveBookings(HotelData.bookings)
        return Result.Success("Booking $bookingId has been cancelled.")
    }

    /**
     * Get all bookings as records for listing.
     */
    fun getAllBookingRecords(): List<BookingRecord> {
        return buildAllBookingRecords()
    }

    /**
     * Admin: Update booking status with state machine rules.
     */
    fun updateBookingStatus(bookingId: String, targetStatus: BookingStatus): Boolean {
        HotelData.bookings.values.forEach { bookings ->
            bookings.firstOrNull { it.bookingId == bookingId }?.let { booking ->
                val canTransition = when (targetStatus) {
                    BookingStatus.CANCELLED -> booking.status == BookingStatus.CONFIRMED
                    BookingStatus.CHECKED_IN -> booking.status == BookingStatus.CONFIRMED
                    BookingStatus.CHECKED_OUT -> booking.status == BookingStatus.CHECKED_IN
                    BookingStatus.CONFIRMED -> false
                }
                if (canTransition) {
                    booking.status = targetStatus
                    com.example.sunmoonresort.data.BookingLocalStore.saveBookings(HotelData.bookings)
                    return@forEach
                }
            }
        }
        return true
    }

    /**
     * Build room booking ranges for admin view.
     */
    fun getRoomBookingRanges(): Map<Int, String> {
        val ranges = mutableMapOf<Int, String>()
        HotelData.rooms.forEach { room ->
            val bookings = HotelData.bookings[room.number] ?: emptyList()
            val rangeText = bookings
                .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.CHECKED_IN }
                .map { "${it.checkInDate} to ${it.checkOutDate}" }
                .joinToString(" | ")
            ranges[room.number] = if (rangeText.isBlank()) "-" else rangeText
        }
        return ranges
    }

    /**
     * Build homepage room type summary.
     */
    @Suppress("NewApi")
    fun buildRoomTypeSummary(): Map<RoomType, Map<String, Any>> {
        val summary = mutableMapOf<RoomType, Map<String, Any>>()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        for (rt in RoomType.entries) {
            val available = HotelData.rooms
                .filter { it.roomType == rt }
                .filter { isRoomAvailableForRange(it.number, today, tomorrow) }
                .count()
            val total = HotelData.rooms.filter { it.roomType == rt }.count()
            val sample = HotelData.rooms.firstOrNull { it.roomType == rt }

            val info = mutableMapOf<String, Any>(
                "available" to available,
                "total" to total,
                "hasBalcony" to (sample?.balcony ?: false),
                "hasNatureView" to (sample?.natureView ?: false),
                "hasWifi" to true, // Free for all
                "hasMinifridge" to (rt == RoomType.DOUBLE || rt == RoomType.SUITE), // Free for DOUBLE/SUITE only
                "rate" to HotelData.getRoomRate(rt)
            )
            summary[rt] = info
        }
        return summary
    }

    // Private helpers
    private fun buildAllBookingRecords(): List<BookingRecord> {
        val bookingRecords = mutableListOf<BookingRecord>()
        HotelData.bookings.forEach { (roomNum, bookingDetailsList) ->
            val roomType = HotelData.findRoom(roomNum)?.roomType?.name ?: "UNKNOWN"
            bookingDetailsList.forEach { bookingDetails ->
                val guest = bookingDetails.guest
                val bill = bookingDetails.bill
                bookingRecords.add(
                    BookingRecord(
                        bookingId = bookingDetails.bookingId,
                        roomNumber = roomNum,
                        guestName = guest.name,
                        contactNumber = guest.contactNumber,
                        roomType = roomType,
                        bill = bill,
                        daysStayed = bookingDetails.daysStayed,
                        checkInDate = bookingDetails.checkInDate,
                        checkOutDate = bookingDetails.checkOutDate,
                        status = bookingDetails.status
                    )
                )
            }
        }
        return bookingRecords
    }

    private fun findBookingById(bookingId: String): BookingDetails? {
        HotelData.bookings.values.forEach { bookings ->
            bookings.firstOrNull { it.bookingId == bookingId }?.let {
                return it
            }
        }
        return null
    }

    private fun normalizeToTenDigits(rawMobile: String?): String {
        if (rawMobile == null) return ""
        val digits = rawMobile.replace(Regex("\\D"), "")
        return if (digits.length > 10) {
            digits.substring(digits.length - 10)
        } else {
            digits
        }
    }

    sealed class Result {
        data class Success(val message: String) : Result()
        data class Error(val message: String) : Result()
    }
}
