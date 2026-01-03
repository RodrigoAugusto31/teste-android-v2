package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Corridor
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.repository.BusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(
    private val repository: BusRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val context: Context
) : ViewModel() {

    private val _busList = MutableLiveData<List<Bus>>()
    val busList: LiveData<List<Bus>> = _busList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _stopList = MutableLiveData<List<Stop>>()
    val stopList: LiveData<List<Stop>> = _stopList

    private val _foundLines = MutableLiveData<List<Line>>()
    val foundLines: LiveData<List<Line>> = _foundLines

    private val _predictions = MutableLiveData<List<Prediction>>()
    val predictions: LiveData<List<Prediction>> = _predictions

    private val _corridors = MutableLiveData<List<Corridor>>()
    val corridors: LiveData<List<Corridor>> = _corridors

    private val _kmlData = MutableLiveData<java.io.InputStream?>()
    val kmlData: LiveData<java.io.InputStream?> = _kmlData

    private val _selectedLine = MutableLiveData<Line?>()
    val selectedLine: LiveData<Line?> = _selectedLine

    var deveMoverCamera = false

    private var refreshJob: Job? = null

    fun clearData() {
        _busList.value = emptyList()
        _stopList.value = emptyList()
        stopRefresh()
    }

    private fun startAutoRefresh(action: suspend () -> List<Bus>) {
        refreshJob?.cancel()

        refreshJob =
            viewModelScope.launch {
                _isLoading.value = true

                while (isActive) {
                    try {
                        val result =
                            withContext(ioDispatcher) {
                                action()
                            }
                        _busList.value = result
                        _errorMessage.value = null
                    } catch (e: Exception) {
                        _errorMessage.value =
                            context.getString(R.string.att_error_message, e.message)
                    } finally {
                        _isLoading.value = false
                    }

                    delay(15_000)
                }
            }
    }

    fun selectLine(line: Line) {
        _selectedLine.value = line
        deveMoverCamera = true

        clearSearchResults()
        loadBusesByLine(line.lineCode)
    }

    fun fetchBuses() {
        _stopList.value = emptyList()
        startAutoRefresh {
            repository.getPositions()
        }
    }

    fun searchLine(query: String) {
        if (query.isBlank()) return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result =
                    withContext(ioDispatcher) {
                        repository.searchLines(query)
                    }
                _foundLines.value = result
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.search_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBusesByLine(lineCode: Int) {
        loadStops(lineCode)
        startAutoRefresh {
            repository.getPositionsByLine(lineCode)
        }
    }

    private fun loadStops(lineCode: Int) {
        viewModelScope.launch {
            try {
                val result =
                    withContext(ioDispatcher) {
                        repository.getStopsByLine(lineCode)
                    }
                _stopList.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchStopPredictions(stopCode: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result =
                    withContext(ioDispatcher) {
                        repository.getStopPredictions(stopCode)
                    }
                _predictions.value = result
            } catch (e: Exception) {
                _errorMessage.value =
                    context.getString(R.string.prediction_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchCorridors() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result =
                    withContext(ioDispatcher) {
                        repository.getCorridors()
                    }
                _corridors.value = result
            } catch (e: Exception) {
                _errorMessage.value =
                    context.getString(R.string.search_corridor_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCorridorsMap() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream =
                    withContext(ioDispatcher) {
                        repository.getCorridorsKml()
                    }
                _kmlData.value = inputStream
            } catch (e: Exception) {
                _errorMessage.value =
                    context.getString(R.string.map_download_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGeneralMap() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream =
                    withContext(ioDispatcher) {
                        repository.getGeneralKml()
                    }
                _kmlData.value = inputStream
            } catch (e: Exception) {
                _errorMessage.value =
                    context.getString(R.string.map_download_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOtherLanesMap() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream =
                    withContext(ioDispatcher) {
                        repository.getOtherLanesKml()
                    }
                _kmlData.value = inputStream
            } catch (e: Exception) {
                _errorMessage.value =
                    context.getString(R.string.map_download_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearchResults() {
        _foundLines.value = emptyList()
    }

    fun clearPredictions() {
        _predictions.value = emptyList()
    }

    fun stopRefresh() {
        refreshJob?.cancel()
    }
}
