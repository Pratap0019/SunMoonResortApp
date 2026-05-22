package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemBookingResultBinding
import com.example.sunmoonresort.model.BookingRecord
import com.example.sunmoonresort.model.BookingStatus
import com.google.android.material.chip.Chip

class BookingResultAdapter(
    private val items: List<BookingRecord>,
    private val onCancelClick: (BookingRecord) -> Unit
) : RecyclerView.Adapter<BookingResultAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemBookingResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookingRecord) {
            binding.bookingId.text = item.bookingId
            binding.roomNumber.text = item.roomNumber.toString()
            binding.roomType.text = item.roomType
            binding.checkInDate.text = item.checkInDate
            binding.checkOutDate.text = item.checkOutDate
            binding.totalAmount.text = "₹ ${String.format("%.2f", item.bill.totalAmount)}"

            // Status Badge
            setupStatusChip(binding.bookingStatus, item.status)

            // Cancel button visibility
            if (item.status == BookingStatus.CONFIRMED) {
                binding.cancelBookingBtn.visibility = ViewGroup.VISIBLE
                binding.actionPlaceholder.visibility = ViewGroup.GONE
                binding.cancelBookingBtn.setOnClickListener {
                    onCancelClick(item)
                }
            } else {
                binding.cancelBookingBtn.visibility = ViewGroup.GONE
                binding.actionPlaceholder.visibility = ViewGroup.VISIBLE
            }
        }

        private fun setupStatusChip(chip: Chip, status: BookingStatus) {
            chip.text = status.name.replace("_", " ").replaceFirstChar { it.uppercase() }
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
        val binding = ItemBookingResultBinding.inflate(
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

