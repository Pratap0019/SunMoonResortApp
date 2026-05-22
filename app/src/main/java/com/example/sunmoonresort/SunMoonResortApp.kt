package com.example.sunmoonresort

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class SunMoonResortApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Required for org.threeten.bp on Android: loads timezone database assets.
        AndroidThreeTen.init(this)
    }
}

