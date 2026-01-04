package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.repository.BusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LinesViewModel @Inject constructor(
    private val repository: BusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _foundLines = MutableLiveData<List<Line>>()
    val foundLines: LiveData<List<Line>> = _foundLines

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
                _foundLines.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
