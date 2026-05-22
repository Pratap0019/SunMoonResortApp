package com.example.sunmoonresort.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sunmoonresort.R
import com.example.sunmoonresort.data.HotelData
import com.example.sunmoonresort.data.service.AdminService
import com.example.sunmoonresort.data.service.BookingService
import com.example.sunmoonresort.databinding.ActivityAdminBookingsNewBinding
import com.example.sunmoonresort.databinding.DialogBillDetailsBinding
import com.example.sunmoonresort.model.BookingRecord
import com.example.sunmoonresort.ui.adapter.AdminBookingsAdapter
import com.example.sunmoonresort.ui.adapter.BreakdownAdapter
import com.example.sunmoonresort.ui.adapter.BreakdownItem
import com.example.sunmoonresort.ui.adapter.RoomInventoryAdapter
import com.example.sunmoonresort.ui.adapter.RoomInventoryItem
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBookingsNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if admin is logged in
        if (!AdminService.isLoggedIn()) {
            // Redirect to login page
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityAdminBookingsNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    /**
     * Admin logout.
     */
    private fun adminLogout() {
        AdminService.logout()
        val intent = Intent(this, AdminLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Load and display all bookings.
     */
    fun loadBookings() {
        try {
            val bookings = BookingService.getAllBookingRecords()
            val roomRanges = BookingService.getRoomBookingRanges()
            displayBookingsList(bookings, roomRanges)
        } catch (e: Exception) {
            showError("Error loading bookings: ${e.message}")
        }
    }

    /**
     * Cancel a booking (admin action).
     */
    private fun cancelBooking(bookingId: String) {
        try {
            if (AdminService.cancelBooking(bookingId)) {
                showSuccess("Booking $bookingId cancelled successfully.")
                loadBookings() // Refresh list
            } else {
                showError("Unable to cancel booking. Check booking status.")
            }
        } catch (e: Exception) {
            showError("Error canceling booking: ${e.message}")
        }
    }

    /**
     * Check in a booking (admin action).
     */
    private fun checkInBooking(bookingId: String) {
        try {
            if (AdminService.checkInBooking(bookingId)) {
                showSuccess("Booking $bookingId checked in successfully.")
                loadBookings() // Refresh list
            } else {
                showError("Unable to check in booking. Check booking status.")
            }
        } catch (e: Exception) {
            showError("Error checking in booking: ${e.message}")
        }
    }

    /**
     * Check out a booking (admin action).
     */
    private fun checkOutBooking(bookingId: String) {
        try {
            if (AdminService.checkOutBooking(bookingId)) {
                showSuccess("Booking $bookingId checked out successfully.")
                loadBookings() // Refresh list
            } else {
                showError("Unable to check out booking. Check booking status.")
            }
        } catch (e: Exception) {
            showError("Error checking out booking: ${e.message}")
        }
    }

    // UI Display Methods
    private fun setupUI() {
        binding.bookingsList.layoutManager = LinearLayoutManager(this)
        binding.roomInventoryList.layoutManager = LinearLayoutManager(this)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.newBookingBtn.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }

        binding.logoutBtn.setOnClickListener {
            adminLogout()
        }

        loadBookings()
    }

    private fun displayBookingsList(bookings: List<BookingRecord>, roomRanges: Map<Int, String>) {
        val sortedBookings = bookings.sortedByDescending { it.checkInDate }
        val hasBookings = sortedBookings.isNotEmpty()

        binding.emptyStateCard.visibility = if (hasBookings) android.view.View.GONE else android.view.View.VISIBLE
        binding.bookingsList.visibility = if (hasBookings) android.view.View.VISIBLE else android.view.View.GONE

        binding.bookingsList.adapter = AdminBookingsAdapter(
            items = sortedBookings,
            onCheckIn = { checkInBooking(it.bookingId) },
            onCheckOut = { checkOutBooking(it.bookingId) },
            onCancel = { cancelBooking(it.bookingId) },
            onViewBill = { showBillDetails(it) }
        )

        val roomItems = HotelData.rooms
            .sortedBy { it.number }
            .map { room ->
                RoomInventoryItem(
                    room = room,
                    bookedDates = roomRanges[room.number] ?: "-"
                )
            }

        binding.roomInventoryList.adapter = RoomInventoryAdapter(roomItems)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showBillDetails(record: BookingRecord) {
        val dialogBinding = DialogBillDetailsBinding.inflate(layoutInflater)

        dialogBinding.billingDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        dialogBinding.guestName.text = record.guestName
        dialogBinding.contactNumber.text = record.contactNumber
        dialogBinding.roomNumber.text = record.roomNumber.toString()
        dialogBinding.roomType.text = record.roomType
        dialogBinding.durationDays.text = getString(R.string.duration_days_format, record.daysStayed)
        dialogBinding.checkInDate.text = record.checkInDate.ifBlank { getString(R.string.na) }
        dialogBinding.checkOutDate.text = record.checkOutDate.ifBlank { getString(R.string.na) }
        dialogBinding.totalAmount.text = String.format(Locale.getDefault(), "Rs %.2f", record.bill.totalAmount)

        val rows = record.bill.breakdown.map { (itemName, amount) ->
            BreakdownItem(
                itemName = itemName,
                calculation = record.bill.calculationDetails[itemName] ?: "",
                amount = amount
            )
        }
        dialogBinding.breakdownItems.layoutManager = LinearLayoutManager(this)
        dialogBinding.breakdownItems.adapter = BreakdownAdapter(rows)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.bill_details))
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

