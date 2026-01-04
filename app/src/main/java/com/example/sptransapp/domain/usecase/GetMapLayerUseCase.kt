package com.example.sptransapp.domain.usecase

import com.example.sptransapp.domain.repository.BusRepository
import java.io.InputStream
import javax.inject.Inject

class GetMapLayerUseCase @Inject constructor(
    private val repository: BusRepository
) {
    suspend operator fun invoke(layerId: Int): InputStream? {
        return when (layerId) {
            0 -> repository.getCorridorsKml()
            1 -> repository.getOtherLanesKml()
            2 -> repository.getGeneralKml()
            else -> null
        }
    }
}
