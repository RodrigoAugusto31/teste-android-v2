package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.repository.BusRepository
import javax.inject.Inject

class GetBusPositionsByLineUseCase @Inject constructor(
    private val repository: BusRepository
) {
    suspend operator fun invoke(lineCode: Int): List<Bus> {
        return repository.getPositionsByLine(lineCode)
    }
}
