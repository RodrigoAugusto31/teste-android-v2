package com.example.sptransapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.R
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.usecase.GetFavoritesUseCase
import com.example.sptransapp.domain.usecase.SearchLinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LinesViewModel @Inject constructor(
    private val searchLinesUseCase: SearchLinesUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _displayedLines = MutableLiveData<List<Line>>()
    val displayedLines: LiveData<List<Line>> = _displayedLines

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        _isLoading.value = true
        viewModelScope.launch {
            getFavoritesUseCase().collect { favorites ->
                _displayedLines.value = favorites
                _isLoading.value = false
            }
        }
    }

    fun searchLine(query: String) {
        if (query.isBlank()) {
            loadFavorites()
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    searchLinesUseCase(query)
                }
                _displayedLines.value = result
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.search_error_message, e.message)
                _displayedLines.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
