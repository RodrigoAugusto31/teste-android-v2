package com.example.sptransapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sptransapp.databinding.ItemPredictionBinding
import com.example.sptransapp.domain.model.Prediction

class PredictionAdapter(
    private val predictions: List<Prediction>,
) : RecyclerView.Adapter<PredictionAdapter.ViewHolder>() {
    inner class ViewHolder(
        private val binding: ItemPredictionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Prediction) {
            binding.textviewPrevisionLine.text = item.line
            binding.textviewPrevisionDestination.text = item.destination
            binding.textviewPrevisionTime.text = item.arrivalTime
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            ItemPredictionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(predictions[position])
    }

    override fun getItemCount() = predictions.size
}
