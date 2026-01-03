package com.example.sptransapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.domain.model.Prediction
import com.example.sptransapp.domain.repository.BusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StopViewModel(private val repository: BusRepository) : ViewModel() {


}
