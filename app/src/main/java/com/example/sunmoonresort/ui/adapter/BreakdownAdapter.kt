package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemBreakdownLineBinding

data class BreakdownItem(
    val itemName: String,
    val calculation: String,
    val amount: Double
)

class BreakdownAdapter(
    private val items: List<BreakdownItem>
) : RecyclerView.Adapter<BreakdownAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemBreakdownLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BreakdownItem) {
            binding.itemName.text = item.itemName
            binding.itemCalculation.text = item.calculation
            binding.itemAmount.text = "₹ ${String.format("%.2f", item.amount)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBreakdownLineBinding.inflate(
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

