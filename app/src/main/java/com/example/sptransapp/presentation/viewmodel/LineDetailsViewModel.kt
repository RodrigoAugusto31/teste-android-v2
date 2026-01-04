package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.usecase.CheckIfFavoriteUseCase
import com.example.sptransapp.domain.usecase.GetBusPositionsByLineUseCase
import com.example.sptransapp.domain.usecase.GetMapLayerUseCase
import com.example.sptransapp.domain.usecase.GetStopPredictionsUseCase
import com.example.sptransapp.domain.usecase.GetStopsUseCase
import com.example.sptransapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class LineDetailsViewModel @Inject constructor(
    private val getBusPositionsByLineUseCase: GetBusPositionsByLineUseCase,
    private val getStopPredictionsUseCase: GetStopPredictionsUseCase,
    private val getStopsUseCase: GetStopsUseCase,
    private val getMapLayerUseCase: GetMapLayerUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val checkIfFavoriteUseCase: CheckIfFavoriteUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _stopList = MutableLiveData<List<Stop>>()
    val stopList: LiveData<List<Stop>> = _stopList

    private val _predictions = MutableLiveData<List<Prediction>>()
    val predictions: LiveData<List<Prediction>> = _predictions

    private val _kmlData = MutableLiveData<InputStream?>()
    val kmlData: LiveData<InputStream?> = _kmlData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    var shouldMoveCamera = true

    private var refreshJob: Job? = null

    private val _currentLineCode = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val busList: StateFlow<List<Bus>> = _currentLineCode
        .filterNotNull()
        .flatMapLatest { code ->
            flow {
                while (true) {
                    try {
                        val result = getBusPositionsByLineUseCase(code)
                        emit(result)
                    } catch (_: Exception) {
                        emit(emptyList())
                    }
                    delay(15_000)
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun startMonitoring(lineCode: Int) {
        if (_currentLineCode.value == lineCode) return

        _currentLineCode.value = lineCode
        shouldMoveCamera = true

        loadStops(lineCode)
        checkFavoriteStatus(lineCode)
    }

    private fun loadStops(lineCode: Int) {
        viewModelScope.launch {
            getStopsUseCase(lineCode)
                .collect { stops ->
                    _stopList.value = stops
                }
        }
    }

    fun fetchStopPredictions(stopCode: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    getStopPredictionsUseCase(stopCode)
                }
                _predictions.value = result
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.prediction_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMapLayer(layerId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream = withContext(Dispatchers.IO) {
                    getMapLayerUseCase(layerId)
                }
                _kmlData.value = inputStream
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.map_download_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPredictions() {
        _predictions.value = emptyList()
    }

    fun checkFavoriteStatus(lineCode: Int) {
        viewModelScope.launch {
            checkIfFavoriteUseCase(lineCode).collect { isFav ->
                _isFavorite.value = isFav
            }
        }
    }

    fun toggleFavorite(line: Line) {
        viewModelScope.launch {
            toggleFavoriteUseCase(line)
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
