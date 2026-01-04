package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.repository.BusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class LineDetailsViewModel @Inject constructor(
    private val repository: BusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _busList = MutableLiveData<List<Bus>>()
    val busList: LiveData<List<Bus>> = _busList

    private val _stopList = MutableLiveData<List<Stop>>()
    val stopList: LiveData<List<Stop>> = _stopList

    private val _predictions = MutableLiveData<List<Prediction>>()
    val predictions: LiveData<List<Prediction>> = _predictions

    private val _kmlData = MutableLiveData<InputStream?>()
    val kmlData: LiveData<InputStream?> = _kmlData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    var shouldMoveCamera = true
    private var refreshJob: Job? = null
    private var currentLineCode: Int? = null

    fun startMonitoring(lineCode: Int) {
        if (currentLineCode == lineCode && refreshJob?.isActive == true) return

        currentLineCode = lineCode
        shouldMoveCamera = true

        loadStops(lineCode)

        startAutoRefresh {
            repository.getPositionsByLine(lineCode)
        }
    }

    private fun startAutoRefresh(action: suspend () -> List<Bus>) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.value = true
            while (isActive) {
                try {
                    val result = withContext(Dispatchers.IO) { action() }
                    _busList.value = result
                    _errorMessage.value = null
                } catch (e: Exception) {
                    _errorMessage.value = null
                } finally {
                    _isLoading.value = false
                }
                delay(15_000)
            }
        }
    }

    private fun loadStops(lineCode: Int) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
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
                val result = withContext(Dispatchers.IO) {
                    repository.getStopPredictions(stopCode)
                }
                _predictions.value = result
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.prediction_error_message, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCorridorsMap() = loadKml { repository.getCorridorsKml() }
    fun loadOtherLanesMap() = loadKml { repository.getOtherLanesKml() }
    fun loadGeneralMap() = loadKml { repository.getGeneralKml() }

    private fun loadKml(source: suspend () -> InputStream?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream = withContext(Dispatchers.IO) { source() }
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

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
