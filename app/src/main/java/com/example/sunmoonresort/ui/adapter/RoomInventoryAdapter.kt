package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemRoomInventoryBinding
import com.example.sunmoonresort.model.Room

data class RoomInventoryItem(
    val room: Room,
    val bookedDates: String
)

class RoomInventoryAdapter(
    private val items: List<RoomInventoryItem>
) : RecyclerView.Adapter<RoomInventoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRoomInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoomInventoryItem) {
            binding.roomNumber.text = item.room.number.toString()
            binding.roomType.text = item.room.roomType.name
            binding.bookedDates.text = item.bookedDates

            // Status chip
            val isBooked = item.bookedDates != "-"
            binding.statusChip.text = if (isBooked) "Booked" else "Available"
            binding.statusChip.setChipBackgroundColorResource(
                if (isBooked) android.R.color.darker_gray
                else com.google.android.material.R.color.design_default_color_primary
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoomInventoryBinding.inflate(
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

