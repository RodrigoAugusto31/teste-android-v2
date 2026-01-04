package com.example.sptransapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sptransapp.databinding.ItemLineBinding
import com.example.sptransapp.domain.model.Line

class LineSelectionAdapter(
    private val lines: List<Line>,
    private val onLineClick: (Line) -> Unit,
) : RecyclerView.Adapter<LineSelectionAdapter.ViewHolder>() {
    inner class ViewHolder(
        private val binding: ItemLineBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: Line) {
            binding.textviewItemSign.text = line.fullSign
            binding.textviewItemName.text = line.name

            binding.root.setOnClickListener { onLineClick(line) }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = ItemLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(lines[position])
    }

    override fun getItemCount() = lines.size
}
