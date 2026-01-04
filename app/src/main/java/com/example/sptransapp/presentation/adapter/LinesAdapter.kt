package com.example.sptransapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sptransapp.databinding.ItemLineBinding
import com.example.sptransapp.domain.model.Line

class LinesAdapter(
    private val onLinhaClick: (Line) -> Unit,
) : ListAdapter<Line, LinesAdapter.LineViewHolder>(LineDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LineViewHolder {
        val binding =
            ItemLineBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return LineViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LineViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    inner class LineViewHolder(
        private val binding: ItemLineBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: Line) {
            binding.textviewItemSign.text = line.fullSign
            binding.textviewItemName.text = line.name

            binding.root.setOnClickListener {
                onLinhaClick(line)
            }
        }
    }

    class LineDiffCallback : DiffUtil.ItemCallback<Line>() {
        override fun areItemsTheSame(
            oldItem: Line,
            newItem: Line,
        ): Boolean = oldItem.fullSign == newItem.fullSign

        override fun areContentsTheSame(
            oldItem: Line,
            newItem: Line,
        ): Boolean = oldItem == newItem
    }
}
