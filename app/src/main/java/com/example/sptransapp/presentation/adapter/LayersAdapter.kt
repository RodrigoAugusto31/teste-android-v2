package com.example.sptransapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sptransapp.databinding.ItemLayerBinding

data class LayerOption(
    val id: Int,
    val title: String,
    val iconRes: Int,
    var isSelected: Boolean = false,
)

class LayersAdapter(
    private val options: List<LayerOption>,
    private val onOptionClick: (LayerOption) -> Unit,
) : RecyclerView.Adapter<LayersAdapter.ViewHolder>() {
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    init {
        selectedItemPosition = options.indexOfFirst { it.isSelected }
    }

    inner class ViewHolder(
        private val binding: ItemLayerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LayerOption) {
            binding.textviewTitle.text = item.title
            binding.imageviewRouteIcon.setImageResource(item.iconRes)

            binding.checkboxSelected.isChecked = item.isSelected

            binding.root.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                handleSelection(currentPosition)
                onOptionClick(item)
            }

            binding.checkboxSelected.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    handleSelection(currentPosition)
                    onOptionClick(item)
                }
            }
        }
    }

    private fun handleSelection(newPosition: Int) {
        if (selectedItemPosition == newPosition) {
            options[newPosition].isSelected = false
            notifyItemChanged(newPosition)
            selectedItemPosition = RecyclerView.NO_POSITION
        } else {
            if (selectedItemPosition != RecyclerView.NO_POSITION) {
                options[selectedItemPosition].isSelected = false
                notifyItemChanged(selectedItemPosition)
            }

            selectedItemPosition = newPosition
            options[selectedItemPosition].isSelected = true
            notifyItemChanged(selectedItemPosition)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = ItemLayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size
}
