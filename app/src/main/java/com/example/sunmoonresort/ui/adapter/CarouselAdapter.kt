package com.example.sunmoonresort.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunmoonresort.databinding.ItemCarouselSlideBinding

data class CarouselSlide(
    val imageResId: Int,
    val title: String,
    val description: String,
    val ctaText: String = "Book Now",
    val onCtaClick: () -> Unit = {}
)

class CarouselAdapter(
    private val slides: List<CarouselSlide>
) : RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCarouselSlideBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: CarouselSlide) {
            binding.carouselImage.setImageResource(slide.imageResId)
            binding.carouselTitle.text = slide.title
            binding.carouselDescription.text = slide.description
            binding.carouselCta.text = slide.ctaText
            binding.carouselCta.setOnClickListener {
                slide.onCtaClick()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarouselSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount() = slides.size
}

