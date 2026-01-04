package com.example.sptransapp.presentation.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sptransapp.databinding.BottomSheetPredictionBinding
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.presentation.adapter.PredictionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class
PredictionBottomSheet(
    private val stopName: String,
    private val predictions: List<Prediction>,
    private val onRouteClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPredictionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPredictionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textviewStopName.text = stopName
        binding.recyclerviewPredictions.adapter = PredictionAdapter(predictions)

        binding.btnRoute.setOnClickListener {
            onRouteClick()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PredictionBottomSheet"
    }
}
