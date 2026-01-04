package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Line
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
class BusViewModel @Inject constructor(
    private val repository: BusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _busList = MutableLiveData<List<Bus>>()
    val busList: LiveData<List<Bus>> = _busList

    private val _foundLines = MutableLiveData<List<Line>>()
    val foundLines: LiveData<List<Line>> = _foundLines

    private val _selectedLine = MutableLiveData<Line?>()
    val selectedLine: LiveData<Line?> = _selectedLine

    private val _kmlData = MutableLiveData<InputStream?>()
    val kmlData: LiveData<InputStream?> = _kmlData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    var shouldMoveCamera = false
    private var refreshJob: Job? = null

    fun fetchInitialBuses() {
        shouldMoveCamera = false
        startAutoRefresh {
            repository.getPositions()
        }
    }

    fun searchLine(query: String) {
        if (query.isBlank()) return
        _isLoading.value = true

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

    fun loadBusesByLine(line: Line) {
        _selectedLine.value = line
        shouldMoveCamera = true

        startAutoRefresh {
            repository.getPositionsByLine(line.lineCode)
        }
    }

    fun clearFilter() {
        _selectedLine.value = null
        fetchInitialBuses()
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

    fun clearSearchResults() {
        _foundLines.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
