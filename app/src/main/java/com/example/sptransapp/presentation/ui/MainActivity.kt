package com.example.sptransapp.presentation.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.example.sptransapp.R
import com.example.sptransapp.data.api.RetrofitClient
import com.example.sptransapp.data.repository.OnibusRepositoryImpl
import com.example.sptransapp.databinding.ActivityMainBinding
import com.example.sptransapp.domain.model.Corredor
import com.example.sptransapp.domain.model.Linha
import com.example.sptransapp.domain.model.Parada
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
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolygonManager
import com.google.maps.android.collections.PolylineManager

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private var googleMap: GoogleMap? = null

    private var shouldMoveCamera = false
    private val busMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()

    private var paradaSelecionada: Parada? = null

    private var currentKmlLayer: com.google.maps.android.data.kml.KmlLayer? = null

    private lateinit var markerManager: MarkerManager
    private lateinit var busMarkerCollection: MarkerManager.Collection

    private lateinit var polygonManager: PolygonManager
    private lateinit var polylineManager: PolylineManager
    private lateinit var groundOverlayManager: GroundOverlayManager

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

    override fun onResume() {
        super.onResume()
    }

    private fun setupMap() {
        val mapFragment =
            supportFragmentManager
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
            } else {
                false
            }
        }

        binding.btnClearFilter.setOnClickListener {
            binding.etSearch.text.clear()
            binding.btnClearFilter.isVisible = false
            viewModel.buscarOnibus()
            Toast.makeText(this, "Exibindo todos os ônibus...", Toast.LENGTH_SHORT).show()
        }

        binding.btnCorredores.setOnClickListener {
            val opcoes =
                arrayOf(
                    "Ver Apenas Corredores",
                    "Ver Outras Vias (Ruas/Avs)",
                    "Ver Trânsito Geral (Tudo)",
                    "Limpar Mapa",
                )

            AlertDialog.Builder(this)
                .setTitle("Camadas de Trânsito")
                .setItems(opcoes) { _, which ->
                    if (which != 3) {
                        Toast.makeText(this, "Baixando camada do mapa... aguarde.", Toast.LENGTH_SHORT).show()
                    }

                    when (which) {
                        0 -> viewModel.carregarMapaCorredores()
                        1 -> viewModel.carregarMapaOutrasVias()
                        2 -> viewModel.carregarMapaGeral()
                        3 -> {
                            currentKmlLayer?.removeLayerFromMap()
                            currentKmlLayer = null
                            Toast.makeText(this, "Camadas removidas.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }
    }

    private fun realizarBusca() {
        val termo = binding.etSearch.text.toString()
        if (termo.isNotEmpty()) {
            val imm =
                getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

            viewModel.pesquisarLinha(termo)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        markerManager = MarkerManager(map)
        polygonManager = PolygonManager(map)
        polylineManager = PolylineManager(map)
        groundOverlayManager = GroundOverlayManager(map)

        busMarkerCollection = markerManager.newCollection()

        val saoPaulo = LatLng(-23.5505, -46.6333)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, 12f))
        map.uiSettings.isZoomControlsEnabled = true

        busMarkerCollection.setOnMarkerClickListener { marker ->
            val tag = marker.tag

            if (tag is Parada) {
                paradaSelecionada = tag
                Toast.makeText(this, "Buscando previsões...", Toast.LENGTH_SHORT).show()
                viewModel.buscarPrevisaoDaParada(tag.codigo)
                true
            } else {
                false
            }
        }
    }

    private fun setupObservers() {
        viewModel.onibusList.observe(this) { listaOnibus ->

            busMarkers.forEach { it.remove() }
            busMarkers.clear()

            if (shouldMoveCamera) {
                val primeiro = listaOnibus[0]
                val pos = LatLng(primeiro.latitude, primeiro.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))

                shouldMoveCamera = false
            }

            listaOnibus.forEach { onibus ->
                val marker =
                    busMarkerCollection.addMarker(
                        MarkerOptions()
                            .position(LatLng(onibus.latitude, onibus.longitude))
                            .title("${onibus.letreiro} - ${onibus.sentido}")
                            .snippet("Prefixo: ${onibus.prefixo}"),
                    )

                marker?.tag = "ONIBUS"
                marker?.let { busMarkers.add(it) }
            }
        }

        viewModel.paradasList.observe(this) { listaParadas ->
            listaParadas.forEach { parada ->
                val marker =
                    busMarkerCollection.addMarker(
                        MarkerOptions()
                            .position(LatLng(parada.latitude, parada.longitude))
                            .title("Parada: ${parada.nome}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
                    )
                marker?.tag = parada
            }
        }

        viewModel.linhasEncontradas.observe(this) { linhas ->
            if (linhas.isEmpty()) {
                Toast.makeText(this, "Nenhuma linha encontrada com esse termo.", Toast.LENGTH_LONG)
                    .show()
            } else {
                mostrarDialogSelecaoLinha(linhas)
            }
        }

        viewModel.previsoes.observe(this) { lista ->
            if (lista.isEmpty()) {
                Toast.makeText(this, "Sem previsões para esta parada agora.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                mostrarDialogPrevisoes(lista)
            }
        }

        viewModel.corredores.observe(this) { lista ->
            if (lista.isEmpty()) {
                Toast.makeText(this, "Nenhum corredor encontrado.", Toast.LENGTH_SHORT).show()
            } else {
                mostrarDialogCorredores(lista)
            }
        }

        viewModel.kmlData.observe(this) { inputStream ->
            if (inputStream != null) {
                try {
                    currentKmlLayer?.removeLayerFromMap()

                    val layer =
                        com.google.maps.android.data.kml.KmlLayer(
                            googleMap,
                            inputStream,
                            applicationContext,
                            markerManager,
                            polygonManager,
                            polylineManager,
                            groundOverlayManager,
                            null,
                        )

                    layer.addLayerToMap()
                    currentKmlLayer = layer

                    Toast.makeText(this, "Mapa de Vias carregado!", Toast.LENGTH_SHORT).show()

                    layer.setOnFeatureClickListener { feature ->
                        val nome = feature.getProperty("name") ?: "Via sem nome"
                        val descricao = feature.getProperty("description") ?: ""
                        val descricaoLimpa = android.text.Html.fromHtml(descricao, android.text.Html.FROM_HTML_MODE_COMPACT)

                        AlertDialog.Builder(this)
                            .setTitle(nome)
                            .setMessage(descricaoLimpa)
                            .setPositiveButton("Fechar", null)
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao processar KML: ${e.message}", Toast.LENGTH_LONG).show()
                }
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

                shouldMoveCamera = true

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
            .setNeutralButton("Como Chegar (Rota)") { _, _ ->
                abrirRotaNoMaps()
            }
            .show()
    }

    private fun mostrarDialogCorredores(lista: List<Corredor>) {
        val nomes = lista.map { it.nome }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Corredores de SP")
            .setItems(nomes) { _, which ->
                val corredorSelecionado = lista[which]
                Toast.makeText(this, "Corredor: ${corredorSelecionado.nome}", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun abrirRotaNoMaps() {
        val parada = paradaSelecionada ?: return

        val uri = "https://www.google.com/maps/dir/?api=1&destination=${parada.latitude},${parada.longitude}&travelmode=walking".toUri()

        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir o mapa.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopRefresh()
    }
}
