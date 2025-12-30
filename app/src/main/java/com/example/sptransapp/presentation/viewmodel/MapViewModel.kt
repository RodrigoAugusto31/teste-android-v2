package com.example.sptransapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Onibus
import com.example.sptransapp.domain.model.Parada
import com.example.sptransapp.domain.model.Previsao
import com.example.sptransapp.domain.repository.OnibusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(
    private val repository: OnibusRepository
) : ViewModel() {

    private val _onibusList = MutableLiveData<List<Onibus>>()
    val onibusList: LiveData<List<Onibus>> = _onibusList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _paradasList = MutableLiveData<List<Parada>>()
    val paradasList: LiveData<List<Parada>> = _paradasList

    private val _linhasEncontradas = MutableLiveData<List<Linha>>()
    val linhasEncontradas: LiveData<List<Linha>> = _linhasEncontradas

    private val _previsoes = MutableLiveData<List<Previsao>>()
    val previsoes: LiveData<List<Previsao>> = _previsoes

    fun buscarOnibus() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    repository.buscarPosicoes()
                }

                _onibusList.value = resultado

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Erro ao carregar autocarros: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun pesquisarLinha(termo: String) {
        if (termo.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    repository.buscarLinhas(termo)
                }
                _linhasEncontradas.value = resultado
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao buscar linha: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarOnibusDaLinha(codigoLinha: Int) {
        _isLoading.value = true
        _paradasList.value = emptyList()

        viewModelScope.launch {
            try {
                val onibusResult = withContext(Dispatchers.IO) {
                    repository.buscarPosicoesPorLinha(codigoLinha)
                }
                _onibusList.value = onibusResult

                val paradasResult = withContext(Dispatchers.IO) {
                    repository.buscarParadasPorLinha(codigoLinha)
                }
                _paradasList.value = paradasResult

            } catch (e: Exception) {
                _errorMessage.value = "Erro ao filtrar linha/paradas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun buscarPrevisaoDaParada(codigoParada: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    repository.buscarPrevisaoParada(codigoParada)
                }
                _previsoes.value = resultado
            } catch (e: Exception) {
                _errorMessage.value = "Erro na previsão: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}