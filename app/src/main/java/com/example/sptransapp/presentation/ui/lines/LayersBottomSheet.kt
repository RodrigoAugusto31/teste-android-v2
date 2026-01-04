package com.example.sptransapp.presentation.ui.lines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sptransapp.R
import com.example.sptransapp.presentation.adapter.LayerOption
import com.example.sptransapp.presentation.adapter.LayersAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LayersBottomSheet(
    private val currentSelectedId: Int?,
    private val onLayerSelected: (Int?) -> Unit,
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val recyclerView = RecyclerView(requireContext())
        recyclerView.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        recyclerView.setPadding(0, 32, 0, 32)
        return recyclerView
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val options =
            listOf(
                LayerOption(
                    0,
                    getString(R.string.corridor_title_label),
                    R.drawable.ic_corridor,
                    currentSelectedId == 0,
                ),
                LayerOption(
                    1,
                    getString(R.string.other_roads_title_label),
                    R.drawable.ic_route,
                    currentSelectedId == 1,
                ),
                LayerOption(
                    2,
                    getString(R.string.general_traffic_title_label),
                    R.drawable.ic_streets,
                    currentSelectedId == 2,
                ),
            )

        val adapter =
            LayersAdapter(options) { item ->
                if (item.isSelected) {
                    onLayerSelected(item.id)
                } else {
                    onLayerSelected(null)
                }
                dismiss()
            }

        if (view is RecyclerView) {
            view.layoutManager = LinearLayoutManager(requireContext())
            view.adapter = adapter
        }
    }

    companion object {
        const val TAG = "LayersBottomSheet"
    }
}
