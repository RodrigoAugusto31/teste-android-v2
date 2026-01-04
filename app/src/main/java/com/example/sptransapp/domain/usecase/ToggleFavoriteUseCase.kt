package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.repository.BusRepository
import javax.inject.Inject

class ToggleFavoriteUseCase
    @Inject
    constructor(
        private val repository: BusRepository,
    ) {
        suspend operator fun invoke(line: Line) {
            repository.toggleFavorite(line)
        }
    }
