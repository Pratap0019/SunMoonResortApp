package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.R
import com.example.sunmoonresort.databinding.ItemRoomInventoryBinding
import com.example.sunmoonresort.model.Room

data class RoomInventoryItem(
    val room: Room,
    val bookedDatesList: List<String>
)

class RoomInventoryAdapter(
    private val items: List<RoomInventoryItem>
) : RecyclerView.Adapter<RoomInventoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRoomInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoomInventoryItem) {
            val context = binding.root.context

            binding.roomNumber.text = item.room.number.toString()
            binding.roomType.text = item.room.roomType.name

            val isBooked = item.bookedDatesList.isNotEmpty()
            binding.statusChip.text = context.getString(
                if (isBooked) R.string.no else R.string.yes
            )
            binding.statusChip.setChipBackgroundColorResource(
                if (isBooked) R.color.badge_booked
                else R.color.badge_available
            )
            binding.statusChip.setTextColor(ContextCompat.getColor(context, R.color.text_dark))

            // Dynamically populate booked date rows
            binding.bookedDatesContainer.removeAllViews()
            if (item.bookedDatesList.isEmpty()) {
                val tv = TextView(context)
                tv.text = "-"
                tv.textSize = 10f
                tv.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                tv.includeFontPadding = false
                binding.bookedDatesContainer.addView(tv)
            } else {
                item.bookedDatesList.forEach { dateRange ->
                    val tv = TextView(context)
                    tv.text = dateRange
                    tv.textSize = 10f
                    tv.setTextColor(ContextCompat.getColor(context, R.color.text_dark))
                    tv.includeFontPadding = false
                    val lp = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = 2
                    tv.layoutParams = lp
                    binding.bookedDatesContainer.addView(tv)
                }
            }
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
