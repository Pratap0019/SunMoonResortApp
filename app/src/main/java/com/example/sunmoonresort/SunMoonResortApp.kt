package com.example.sunmoonresort

import android.app.Application
import com.example.sunmoonresort.data.BookingFirebaseStore
import com.example.sunmoonresort.data.BookingLocalStore
import com.example.sunmoonresort.data.BookingStoreConfig
import com.example.sunmoonresort.data.BookingSupabaseStore
import com.example.sunmoonresort.data.HotelData
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen

class SunMoonResortApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)
        BookingLocalStore.init(this)

        when (BookingStoreConfig.selectedBackend) {
            BookingStoreConfig.StorageBackend.LOCAL -> {
                HotelData.replaceBookings(BookingLocalStore.loadBookings())
                notifyDataReady()
            }

            BookingStoreConfig.StorageBackend.FIREBASE -> {
                FirebaseApp.initializeApp(this)
                BookingFirebaseStore.loadBookingsAsync { firebaseBookings ->
                    HotelData.replaceBookings(firebaseBookings)
                    notifyDataReady()
                }
            }

            BookingStoreConfig.StorageBackend.SUPABASE -> {
                BookingSupabaseStore.loadBookingsAsync { supabaseBookings ->
                    HotelData.replaceBookings(supabaseBookings)
                    notifyDataReady()
                }
            }
        }
    }

    companion object {
        /** True once HotelData has been hydrated (from the selected store). */
        @Volatile var isDataReady: Boolean = false
            private set

        private val pendingCallbacks = mutableListOf<() -> Unit>()
        private val lock = Any()

        fun onDataReady(callback: () -> Unit) {
            synchronized(lock) {
                if (isDataReady) {
                    callback()
                } else {
                    pendingCallbacks.add(callback)
                }
            }
        }

        private fun notifyDataReady() {
            val toInvoke: List<() -> Unit>
            synchronized(lock) {
                isDataReady = true
                toInvoke = pendingCallbacks.toList()
                pendingCallbacks.clear()
            }
            toInvoke.forEach { it() }
        }
    }
}
