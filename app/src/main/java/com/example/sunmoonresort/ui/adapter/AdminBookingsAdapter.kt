package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemAdminBookingBinding
import com.example.sunmoonresort.model.BookingRecord
import com.example.sunmoonresort.model.BookingStatus

class AdminBookingsAdapter(
    private val items: List<BookingRecord>,
    private val onCheckIn: (BookingRecord) -> Unit,
    private val onCheckOut: (BookingRecord) -> Unit,
    private val onCancel: (BookingRecord) -> Unit,
    private val onViewBill: (BookingRecord) -> Unit
) : RecyclerView.Adapter<AdminBookingsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAdminBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookingRecord) {
            binding.roomNumber.text = item.roomNumber.toString()
            binding.roomType.text = item.roomType
            binding.guestName.text = item.guestName
            binding.checkInDate.text = item.checkInDate
            binding.checkOutDate.text = item.checkOutDate
            binding.totalAmount.text = "₹ ${String.format("%.2f", item.bill.totalAmount)}"

            // Status badge
            setupStatusBadge(binding.statusBadge, item.status)

            // Button visibility based on status
            binding.checkInBtn.visibility = if (item.status == BookingStatus.CONFIRMED) View.VISIBLE else View.GONE
            binding.checkOutBtn.visibility = if (item.status == BookingStatus.CHECKED_IN) View.VISIBLE else View.GONE
            binding.cancelBookingBtn.visibility = if (item.status == BookingStatus.CONFIRMED) View.VISIBLE else View.GONE
            binding.viewBillBtn.visibility = if (item.status == BookingStatus.CHECKED_OUT) View.VISIBLE else View.GONE

            // Button listeners
            binding.checkInBtn.setOnClickListener { onCheckIn(item) }
            binding.checkOutBtn.setOnClickListener { onCheckOut(item) }
            binding.cancelBookingBtn.setOnClickListener { onCancel(item) }
            binding.viewBillBtn.setOnClickListener { onViewBill(item) }
        }

        private fun setupStatusBadge(chip: com.google.android.material.chip.Chip, status: BookingStatus) {
            chip.text = when (status) {
                BookingStatus.CONFIRMED -> "Confirmed"
                BookingStatus.CHECKED_IN -> "Serving"
                BookingStatus.CHECKED_OUT -> "Checked Out"
                BookingStatus.CANCELLED -> "Cancelled"
            }

            chip.setChipBackgroundColorResource(
                when (status) {
                    BookingStatus.CONFIRMED -> com.google.android.material.R.color.design_default_color_primary
                    BookingStatus.CHECKED_IN -> android.R.color.holo_orange_light
                    BookingStatus.CHECKED_OUT -> android.R.color.holo_green_light
                    BookingStatus.CANCELLED -> android.R.color.holo_red_light
                }
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}

