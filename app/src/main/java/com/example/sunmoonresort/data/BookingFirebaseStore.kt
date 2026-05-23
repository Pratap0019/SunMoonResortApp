package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.BookingDetails
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken

/**
 * Firebase Firestore implementation of [BookingStore].
 *
 * Data is stored as a single JSON string (same GSON format as [BookingLocalStore]) inside:
 *
 *   Collection : hotel_bookings
 *   Document   : bookings_data
 *   Field      : bookings_json  →  Map<Int, List<BookingDetails>> serialised with GSON
 *
 * This mirrors the SharedPreferences-based approach so migration is straightforward.
 *
 * NOTE: [loadBookings] always returns an empty map because Firestore is asynchronous.
 *       Use [loadBookingsAsync] for the initial data hydration.
 */
object BookingFirebaseStore : BookingStore {

    private const val COLLECTION = "hotel_bookings"
    private const val DOCUMENT   = "bookings_data"
    private const val FIELD_JSON = "bookings_json"

    private val gson = GsonFactory.instance

    // ──────────────────────────────────────────────────────────────────────────
    // Save
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Serialise the bookings map to JSON and persist it in Firestore (fire-and-forget).
     * Errors are logged but do not crash the app.
     */
    override fun saveBookings(bookings: Map<Int, List<BookingDetails>>) {
        val json = gson.toJson(bookings)
        Firebase.firestore
            .collection(COLLECTION)
            .document(DOCUMENT)
            .set(mapOf(FIELD_JSON to json))
            .addOnFailureListener { e ->
                // Log failure — in production replace with proper logging/analytics
                android.util.Log.e("BookingFirebaseStore", "Failed to save bookings to Firestore", e)
            }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Load (sync — not supported for Firestore)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Synchronous load is not available for Firestore.
     * Returns an empty map; use [loadBookingsAsync] instead.
     */
    override fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>> = mutableMapOf()

    // ──────────────────────────────────────────────────────────────────────────
    // Load (async)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetch the latest bookings from Firestore and deliver them via [onComplete].
     * [onComplete] is always called (with an empty map on failure), ensuring the app
     * never hangs waiting for a result.
     */
    override fun loadBookingsAsync(
        onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit
    ) {
        Firebase.firestore
            .collection(COLLECTION)
            .document(DOCUMENT)
            .get()
            .addOnSuccessListener { document ->
                val json = document?.getString(FIELD_JSON)
                val result: MutableMap<Int, MutableList<BookingDetails>> = if (!json.isNullOrBlank()) {
                    try {
                        val type = object : TypeToken<MutableMap<Int, MutableList<BookingDetails>>>() {}.type
                        gson.fromJson(json, type) ?: mutableMapOf()
                    } catch (e: Exception) {
                        android.util.Log.e("BookingFirebaseStore", "Failed to parse Firestore bookings JSON", e)
                        mutableMapOf()
                    }
                } else {
                    mutableMapOf()
                }
                onComplete(result)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("BookingFirebaseStore", "Failed to load bookings from Firestore", e)
                onComplete(mutableMapOf())
            }
    }
}

