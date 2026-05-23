package com.example.sunmoonresort

import android.app.Application
import com.example.sunmoonresort.data.BookingFirebaseStore
import com.example.sunmoonresort.data.BookingLocalStore
import com.example.sunmoonresort.data.BookingStoreConfig
import com.example.sunmoonresort.data.HotelData
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen

class SunMoonResortApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
        FirebaseApp.initializeApp(this)
        BookingLocalStore.init(this)

        if (BookingStoreConfig.storeDataInFirebase) {
            // Firebase path: load async — notify once data arrives.
            BookingFirebaseStore.loadBookingsAsync { firebaseBookings ->
                HotelData.replaceBookings(firebaseBookings)
                notifyDataReady()
            }
        } else {
            // Local path: load sync — data is ready immediately.
            HotelData.replaceBookings(BookingLocalStore.loadBookings())
            notifyDataReady()
        }
    }

    companion object {
        /** True once HotelData has been hydrated (from either store). */
        @Volatile var isDataReady: Boolean = false
            private set

        private val pendingCallbacks = mutableListOf<() -> Unit>()
        private val lock = Any()

        /**
         * Register [callback] to be invoked on the main thread when booking data is ready.
         * If data is already available, [callback] is called immediately (inline).
         */
        fun onDataReady(callback: () -> Unit) {
            synchronized(lock) {
                if (isDataReady) {
                    callback()          // already ready — call inline
                } else {
                    pendingCallbacks.add(callback)
                }
            }
        }

        /** Called internally once HotelData has been populated. */
        private fun notifyDataReady() {
            val toInvoke: List<() -> Unit>
            synchronized(lock) {
                isDataReady = true
                toInvoke = pendingCallbacks.toList()
                pendingCallbacks.clear()
            }
            // Firestore already delivers on the main thread; local path is also main thread.
            toInvoke.forEach { it() }
        }
    }
}
