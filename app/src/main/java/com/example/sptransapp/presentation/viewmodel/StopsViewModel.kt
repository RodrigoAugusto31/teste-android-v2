package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.repository.BusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StopsViewModel @Inject constructor(
    private val repository: BusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _stopList = MutableLiveData<List<Stop>>()
    val stopList: LiveData<List<Stop>> = _stopList

    private val _foundLines = MutableLiveData<List<Line>>()
    val foundLines: LiveData<List<Line>> = _foundLines

    private val _predictions = MutableLiveData<List<Prediction>>()
    val predictions: LiveData<List<Prediction>> = _predictions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun searchLine(query: String) {
        if (query.isBlank()) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
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

    fun loadStops(lineCode: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getStopsByLine(lineCode)
                }
                _stopList.value = result
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.att_error_message, e.message)
            } finally {
                _isLoading.value = false
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

    fun clearSearchResults() {
        _foundLines.value = emptyList()
    }

    fun clearPredictions() {
        _predictions.value = emptyList()
    }

    fun clearMapData() {
        _stopList.value = emptyList()
        clearPredictions()
    }
}
