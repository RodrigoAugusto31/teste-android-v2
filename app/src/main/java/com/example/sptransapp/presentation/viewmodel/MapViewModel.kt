package com.example.sptransapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sptransapp.domain.model.Corredor
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Onibus
import com.example.sptransapp.domain.model.Parada
import com.example.sptransapp.domain.model.Previsao
import com.example.sptransapp.domain.repository.OnibusRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(
    private val repository: OnibusRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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

    private val _corredores = MutableLiveData<List<Corredor>>()
    val corredores: LiveData<List<Corredor>> = _corredores

    private val _kmlData = MutableLiveData<java.io.InputStream?>()
    val kmlData: LiveData<java.io.InputStream?> = _kmlData

    private var refreshJob: Job? = null

    private fun startAutoRefresh(action: suspend () -> List<Onibus>) {
        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {
            _isLoading.value = true

            while (isActive) {
                try {
                    val resultado = withContext(ioDispatcher) {
                        action()
                    }
                    _onibusList.value = resultado
                    _errorMessage.value = null

                } catch (e: Exception) {
                    _errorMessage.value = "Erro na atualização: ${e.message}"
                } finally {
                    _isLoading.value = false
                }

                delay(15_000)
            }
        }
    }

    fun buscarOnibus() {
        _paradasList.value = emptyList()

        startAutoRefresh {
            repository.buscarPosicoes()
        }
    }

    fun pesquisarLinha(termo: String) {
        if (termo.isBlank()) return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val resultado = withContext(ioDispatcher) {
                    repository.buscarLinhas(termo)
                }
                _linhasEncontradas.value = resultado
            } catch (e: Exception) {
                _errorMessage.value = "Erro na busca: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarOnibusDaLinha(codigoLinha: Int) {
        carregarParadas(codigoLinha)

        startAutoRefresh {
            repository.buscarPosicoesPorLinha(codigoLinha)
        }
    }

    private fun carregarParadas(codigoLinha: Int) {
        viewModelScope.launch {
            try {
                val resultado = withContext(ioDispatcher) {
                    repository.buscarParadasPorLinha(codigoLinha)
                }
                _paradasList.value = resultado
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun buscarPrevisaoDaParada(codigoParada: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val resultado = withContext(ioDispatcher) {
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

    fun buscarListaCorredores() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val resultado = withContext(ioDispatcher) {
                    repository.buscarCorredores()
                }
                _corredores.value = resultado
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao buscar corredores: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarMapaCorredores() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream = withContext(ioDispatcher) {
                    repository.buscarKmlCorredores()
                }
                _kmlData.value = inputStream
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao baixar mapa de corredores: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarMapaGeral() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val inputStream = withContext(ioDispatcher) {
                    repository.buscarKmlGeral()
                }
                _kmlData.value = inputStream
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao baixar mapa geral: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopRefresh() {
        refreshJob?.cancel()
    }
}