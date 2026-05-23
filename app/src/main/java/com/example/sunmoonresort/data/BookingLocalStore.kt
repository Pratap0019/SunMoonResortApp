package com.example.sunmoonresort.data

import android.content.Context
import com.example.sunmoonresort.model.BookingDetails
import com.google.gson.reflect.TypeToken

object BookingLocalStore : BookingStore {
    private const val PREFS_NAME = "sunmoon_resort_prefs"
    private const val KEY_BOOKINGS = "bookings"

    private val gson = GsonFactory.instance
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    override fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>> {
        val context = appContext ?: return mutableMapOf()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BOOKINGS, null) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<Int, MutableList<BookingDetails>>>() {}.type
            gson.fromJson<MutableMap<Int, MutableList<BookingDetails>>>(json, type) ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    override fun saveBookings(bookings: Map<Int, List<BookingDetails>>) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(bookings)
        prefs.edit().putString(KEY_BOOKINGS, json).apply()
    }

    /** LocalStore is synchronous — [onComplete] is called immediately with the loaded result. */
    override fun loadBookingsAsync(
        onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit
    ) {
        onComplete(loadBookings())
    }
}


