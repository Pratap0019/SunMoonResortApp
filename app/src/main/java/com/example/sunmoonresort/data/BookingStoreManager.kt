package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.BookingDetails

/**
 * Central router for all booking persistence operations.
 */
object BookingStoreManager {

    private val activeStore: BookingStore
        get() = when (BookingStoreConfig.selectedBackend) {
            BookingStoreConfig.StorageBackend.LOCAL -> BookingLocalStore
            BookingStoreConfig.StorageBackend.FIREBASE -> BookingFirebaseStore
            BookingStoreConfig.StorageBackend.SUPABASE -> BookingSupabaseStore
        }

    fun saveBookings(bookings: Map<Int, List<BookingDetails>>) {
        activeStore.saveBookings(bookings)
    }

    fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>> = activeStore.loadBookings()

    fun loadBookingsAsync(onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit) {
        activeStore.loadBookingsAsync(onComplete)
    }
}
