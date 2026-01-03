package com.example.sptransapp.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sptransapp.R
import com.example.sptransapp.databinding.BottomSheetLineSelectionBinding
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.presentation.adapter.LineSelectionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LineSelectionBottomSheet(
    private val lines: List<Line>,
    private val onLineSelected: (Line) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLineSelectionBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.Theme_App_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_App_BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLineSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LineSelectionAdapter(lines) { line ->
            onLineSelected(line)
            dismiss()
        }

        binding.rvLinhasSelection.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LineSelectionBottomSheet"
    }
}
