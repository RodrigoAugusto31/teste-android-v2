package com.example.sptransapp.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.sptransapp.R
import com.example.sptransapp.data.api.RetrofitClient
import com.example.sptransapp.data.repository.BusRepositoryImpl
import com.example.sptransapp.databinding.FragmentBusBinding
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.presentation.ui.model.BusClusterItem
import com.example.sptransapp.presentation.viewmodel.MapViewModel
import com.example.sptransapp.presentation.viewmodel.MapViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolygonManager
import com.google.maps.android.collections.PolylineManager

class BusFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentBusBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var shouldMoveCamera = false

    private lateinit var clusterManager: ClusterManager<BusClusterItem>

    private lateinit var markerManager: MarkerManager
    private lateinit var polygonManager: PolygonManager
    private lateinit var polylineManager: PolylineManager
    private lateinit var groundOverlayManager: GroundOverlayManager

    private var isSearchingFromThisScreen = false

    private val viewModel: MapViewModel by activityViewModels {
        MapViewModelFactory(
            repository = BusRepositoryImpl(
                api = RetrofitClient.api,
                context = requireContext().applicationContext
            ),
            context = requireContext().applicationContext
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupUI()

        val currentLine = viewModel.selectedLine.value
        if (currentLine != null) {
            binding.edittextSearchBus.setText(currentLine.fullSign)
            binding.btnClearFilter.isVisible = true
        } else if (viewModel.busList.value.isNullOrEmpty()) {
            viewModel.fetchBuses()
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupUI() {
        binding.btnSearchBus.setOnClickListener { performSearch() }

        binding.edittextSearchBus.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.btnClearFilter.setOnClickListener {
            binding.edittextSearchBus.text.clear()
            binding.btnClearFilter.isVisible = false
            viewModel.fetchBuses()
            Toast.makeText(requireContext(),
                getString(R.string.showing_bus_data_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch() {
        val termo = binding.edittextSearchBus.text.toString()
        if (termo.isNotEmpty()) {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.edittextSearchBus.windowToken, 0)
            isSearchingFromThisScreen = true
            viewModel.searchLine(termo)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        markerManager = MarkerManager(map)
        polygonManager = PolygonManager(map)
        polylineManager = PolylineManager(map)
        groundOverlayManager = GroundOverlayManager(map)

        clusterManager = ClusterManager(requireContext(), map)

        val renderer = BusClusterRenderer(requireContext(), map, clusterManager)
        clusterManager.renderer = renderer

        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)

        val saoPaulo = LatLng(-23.5505, -46.6333)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, 12f))
        map.uiSettings.isZoomControlsEnabled = true

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.busList.observe(viewLifecycleOwner) { listaOnibus ->

            clusterManager.clearItems()

            if ((viewModel.deveMoverCamera || shouldMoveCamera) && listaOnibus.isNotEmpty()) {
                val first = listaOnibus[0]
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 14f))
                viewModel.deveMoverCamera = false
                shouldMoveCamera = false
            }

            val clusterItems = listaOnibus.map { onibus ->
                BusClusterItem(onibus)
            }

            clusterManager.addItems(clusterItems)
            clusterManager.cluster()
        }

        viewModel.selectedLine.observe(viewLifecycleOwner) { line ->
            if (line != null) {
                binding.edittextSearchBus.setText(line.fullSign)
                binding.btnClearFilter.isVisible = true
            }
        }

        viewModel.foundLines.observe(viewLifecycleOwner) { lines ->
            if (isSearchingFromThisScreen && isVisible && lines.isNotEmpty()) {
                showLineSelectionDialog(lines)
                isSearchingFromThisScreen = false
            } else if (lines.isEmpty() && isSearchingFromThisScreen) {
                Toast.makeText(requireContext(),
                    getString(R.string.no_lines_found_message), Toast.LENGTH_LONG).show()
                isSearchingFromThisScreen = false
            }
        }

        viewModel.kmlData.observe(viewLifecycleOwner) { inputStream ->
            if (inputStream != null && googleMap != null) {
                try {
                    val layer = com.google.maps.android.data.kml.KmlLayer(
                        googleMap,
                        inputStream,
                        requireContext(),
                        markerManager,
                        polygonManager,
                        polylineManager,
                        groundOverlayManager,
                        null
                    )
                    layer.addLayerToMap()
                    Toast.makeText(requireContext(),
                        getString(R.string.map_loaded_message), Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        getString(R.string.erro_kml_message, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressbarBus.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showLineSelectionDialog(lines: List<Line>) {
        val bottomSheet = LineSelectionBottomSheet(lines) { selectedLine ->
            binding.edittextSearchBus.setText(selectedLine.fullSign)
            binding.btnClearFilter.isVisible = true

            clusterManager.clearItems()
            clusterManager.cluster()

            viewModel.clearSearchResults()

            shouldMoveCamera = true
            viewModel.loadBusesByLine(selectedLine.lineCode)
        }
        bottomSheet.show(parentFragmentManager, LineSelectionBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearSearchResults()
        _binding = null
    }
}
