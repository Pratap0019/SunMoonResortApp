package com.example.sunmoonresort.data

/**
 * Local compile-time switch that controls which [BookingStore] the app uses.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * TO SWITCH STORES — edit the single flag below and rebuild the app:
 *
 *   storeDataInFirebase = true   →  Firestore  (BookingFirebaseStore)
 *   storeDataInFirebase = false  →  SharedPreferences (BookingLocalStore)
 * ─────────────────────────────────────────────────────────────────────────────
 */
object BookingStoreConfig {

    /**
     * Set to `true`  to persist bookings in Firebase Firestore.
     * Set to `false` to persist bookings locally via SharedPreferences.
     *
     * Rebuild the app after changing this value.
     */
    const val storeDataInFirebase: Boolean = false

    /** Convenience alias read by [BookingStoreManager]. */
    val useFirebaseStore: Boolean get() = storeDataInFirebase
}

