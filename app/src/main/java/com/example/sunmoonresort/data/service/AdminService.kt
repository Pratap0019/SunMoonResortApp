package com.example.sunmoonresort.data.service

import com.example.sunmoonresort.model.BookingStatus

object AdminService {

    private const val ADMIN_PASSWORD = "Bhanu@0001"
    private var isAdminLoggedIn = false

    /**
     * Verify admin credentials.
     */
    fun verifyAdmin(password: String): Boolean {
        if (password == ADMIN_PASSWORD) {
            isAdminLoggedIn = true
            return true
        }
        return false
    }

    /**
     * Check if admin is currently logged in.
     */
    fun isLoggedIn(): Boolean = isAdminLoggedIn

    /**
     * Logout admin.
     */
    fun logout() {
        isAdminLoggedIn = false
    }

    /**
     * Cancel a confirmed booking (admin only).
     */
    fun cancelBooking(bookingId: String): Boolean {
        return BookingService.updateBookingStatus(bookingId, BookingStatus.CANCELLED)
    }

    /**
     * Check in a confirmed booking.
     */
    fun checkInBooking(bookingId: String): Boolean {
        return BookingService.updateBookingStatus(bookingId, BookingStatus.CHECKED_IN)
    }

    /**
     * Check out a booking that is already checked in.
     */
    fun checkOutBooking(bookingId: String): Boolean {
        return BookingService.updateBookingStatus(bookingId, BookingStatus.CHECKED_OUT)
    }
}

