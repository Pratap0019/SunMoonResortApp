package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemRoomAvailabilityBinding
import com.example.sunmoonresort.model.RoomType
import com.google.android.material.chip.Chip

class RoomAvailabilityAdapter(
    private val items: List<Pair<RoomType, Map<String, Any>>>
) : RecyclerView.Adapter<RoomAvailabilityAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRoomAvailabilityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<RoomType, Map<String, Any>>) {
            val (roomType, details) = item

            binding.roomTypeName.text = roomType.name
            binding.roomRate.text = "₹ ${details["rate"]}"

            // Balcony
            setupChip(
                binding.balconyBadge,
                if (details["hasBalcony"] as Boolean) "Yes" else "No",
                details["hasBalcony"] as Boolean
            )

            // Nature View
            setupChip(
                binding.natureViewBadge,
                if (details["hasNatureView"] as Boolean) "Yes" else "No",
                details["hasNatureView"] as Boolean
            )

            // WiFi (free for all)
            setupChip(
                binding.wifiBadge,
                if (details["hasWifi"] as Boolean) "Included" else "No",
                details["hasWifi"] as Boolean
            )

            // Mini-Fridge
            setupChip(
                binding.minifridgeBadge,
                if (details["hasMinifridge"] as Boolean) "Included" else "Not Available",
                details["hasMinifridge"] as Boolean
            )

            // Availability
            val available = (details["available"] as? Number)?.toInt() ?: 0
            val total = (details["total"] as? Number)?.toInt() ?: 0
            setupChip(
                binding.availableBadge,
                if (available > 0) "$available / $total available" else "Fully Booked",
                available > 0
            )
        }

        private fun setupChip(chip: Chip, text: String, isPositive: Boolean) {
            chip.text = text
            chip.setChipBackgroundColorResource(
                if (isPositive) android.R.color.holo_green_light
                else android.R.color.darker_gray
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoomAvailabilityBinding.inflate(
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
