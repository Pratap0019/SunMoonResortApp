package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.BookingDetails

/**
 * Common contract for all booking persistence strategies.
 *
 * - [saveBookings]      : Persist the entire bookings map (room → list of bookings).
 * - [loadBookings]      : Synchronously return persisted bookings (used on cold-start with LocalStore).
 * - [loadBookingsAsync] : Asynchronously fetch bookings and deliver them via [onComplete]
 *                         (primary path when [BookingStoreConfig.useFirebaseStore] is true).
 */
interface BookingStore {

    /**
     * Persist all bookings. Implementations may be synchronous (Local) or async/fire-and-forget (Firebase).
     */
    fun saveBookings(bookings: Map<Int, List<BookingDetails>>)

    /**
     * Synchronously load all bookings.
     * Firebase implementation returns an empty map — use [loadBookingsAsync] instead.
     */
    fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>>

    /**
     * Asynchronously load bookings and deliver the result to [onComplete] on the main thread.
     * Local implementation calls [onComplete] immediately with the sync result.
     */
    fun loadBookingsAsync(onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit)
}

