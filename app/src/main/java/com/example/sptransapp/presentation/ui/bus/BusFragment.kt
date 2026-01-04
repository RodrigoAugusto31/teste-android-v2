package com.example.sptransapp.presentation.ui.bus

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.sptransapp.R
import com.example.sptransapp.databinding.FragmentBusBinding
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.presentation.ui.common.LineSelectionBottomSheet
import com.example.sptransapp.presentation.ui.bus.model.BusClusterItem
import com.example.sptransapp.presentation.viewmodel.BusViewModel
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
import com.google.maps.android.data.kml.KmlLayer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BusFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentBusBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

    private lateinit var clusterManager: ClusterManager<BusClusterItem>
    private lateinit var markerManager: MarkerManager
    private lateinit var polygonManager: PolygonManager
    private lateinit var polylineManager: PolylineManager
    private lateinit var groundOverlayManager: GroundOverlayManager

    private var isSearchingFromThisScreen = false

    private val viewModel: BusViewModel by viewModels()

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

        if (viewModel.busList.value.isNullOrEmpty()) {
            viewModel.fetchInitialBuses()
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

            viewModel.clearFilter()
            Toast.makeText(requireContext(),
                getString(R.string.showing_bus_data_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch() {
        val termo = binding.edittextSearchBus.text.toString()
        if (termo.isNotEmpty()) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

            if (viewModel.shouldMoveCamera && listaOnibus.isNotEmpty()) {
                val first = listaOnibus[0]
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            first.latitude,
                            first.longitude
                        ), 14f))
                viewModel.shouldMoveCamera = false
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
                    val layer = KmlLayer(
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

            clusterManager.clearItems()
            clusterManager.cluster()

            viewModel.clearSearchResults()
            viewModel.loadBusesByLine(selectedLine)
        }
        bottomSheet.show(parentFragmentManager, LineSelectionBottomSheet.Companion.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
