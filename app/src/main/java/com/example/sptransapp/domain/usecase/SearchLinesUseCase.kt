package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.repository.BusRepository
import javax.inject.Inject

class SearchLinesUseCase
    @Inject
    constructor(
        private val repository: BusRepository,
    ) {
        suspend operator fun invoke(query: String): List<Line> = repository.searchLines(query)
    }
