package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.repository.BusRepository
import javax.inject.Inject

class GetBusPositionsUseCase
    @Inject
    constructor(
        private val repository: BusRepository,
    ) {
        suspend operator fun invoke(): List<Bus> = repository.getPositions()
    }
