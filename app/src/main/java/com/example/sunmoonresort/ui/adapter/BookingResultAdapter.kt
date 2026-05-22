package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.R
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
            binding.guestName.text = item.guestName
            binding.roomNumber.text = item.roomNumber.toString()
            binding.roomType.text = item.roomType
            binding.checkInDate.text = item.checkInDate
            binding.checkOutDate.text = item.checkOutDate
            binding.totalAmount.text = binding.root.context.getString(
                R.string.currency_amount_inr,
                item.bill.totalAmount
            )

            // Status badge colors intentionally match the admin booking tiles.
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
                    BookingStatus.CONFIRMED -> R.color.status_confirmed
                    BookingStatus.CHECKED_IN -> R.color.status_staying
                    BookingStatus.CHECKED_OUT -> R.color.status_checked_out
                    BookingStatus.CANCELLED -> R.color.status_cancelled
                }
            )
            chip.setTextColor(ContextCompat.getColor(chip.context, R.color.text_dark))
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

