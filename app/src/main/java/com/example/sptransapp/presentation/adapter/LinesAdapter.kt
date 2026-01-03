package com.example.sptransapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sptransapp.databinding.ItemLineBinding
import com.example.sptransapp.domain.model.Line

class LinesAdapter(
    private val onLinhaClick: (Line) -> Unit
) : RecyclerView.Adapter<LinesAdapter.LineViewHolder>() {

    private val lines = mutableListOf<Line>()

    fun updateList(newList: List<Line>) {
        lines.clear()
        lines.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val binding = ItemLineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        holder.bind(lines[position])
    }

    override fun getItemCount(): Int = lines.size

    inner class LineViewHolder(private val binding: ItemLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(line: Line) {
            binding.textviewItemSign.text = line.fullSign
            binding.textviewItemName.text = line.name

            binding.root.setOnClickListener {
                onLinhaClick(line)
            }
        }
    }
}
