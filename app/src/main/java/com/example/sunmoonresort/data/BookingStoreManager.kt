package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.BookingDetails

/**
 * Central router for all booking persistence operations.
 *
 * Delegates every call to either [BookingLocalStore] or [BookingFirebaseStore]
 * based on the current value of [BookingStoreConfig.useFirebaseStore].
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Drop-in replacement for [BookingLocalStore] call sites
 * ──────────────────────────────────────────────────────────────────────────────
 * Replace `BookingLocalStore.saveBookings(...)` with `BookingStoreManager.saveBookings(...)`.
 * No other call-site changes are needed.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Data flow
 * ──────────────────────────────────────────────────────────────────────────────
 *  useFirebaseStore = false  →  SharedPreferences (sync, same as before)
 *  useFirebaseStore = true   →  Firestore         (async save; async load via [loadBookingsAsync])
 */
object BookingStoreManager {

    private val activeStore: BookingStore
        get() = if (BookingStoreConfig.useFirebaseStore) BookingFirebaseStore else BookingLocalStore

    // ──────────────────────────────────────────────────────────────────────────
    // Save
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Persist the entire bookings map using the currently active store.
     *
     * - LocalStore : synchronous write to SharedPreferences.
     * - FirebaseStore : asynchronous fire-and-forget write to Firestore.
     */
    fun saveBookings(bookings: Map<Int, List<BookingDetails>>) {
        activeStore.saveBookings(bookings)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Load (sync)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Synchronously load bookings.
     *
     * - LocalStore   : returns persisted bookings immediately.
     * - FirebaseStore: returns an empty map (Firestore is async — use [loadBookingsAsync]).
     */
    fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>> = activeStore.loadBookings()

    // ──────────────────────────────────────────────────────────────────────────
    // Load (async)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Asynchronously load bookings and deliver the result via [onComplete] on the main thread.
     *
     * - LocalStore: delivers the sync result immediately inside [onComplete].
     * - FirebaseStore: fetches from Firestore and invokes [onComplete] when ready.
     *
     * [onComplete] is always guaranteed to be called (even on failure, with an empty map).
     */
    fun loadBookingsAsync(onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit) {
        activeStore.loadBookingsAsync(onComplete)
    }
}

