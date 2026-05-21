package com.example.sunmoonresort.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sunmoonresort.R
import com.example.sunmoonresort.data.service.BookingService
import com.example.sunmoonresort.databinding.ActivityHomeNewBinding
import com.example.sunmoonresort.ui.adapter.BookingResultAdapter
import com.example.sunmoonresort.ui.adapter.CarouselAdapter
import com.example.sunmoonresort.ui.adapter.CarouselSlide
import com.example.sunmoonresort.ui.adapter.PriceRowAdapter
import com.example.sunmoonresort.ui.adapter.RoomAvailabilityAdapter
import com.google.android.material.tabs.TabLayoutMediator

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()
        setupCarousel()
        setupRoomAvailability()
        setupPricing()
        setupSearchBooking()
        setupCTAButtons()
    }

    private fun initializeUI() {
        // Admin button navigation - go to login first
        binding.adminBtn.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }

        // Main CTA button
        binding.bookStayCta.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }
    }

    private fun setupCarousel() {
        val slides = listOf(
            CarouselSlide(
                imageResId = R.drawable.carousal1,
                title = "Escape to nature's serenity",
                description = "Where comfort meets royalty with blooming nature",
                onCtaClick = { startActivity(Intent(this, BookingActivity::class.java)) }
            ),
            CarouselSlide(
                imageResId = R.drawable.carousal2,
                title = "Luxurious Rooms",
                description = "Relax in our elegantly designed rooms with sea view options",
                ctaText = "View Rooms",
                onCtaClick = { startActivity(Intent(this, BookingActivity::class.java)) }
            ),
            CarouselSlide(
                imageResId = R.drawable.carousal3,
                title = "Refreshing Balconies & Amenities",
                description = "Enjoy our outdoor view, luxury stays and premium facilities",
                ctaText = "Explore Amenities",
                onCtaClick = { startActivity(Intent(this, BookingActivity::class.java)) }
            )
        )

        val carouselAdapter = CarouselAdapter(slides)
        binding.carouselViewpager.adapter = carouselAdapter

        TabLayoutMediator(binding.carouselIndicator, binding.carouselViewpager) { _, _ -> }
            .attach()
    }

    private fun setupRoomAvailability() {
        val roomTypeSummary = BookingService.buildRoomTypeSummary()
        val items = roomTypeSummary.toList()

        val adapter = RoomAvailabilityAdapter(items)
        binding.roomAvailabilityList.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = adapter
        }
    }

    private fun setupPricing() {
        // Room Rates
        val roomRates = listOf(
            "SINGLE" to 2000.0,
            "DOUBLE" to 3500.0,
            "SUITE" to 5000.0
        )
        binding.roomRatesList.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = PriceRowAdapter(roomRates)
        }

        // Extras
        val extras = listOf(
            "Mattress" to 500.0,
            "SPA" to 1500.0,
            "Gym Pass" to 500.0,
            "Pool Pass" to 500.0
        )
        binding.extrasList.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = PriceRowAdapter(extras)
        }

        // Pet Fees
        val petFees = listOf(
            "Small (below 8kg)" to 200.0,
            "Medium (below 15kg)" to 350.0,
            "Large (above 15kg)" to 500.0
        )
        binding.petFeesList.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = PriceRowAdapter(petFees)
        }
    }

    private fun setupSearchBooking() {
        // Input filter for mobile number (10 digits)
        binding.mobileSearchInput.filters = arrayOf(
            InputFilter.LengthFilter(10)
        )

        binding.searchBookingBtn.setOnClickListener {
            val mobileNumber = binding.mobileSearchInput.text.toString().trim()

            if (mobileNumber.isBlank()) {
                showError("Please enter a mobile number")
                return@setOnClickListener
            }

            if (mobileNumber.length != 10) {
                showError("Mobile number must be 10 digits")
                return@setOnClickListener
            }

            searchBookings(mobileNumber)
        }
    }

    private fun searchBookings(mobileNumber: String) {
        try {
            val bookings = BookingService.searchBookingsByMobile(mobileNumber)

            if (bookings.isEmpty()) {
                showError("No bookings found for +91-$mobileNumber")
                binding.searchResultsContainer.visibility = View.GONE
                return
            }

            // Display results
            binding.searchResultsContainer.visibility = View.VISIBLE
            val adapter = BookingResultAdapter(bookings) { booking ->
                cancelBooking(booking.bookingId, mobileNumber)
            }

            binding.searchResultsList.let {
                it.layoutManager = LinearLayoutManager(this)
                it.adapter = adapter
            }

            showSuccess("Found ${bookings.size} booking(s)")
        } catch (e: Exception) {
            showError("Error searching bookings: ${e.message}")
        }
    }

    private fun cancelBooking(bookingId: String, mobileNumber: String) {
        try {
            val result = BookingService.cancelBooking(bookingId, mobileNumber)

            when (result) {
                is BookingService.Result.Success -> {
                    showSuccess(result.message)
                    // Refresh search results
                    searchBookings(mobileNumber)
                }
                is BookingService.Result.Error -> {
                    showError(result.message)
                }
            }
        } catch (e: Exception) {
            showError("Error canceling booking: ${e.message}")
        }
    }

    private fun setupCTAButtons() {
        binding.bookStayCta.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }
    }

    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).setTextColor(android.graphics.Color.RED).show()
    }

    private fun showSuccess(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }
}

