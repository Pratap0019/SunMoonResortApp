package com.example.sunmoonresort

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sunmoonresort.ui.HomeActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch HomeActivity and finish MainActivity
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}