package com.example.sunmoonresort.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
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
import com.example.sunmoonresort.model.BookingStatus
import com.example.sunmoonresort.ui.adapter.AdminBookingsAdapter
import com.example.sunmoonresort.ui.adapter.BreakdownAdapter
import com.example.sunmoonresort.ui.adapter.BreakdownItem
import com.example.sunmoonresort.ui.adapter.RoomInventoryAdapter
import com.example.sunmoonresort.ui.adapter.RoomInventoryItem
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
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
        binding.ongoingBookingsList.layoutManager = LinearLayoutManager(this)
        binding.completedBookingsList.layoutManager = LinearLayoutManager(this)
        binding.roomInventoryList.layoutManager = LinearLayoutManager(this)

        binding.backButton.setOnClickListener {
            navigateBackOrHome()
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
        val ongoingStatuses = setOf(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        val completedStatuses = setOf(BookingStatus.CHECKED_OUT, BookingStatus.CANCELLED)

        val ongoing = bookings.filter { it.status in ongoingStatuses }
            .sortedByDescending { it.checkInDate }
        val completed = bookings.filter { it.status in completedStatuses }
            .sortedByDescending { it.checkInDate }

        // Ongoing section
        binding.ongoingCount.text = getString(R.string.count_format, ongoing.size)
        if (ongoing.isEmpty()) {
            binding.ongoingEmpty.visibility = View.VISIBLE
            binding.ongoingBookingsList.visibility = View.GONE
        } else {
            binding.ongoingEmpty.visibility = View.GONE
            binding.ongoingBookingsList.visibility = View.VISIBLE
            binding.ongoingBookingsList.adapter = AdminBookingsAdapter(
                items = ongoing,
                onCheckIn = { checkInBooking(it.bookingId) },
                onCheckOut = { checkOutBooking(it.bookingId) },
                onCancel = { cancelBooking(it.bookingId) },
                onViewBill = { showBillDetails(it) }
            )
        }

        // Completed / Cancelled section
        binding.completedCount.text = getString(R.string.count_format, completed.size)
        if (completed.isEmpty()) {
            binding.completedEmpty.visibility = View.VISIBLE
            binding.completedBookingsList.visibility = View.GONE
        } else {
            binding.completedEmpty.visibility = View.GONE
            binding.completedBookingsList.visibility = View.VISIBLE
            binding.completedBookingsList.adapter = AdminBookingsAdapter(
                items = completed,
                onCheckIn = { checkInBooking(it.bookingId) },
                onCheckOut = { checkOutBooking(it.bookingId) },
                onCancel = { cancelBooking(it.bookingId) },
                onViewBill = { showBillDetails(it) }
            )
        }

        // Room inventory
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
            .setPositiveButton(R.string.download_bill, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (downloadBillPdf(dialogBinding.root, record.bookingId)) {
                showSuccess(getString(R.string.bill_pdf_saved))
            } else {
                showError(getString(R.string.bill_pdf_failed))
            }
        }
    }

    private fun downloadBillPdf(billView: View, bookingId: String): Boolean {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageMargin = 20

        val widthSpec = View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        billView.measure(widthSpec, heightSpec)
        billView.layout(0, 0, billView.measuredWidth, billView.measuredHeight)

        val scale = (pageWidth - (pageMargin * 2)).toFloat() / billView.measuredWidth.toFloat()
        val printableViewHeight = ((pageHeight - (pageMargin * 2)) / scale).toInt()
        var currentTop = 0
        var pageNumber = 1

        while (currentTop < billView.measuredHeight) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.translate(pageMargin.toFloat(), pageMargin.toFloat())
            canvas.scale(scale, scale)
            canvas.translate(0f, -currentTop.toFloat())
            billView.draw(canvas)
            pdfDocument.finishPage(page)

            currentTop += printableViewHeight
            pageNumber++
        }

        val fileName = "SunMoon_Bill_${bookingId}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        return try {
            createPdfOutputStream(fileName).use { output ->
                if (output == null) {
                    pdfDocument.close()
                    return false
                }
                pdfDocument.writeTo(output)
            }
            pdfDocument.close()
            true
        } catch (_: IOException) {
            pdfDocument.close()
            false
        }
    }

    private fun createPdfOutputStream(fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri: Uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            contentResolver.openOutputStream(uri)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
    }

    private fun navigateBackOrHome() {
        if (isTaskRoot) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        onBackPressedDispatcher.onBackPressed()
    }
}

