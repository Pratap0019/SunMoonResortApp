package com.example.sunmoonresort.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sunmoonresort.R
import com.example.sunmoonresort.data.service.AdminService
import com.example.sunmoonresort.databinding.ActivityAdminLoginBinding

class AdminLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if already logged in
        if (AdminService.isLoggedIn()) {
            navigateToAdminPanel()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        // Login button click listener
        binding.loginButton.setOnClickListener {
            val password = binding.passwordInput.text.toString()
            performLogin(password)
        }

        // Back button click listener
        binding.backButton.setOnClickListener {
            finish()
        }

        // Clear error when user starts typing
        binding.passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.passwordInputLayout.error = null
            }
        }
    }

    private fun performLogin(password: String) {
        // Clear previous errors
        binding.passwordInputLayout.error = null

        // Validate input
        if (password.isBlank()) {
            binding.passwordInputLayout.error = getString(R.string.password_required)
            return
        }

        if (password.length < 6) {
            binding.passwordInputLayout.error = getString(R.string.password_too_short)
            return
        }

        // Show loading state
        binding.loginButton.isEnabled = false
        binding.loginButton.text = getString(R.string.logging_in)
        binding.progressIndicator.show()

        // Perform authentication
        if (AdminService.verifyAdmin(password)) {
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
            navigateToAdminPanel()
        } else {
            binding.passwordInputLayout.error = getString(R.string.invalid_password)
            binding.loginButton.isEnabled = true
            binding.loginButton.text = getString(R.string.login)
            binding.progressIndicator.hide()
            // Clear password for security
            binding.passwordInput.text?.clear()
        }
    }

    private fun navigateToAdminPanel() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

