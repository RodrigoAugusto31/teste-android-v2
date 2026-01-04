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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.sptransapp.R
import com.example.sptransapp.databinding.FragmentBusBinding
import com.example.sptransapp.domain.model.Bus
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
import kotlinx.coroutines.launch

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

    private val currentClusterItems = mutableMapOf<String, BusClusterItem>()

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

        if (viewModel.busList.value.isEmpty()) {
            viewModel.fetchInitialBuses()
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupUI() {
        binding.btnSearchBus.setOnClickListener { performSearch() }
        binding.edittextSearchBus.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else { false }
        }
        binding.btnClearFilter.setOnClickListener {
            binding.edittextSearchBus.text.clear()
            binding.btnClearFilter.isVisible = false
            viewModel.clearFilter()
            Toast.makeText(requireContext(), getString(R.string.showing_bus_data_message), Toast.LENGTH_SHORT).show()
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
        clusterManager = ClusterManager(requireContext(), map, markerManager)

        val renderer = BusClusterRenderer(requireContext(), map, clusterManager)
        clusterManager.renderer = renderer
        map.setOnCameraIdleListener(clusterManager)

        val saoPaulo = LatLng(-23.5505, -46.6333)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, 12f))
        map.uiSettings.isZoomControlsEnabled = true

        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.busList.collect { busList ->
                        updateMapMarkersSmart(busList)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressbarBus.isVisible = isLoading
                    }
                }
            }
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
                    val layer = KmlLayer(googleMap, inputStream, requireContext(), markerManager, polygonManager, polylineManager, groundOverlayManager, null)
                    layer.addLayerToMap()
                    Toast.makeText(requireContext(), getString(R.string.map_loaded_message), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.erro_kml_message, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun updateMapMarkersSmart(listBus: List<Bus>) {
        if (googleMap == null) return

        if (viewModel.shouldMoveCamera && listBus.isNotEmpty()) {
            val first = listBus[0]
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 14f))
            viewModel.shouldMoveCamera = false
        }

        if (listBus.size > 500 && currentClusterItems.isEmpty()) {
            clusterManager.clearItems()
            val newItems = listBus.map { BusClusterItem(it) }
            clusterManager.addItems(newItems)
            newItems.forEach { currentClusterItems[it.bus.prefix] = it } // Salva no cache
            clusterManager.cluster()
            return
        }

        val newBusIds = listBus.map { it.prefix }.toSet()
        val itemsToRemove = mutableListOf<BusClusterItem>()
        val itemsToAdd = mutableListOf<BusClusterItem>()

        val iterator = currentClusterItems.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in newBusIds) {
                itemsToRemove.add(entry.value)
                iterator.remove()
            }
        }
        clusterManager.removeItems(itemsToRemove)

        listBus.forEach { bus ->
            val existingItem = currentClusterItems[bus.prefix]

            if (existingItem != null) {
                if (existingItem.position.latitude != bus.latitude ||
                    existingItem.position.longitude != bus.longitude) {

                    clusterManager.removeItem(existingItem)

                    val newItem = BusClusterItem(bus)
                    clusterManager.addItem(newItem)
                    currentClusterItems[bus.prefix] = newItem
                }
            } else {
                val newItem = BusClusterItem(bus)
                itemsToAdd.add(newItem)
                currentClusterItems[bus.prefix] = newItem
            }
        }

        clusterManager.addItems(itemsToAdd)
        clusterManager.cluster()
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
