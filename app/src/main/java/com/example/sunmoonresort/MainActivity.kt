package com.example.sunmoonresort

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.sunmoonresort.data.BookingStoreConfig
import com.example.sunmoonresort.databinding.ActivityMainBinding
import com.example.sunmoonresort.ui.HomeActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Safety timeout: navigate to HomeActivity even if Firebase never responds (5 seconds).
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { navigateToHome() }
    private val TIMEOUT_MS = 5_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BookingStoreConfig.storeDataInFirebase) {
            // Local store: data is already ready — skip the loading screen entirely.
            navigateToHome()
            return
        }

        // Firebase store: show branded loading screen while Firestore responds.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start safety timeout so the app never gets stuck.
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        // Navigate to HomeActivity as soon as data is ready.
        SunMoonResortApp.onDataReady {
            timeoutHandler.removeCallbacks(timeoutRunnable)
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }
}