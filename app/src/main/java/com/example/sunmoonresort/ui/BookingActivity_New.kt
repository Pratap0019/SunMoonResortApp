package com.example.sunmoonresort.ui

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sunmoonresort.data.service.BookingService
import com.example.sunmoonresort.databinding.ActivityBookingNewBinding
import com.example.sunmoonresort.model.Extras
import com.example.sunmoonresort.model.RoomType
import com.example.sunmoonresort.ui.adapter.BreakdownAdapter
import com.example.sunmoonresort.ui.adapter.BreakdownItem
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingNewBinding
    private var selectedCheckIn: LocalDate? = null
    private var selectedCheckOut: LocalDate? = null
    private var availabilityChecked = false
    private var currentBill: com.example.sunmoonresort.model.Bill? = null
    private var assignedRoomNumber: Int? = null

    private val selectedExtras = mutableSetOf<Extras>()
    private var spaSessionsSelected = 1
    private var selectedPetWeightKg: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()
        setupDatePickers()
        setupExtrasSelection()
        setupSpaSessions()
        setupPetWeightSelection()
        setupButtons()
    }

    private fun initializeUI() {
        binding.backBtn.setOnClickListener {
            navigateBackOrHome()
        }

        binding.contactNumber.filters = arrayOf(InputFilter.LengthFilter(10))
    }

    private fun navigateBackOrHome() {
        if (isTaskRoot) {
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        onBackPressedDispatcher.onBackPressed()
    }

    private fun setupDatePickers() {
        binding.checkInDate.inputType = InputType.TYPE_NULL
        binding.checkOutDate.inputType = InputType.TYPE_NULL

        binding.checkInDate.setOnClickListener {
            showDatePickerDialog(minDate = LocalDate.now()) { date ->
                selectedCheckIn = date
                binding.checkInDate.setText(date.toString())

                if (selectedCheckOut != null && !selectedCheckOut!!.isAfter(date)) {
                    selectedCheckOut = null
                    binding.checkOutDate.setText("")
                    binding.daysStayed.setText("")
                }

                resetRoomTypeSelectionForDateChange()
                validateDateRange()
            }
        }

        binding.checkOutDate.setOnClickListener {
            if (selectedCheckIn == null) {
                showError("Please select check-in date first")
                return@setOnClickListener
            }

            showDatePickerDialog(selectedCheckIn!!.plusDays(1)) { date ->
                selectedCheckOut = date
                binding.checkOutDate.setText(date.toString())
                resetRoomTypeSelectionForDateChange()
                validateDateRange()
            }
        }
    }

    private fun resetRoomTypeSelectionForDateChange() {
        availabilityChecked = false
        currentBill = null
        assignedRoomNumber = null
        binding.roomTypeSpinner.setText("", false)
        binding.roomTypeSpinner.isEnabled = false
        binding.calculatePriceBtn.isEnabled = false
        binding.priceBreakdownCard.visibility = View.GONE
    }

    private fun showDatePickerDialog(minDate: LocalDate? = null, onDateSelected: (LocalDate) -> Unit) {
        val todayMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val minMs = (minDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()) ?: todayMs
        val constraints = CalendarConstraints.Builder()
            .setStart(todayMs)
            .setValidator(DateValidatorPointForward.from(minMs))
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(minMs)
            .setTitleText("Select Date")
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            onDateSelected(selectedDate)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun validateDateRange() {
        binding.checkInError.visibility = View.GONE
        binding.checkOutError.visibility = View.GONE

        if (selectedCheckIn != null && selectedCheckOut != null) {
            if (!selectedCheckOut!!.isAfter(selectedCheckIn!!)) {
                binding.checkOutError.visibility = View.VISIBLE
                binding.checkOutDate.setText("")
                selectedCheckOut = null
                binding.daysStayed.setText("")
                return
            }

            val days = ChronoUnit.DAYS.between(selectedCheckIn, selectedCheckOut).toInt()
            binding.daysStayed.setText(days.toString())
            binding.calculatePriceBtn.isEnabled = true
            binding.checkAvailabilityBtn.isEnabled = true
        }
    }

    private fun setupExtrasSelection() {
        // Extras are independent checkboxes, so each option can be selected/unselected.
        binding.extraMattressCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedExtras.add(Extras.MATTRESS) else selectedExtras.remove(Extras.MATTRESS)
        }

        binding.extraGymCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedExtras.add(Extras.GymPASS) else selectedExtras.remove(Extras.GymPASS)
        }

        binding.extraPoolCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedExtras.add(Extras.PoolPASS) else selectedExtras.remove(Extras.PoolPASS)
        }

        binding.extraSpaCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedExtras.add(Extras.SPA)
                binding.spaSessionsContainer.isEnabled = true
                binding.spaSessionsContainer.alpha = 1f
                binding.spaSessionsSpinner.isEnabled = true
                if (binding.spaSessionsSpinner.text.isNullOrBlank()) {
                    binding.spaSessionsSpinner.setText("1 session", false)
                }
            } else {
                selectedExtras.remove(Extras.SPA)
                binding.spaSessionsSpinner.setText("", false)
                binding.spaSessionsSpinner.isEnabled = false
                binding.spaSessionsContainer.isEnabled = false
                binding.spaSessionsContainer.alpha = 0.85f
                spaSessionsSelected = 1
            }
        }
    }

    private fun setupSpaSessions() {
        val sessionOptions = listOf("1 session", "2 sessions", "3 sessions")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sessionOptions)
        binding.spaSessionsSpinner.setAdapter(adapter)
        binding.spaSessionsSpinner.setText("1 session", false)
        binding.spaSessionsSpinner.setOnItemClickListener { _, _, position, _ ->
            spaSessionsSelected = position + 1
        }
    }

    private fun setupPetWeightSelection() {
        val petOptions = listOf("Small", "Medium", "Large")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, petOptions)
        binding.petWeightSpinner.setAdapter(adapter)
        binding.petWeightSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedPetWeightKg = when (position) {
                0 -> 5.0
                1 -> 10.0
                2 -> 20.0
                else -> null
            }
        }
    }

    private fun setupButtons() {
        binding.checkAvailabilityBtn.setOnClickListener {
            checkAvailability()
        }

        binding.calculatePriceBtn.setOnClickListener {
            calculatePrice()
        }

        binding.confirmBookingBtn.setOnClickListener {
            confirmBooking()
        }
    }

    private fun checkAvailability() {
        if (selectedCheckIn == null || selectedCheckOut == null) {
            showError("Please select both check-in and check-out dates")
            return
        }

        availabilityChecked = true
        binding.roomTypeSpinner.isEnabled = true

        val availability = BookingService.getRoomTypeAvailability(selectedCheckIn, selectedCheckOut)
        val roomOptions = mutableListOf<String>()

        for ((roomType, count) in availability) {
            if (count > 0) {
                roomOptions.add("${roomType.name} - $count room${if (count > 1) "s" else ""} available")
            }
        }

        if (roomOptions.isEmpty()) {
            showError("No rooms available for selected dates")
            availabilityChecked = false
            binding.roomTypeSpinner.setText("", false)
            binding.roomTypeSpinner.isEnabled = false
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, roomOptions)
        binding.roomTypeSpinner.setAdapter(adapter)
        // Preselect first available option after date availability check.
        binding.roomTypeSpinner.setText(roomOptions.first(), false)
        binding.calculatePriceBtn.isEnabled = true

        showSuccess("Availability checked! Select a room type.")
    }

    private fun calculatePrice() {
        if (!availabilityChecked || selectedCheckIn == null || selectedCheckOut == null) {
            showError("Please check availability first")
            return
        }

        val selectedRoomText = binding.roomTypeSpinner.text.toString()
        if (selectedRoomText.isEmpty()) {
            showError("Please select a room type")
            return
        }

        val roomTypeName = selectedRoomText.split(" ")[0]
        val roomType = RoomType.valueOf(roomTypeName)

        val daysStayed = binding.daysStayed.text.toString().toIntOrNull() ?: 0
        val petWeight = selectedPetWeightKg

        try {
            val room = BookingService.getFirstAvailableRoom(roomType, selectedCheckIn!!, selectedCheckOut!!)
            if (room == null) {
                showError("No available rooms of type $roomTypeName for selected dates")
                return
            }

            assignedRoomNumber = room.number

            val bill = BookingService.calculateBill(
                room.number,
                daysStayed,
                selectedExtras.toList(),
                petWeight,
                if (Extras.SPA in selectedExtras) spaSessionsSelected else null
            )

            currentBill = bill
            displayBillBreakdown(bill, room.number, roomType)
        } catch (e: Exception) {
            showError("Error calculating price: ${e.message}")
        }
    }

    private fun displayBillBreakdown(bill: com.example.sunmoonresort.model.Bill, roomNumber: Int, roomType: RoomType) {
        binding.priceBreakdownCard.visibility = View.VISIBLE
        binding.assignedRoomNumber.text = roomNumber.toString()
        binding.assignedRoomType.text = roomType.name

        val breakdownItems = bill.breakdown.map { (itemName, amount) ->
            BreakdownItem(
                itemName = itemName,
                calculation = bill.calculationDetails[itemName] ?: "",
                amount = amount
            )
        }

        val adapter = BreakdownAdapter(breakdownItems)
        binding.breakdownList.layoutManager = LinearLayoutManager(this)
        binding.breakdownList.adapter = adapter

        binding.confirmBookingBtn.isVisible = true
    }

    private fun confirmBooking() {
        val guestName = binding.guestName.text.toString().trim()
        val contactNumber = binding.contactNumber.text.toString().trim()

        binding.nameError.visibility = View.GONE
        binding.mobileError.visibility = View.GONE

        var valid = true

        if (guestName.isEmpty() || !guestName.matches(Regex("[A-Za-z ]+"))) {
            binding.nameError.visibility = View.VISIBLE
            valid = false
        }

        if (contactNumber.length != 10 || !contactNumber.matches(Regex("[6-9][0-9]{9}"))) {
            binding.mobileError.visibility = View.VISIBLE
            valid = false
        }

        if (!valid) return

        val roomNumber = assignedRoomNumber
        val bill = currentBill
        if (roomNumber == null || bill == null || selectedCheckIn == null || selectedCheckOut == null) {
            showError("Please calculate price again before confirming booking")
            return
        }

        binding.confirmBookingBtn.isEnabled = false
        BookingService.confirmBooking(
            roomNumber = roomNumber,
            guestName = guestName,
            contactNumber = "+91-$contactNumber",
            daysStayed = binding.daysStayed.text.toString().toInt(),
            checkIn = selectedCheckIn.toString(),
            checkOut = selectedCheckOut.toString(),
            bill = bill,
        ) { result ->
            binding.confirmBookingBtn.isEnabled = true
            when (result) {
                is BookingService.ConfirmBookingResult.Success -> {
                    binding.priceBreakdownCard.visibility = View.GONE
                    binding.calculatePriceArea.visibility = View.GONE
                    binding.successCard.visibility = View.VISIBLE
                    binding.successMessage.text = buildString {
                        append("Booking Confirmed!\n\n")
                        append("Booking ID: ${result.bookingId}\n")
                        append("Room: ${binding.assignedRoomNumber.text}\n")
                        append("Guest: $guestName\n")
                        append("Total: ₹${String.format("%.2f", bill.totalAmount)}")
                    }
                }

                BookingService.ConfirmBookingResult.RoomAlreadyBooked -> {
                    val reassigned = tryAutoReassignRoomAfterConflict()
                    if (!reassigned) {
                        refreshAvailabilityAfterConflict()
                    }
                }

                is BookingService.ConfirmBookingResult.Error -> {
                    showError("Error confirming booking: ${result.message}")
                }
            }
        }
    }

    private fun tryAutoReassignRoomAfterConflict(): Boolean {
        val checkIn = selectedCheckIn ?: return false
        val checkOut = selectedCheckOut ?: return false

        val selectedRoomText = binding.roomTypeSpinner.text?.toString().orEmpty()
        if (selectedRoomText.isBlank()) return false

        val roomTypeName = selectedRoomText.split(" ").firstOrNull().orEmpty()
        val roomType = try {
            RoomType.valueOf(roomTypeName)
        } catch (_: IllegalArgumentException) {
            return false
        }

        val previousRoomNumber = assignedRoomNumber
        val nextRoom = BookingService.getFirstAvailableRoom(roomType, checkIn, checkOut) ?: return false
        if (previousRoomNumber != null && nextRoom.number == previousRoomNumber) return false

        val daysStayed = binding.daysStayed.text.toString().toIntOrNull() ?: return false
        val spaSessions = if (Extras.SPA in selectedExtras) spaSessionsSelected else null
        val newBill = BookingService.calculateBill(
            roomNumber = nextRoom.number,
            daysStayed = daysStayed,
            selectedExtras = selectedExtras.toList(),
            petWeight = selectedPetWeightKg,
            spaSessions = spaSessions,
        )

        assignedRoomNumber = nextRoom.number
        currentBill = newBill
        displayBillBreakdown(newBill, nextRoom.number, roomType)
        showError("The previously selected room was just booked. We switched you to room ${nextRoom.number}. Please confirm again.")
        return true
    }

    private fun refreshAvailabilityAfterConflict() {
        val checkIn = selectedCheckIn
        val checkOut = selectedCheckOut
        if (checkIn == null || checkOut == null) {
            resetRoomTypeSelectionForDateChange()
            showError("This room was just booked by someone else. Please select dates and check availability again.")
            return
        }

        val availability = BookingService.getRoomTypeAvailability(checkIn, checkOut)
        val roomOptions = mutableListOf<String>()
        for ((roomType, count) in availability) {
            if (count > 0) {
                roomOptions.add("${roomType.name} - $count room${if (count > 1) "s" else ""} available")
            }
        }

        currentBill = null
        assignedRoomNumber = null
        binding.priceBreakdownCard.visibility = View.GONE
        binding.confirmBookingBtn.isVisible = false

        if (roomOptions.isEmpty()) {
            availabilityChecked = false
            binding.roomTypeSpinner.setText("", false)
            binding.roomTypeSpinner.isEnabled = false
            binding.calculatePriceBtn.isEnabled = false
            showError("No rooms are currently available for selected dates. Please change dates and try again.")
            return
        }

        availabilityChecked = true
        binding.roomTypeSpinner.isEnabled = true
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, roomOptions)
        binding.roomTypeSpinner.setAdapter(adapter)
        binding.roomTypeSpinner.setText(roomOptions.first(), false)
        binding.calculatePriceBtn.isEnabled = true
        showError("Selected room was just booked. We refreshed the latest options. Please calculate price again.")
    }

    private fun showError(message: String) {
        binding.errorAlert.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    private fun showSuccess(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }
}

