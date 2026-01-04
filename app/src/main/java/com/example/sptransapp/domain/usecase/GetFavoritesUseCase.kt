package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.repository.BusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase
    @Inject
    constructor(
        private val repository: BusRepository,
    ) {
        operator fun invoke(): Flow<List<Line>> = repository.getFavoriteLines()
    }
