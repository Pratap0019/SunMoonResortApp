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

    // Safety timeout: navigate to HomeActivity even if remote backend never responds (5 seconds).
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { navigateToHome() }
    private val TIMEOUT_MS = 5_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BookingStoreConfig.isRemoteStoreEnabled) {
            // Local store: data is ready immediately.
            navigateToHome()
            return
        }

        // Remote store (Firebase/Supabase): show loading screen while async hydration finishes.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

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