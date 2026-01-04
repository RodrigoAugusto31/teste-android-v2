package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.repository.BusRepository
import javax.inject.Inject

class GetStopPredictionsUseCase @Inject constructor(
    private val repository: BusRepository
) {
    suspend operator fun invoke(stopCode: Int): List<Prediction> {
        return repository.getStopPredictions(stopCode)
    }
}
