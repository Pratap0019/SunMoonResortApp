package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemPriceRowBinding

class PriceRowAdapter(
    private val items: List<Pair<String, Double>>
) : RecyclerView.Adapter<PriceRowAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPriceRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<String, Double>) {
            binding.priceItemName.text = item.first
            binding.priceItemValue.text = "₹ ${item.second}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPriceRowBinding.inflate(
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

