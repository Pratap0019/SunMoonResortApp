package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.BookingDetails
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.reflect.TypeToken

/**
 * Firebase Firestore implementation of [BookingStore].
 *
 * Data is stored as a single JSON string (same GSON format as [BookingLocalStore]) inside:
 *
 *   Collection : hotel_bookings
 *   Document   : bookings_data
 *   Field      : bookings_json  →  Map<Int, List<BookingDetails>> serialised with GSON
 */
object BookingFirebaseStore : BookingStore {

    private const val COLLECTION = "hotel_bookings"
    private const val DOCUMENT = "bookings_data"
    private const val FIELD_JSON = "bookings_json"

    private val gson = GsonFactory.instance
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    override fun saveBookings(bookings: Map<Int, List<BookingDetails>>) {
        val json = gson.toJson(bookings)
        firestore
            .collection(COLLECTION)
            .document(DOCUMENT)
            .set(mapOf(FIELD_JSON to json))
            .addOnFailureListener { e ->
                android.util.Log.e("BookingFirebaseStore", "Failed to save bookings to Firestore", e)
            }
    }

    override fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>> = mutableMapOf()

    override fun loadBookingsAsync(
        onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit
    ) {
        firestore
            .collection(COLLECTION)
            .document(DOCUMENT)
            .get()
            .addOnSuccessListener { document ->
                val json = document.getString(FIELD_JSON)
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
