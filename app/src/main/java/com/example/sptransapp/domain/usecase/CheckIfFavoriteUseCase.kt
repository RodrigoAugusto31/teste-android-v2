package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.repository.BusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckIfFavoriteUseCase
    @Inject
    constructor(
        private val repository: BusRepository,
    ) {
        operator fun invoke(lineCode: Int): Flow<Boolean> = repository.isFavorite(lineCode)
    }
