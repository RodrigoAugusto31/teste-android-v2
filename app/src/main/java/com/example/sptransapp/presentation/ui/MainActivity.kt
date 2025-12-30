package com.example.sptransapp.presentation.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.sptransapp.R
import com.example.sptransapp.data.api.RetrofitClient
import com.example.sptransapp.data.repository.OnibusRepositoryImpl
import com.example.sptransapp.databinding.ActivityMainBinding
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Previsao
import com.example.sptransapp.presentation.viewmodel.MapViewModel
import com.example.sptransapp.presentation.viewmodel.MapViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(OnibusRepositoryImpl(RetrofitClient.api))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupUI()
        setupObservers()

        viewModel.buscarOnibus()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupUI() {
        binding.btnSearch.setOnClickListener {
            realizarBusca()
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                realizarBusca()
                true
            } else false
        }

        binding.btnClearFilter.setOnClickListener {
            binding.etSearch.text.clear()
            binding.btnClearFilter.isVisible = false
            viewModel.buscarOnibus()
            Toast.makeText(this, "Exibindo todos os ônibus...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun realizarBusca() {
        val termo = binding.etSearch.text.toString()
        if (termo.isNotEmpty()) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

            viewModel.pesquisarLinha(termo)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val saoPaulo = LatLng(-23.5505, -46.6333)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, 12f))
        map.uiSettings.isZoomControlsEnabled = true

        map.setOnMarkerClickListener { marker ->
            val tag = marker.tag

            if (tag is Int) {
                Toast.makeText(this, "Buscando previsões...", Toast.LENGTH_SHORT).show()
                viewModel.buscarPrevisaoDaParada(tag)
                false
            } else {
                false
            }
        }
    }

    private fun setupObservers() {
        viewModel.onibusList.observe(this) { listaOnibus ->
            googleMap?.clear()

            if (listaOnibus.isEmpty()) {
                Toast.makeText(this, "Nenhum ônibus encontrado.", Toast.LENGTH_SHORT).show()
            } else {
                if (binding.btnClearFilter.isVisible && listaOnibus.isNotEmpty()) {
                    val primeiro = listaOnibus[0]
                    val pos = LatLng(primeiro.latitude, primeiro.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
                }
            }

            listaOnibus.forEach { onibus ->
                googleMap?.addMarker(
                    MarkerOptions()
                        .position(LatLng(onibus.latitude, onibus.longitude))
                        .title("${onibus.letreiro} - ${onibus.sentido}")
                        .snippet("Prefixo: ${onibus.prefixo}")
                )
            }
        }

        viewModel.paradasList.observe(this) { listaParadas ->

            listaParadas.forEach { parada ->
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(LatLng(parada.latitude, parada.longitude))
                        .title("Parada: ${parada.nome}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                marker?.tag = parada.codigo
            }
        }

        viewModel.linhasEncontradas.observe(this) { linhas ->
            if (linhas.isEmpty()) {
                Toast.makeText(this, "Nenhuma linha encontrada com esse termo.", Toast.LENGTH_LONG).show()
            } else {
                mostrarDialogSelecaoLinha(linhas)
            }
        }

        viewModel.previsoes.observe(this) { lista ->
            if (lista.isEmpty()) {
                Toast.makeText(this, "Sem previsões para esta parada agora.", Toast.LENGTH_SHORT).show()
            } else {
                mostrarDialogPrevisoes(lista)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
        viewModel.errorMessage.observe(this) { erro ->
            erro?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun mostrarDialogSelecaoLinha(linhas: List<Linha>) {
        val itens = linhas.map { "${it.letreiroCompleto} - ${it.nome}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecione a Linha")
            .setItems(itens) { _, which ->
                val linhaSelecionada = linhas[which]
                binding.etSearch.setText(linhaSelecionada.letreiroCompleto)
                binding.btnClearFilter.isVisible = true

                googleMap?.clear()

                viewModel.carregarOnibusDaLinha(linhaSelecionada.codigoLinha)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogPrevisoes(lista: List<Previsao>) {
        val itens = lista.map { it.toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Próximos Ônibus")
            .setItems(itens, null)
            .setPositiveButton("OK", null)
            .show()
    }
}