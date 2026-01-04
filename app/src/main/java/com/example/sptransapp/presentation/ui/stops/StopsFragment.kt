package com.example.sptransapp.presentation.ui.stops

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.sptransapp.R
import com.example.sptransapp.databinding.FragmentStopsBinding
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.presentation.ui.common.LineSelectionBottomSheet
import com.example.sptransapp.presentation.ui.common.PredictionBottomSheet
import com.example.sptransapp.presentation.viewmodel.StopsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.collections.MarkerManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StopsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentStopsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var markerManager: MarkerManager
    private lateinit var stopsMarkerCollection: MarkerManager.Collection

    private var selectedStop: Stop? = null

    private val viewModel: StopsViewModel by viewModels()

    private var isSearchingFromThisScreen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStopsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupUI()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupUI() {
        binding.btnSearchStop.setOnClickListener {
            performSearch()
        }

        binding.edittextSearchStop.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.btnClearMap.setOnClickListener {
            binding.edittextSearchStop.text.clear()
            binding.btnClearMap.isVisible = false
            stopsMarkerCollection.clear()
            viewModel.clearMapData()
            Toast.makeText(requireContext(),
                getString(R.string.clean_map_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch() {
        val query = binding.edittextSearchStop.text.toString()
        if (query.isNotEmpty()) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edittextSearchStop.windowToken, 0)

            isSearchingFromThisScreen = true
            viewModel.searchLine(query)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        markerManager = MarkerManager(map)
        stopsMarkerCollection = markerManager.newCollection()

        val saoPaulo = LatLng(-23.5505, -46.6333)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(saoPaulo, 12f))

        stopsMarkerCollection.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is Stop) {
                selectedStop = tag
                Toast.makeText(requireContext(), getString(R.string.search_predictions_message), Toast.LENGTH_SHORT).show()
                viewModel.fetchStopPredictions(tag.stopCode)
                true
            } else {
                false
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.foundLines.observe(viewLifecycleOwner) { lines ->
            if (isSearchingFromThisScreen && isVisible && lines.isNotEmpty()) {
                showLineSelectionDialog(lines)
                isSearchingFromThisScreen = false
            }
        }

        viewModel.stopList.observe(viewLifecycleOwner) { stopList ->
            stopsMarkerCollection.clear()

            if (stopList.isNotEmpty()) {
                val primeira = stopList[0]
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            primeira.latitude,
                            primeira.longitude
                        ), 14f))
                binding.btnClearMap.isVisible = true
            }

            stopList.forEach { parada ->
                val marker = stopsMarkerCollection.addMarker(
                    MarkerOptions()
                        .position(LatLng(parada.latitude, parada.longitude))
                        .title("Stop: ${parada.name}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                marker?.tag = parada
            }
        }

        viewModel.predictions.observe(viewLifecycleOwner) { list ->
            if (isVisible && list.isNotEmpty()) {
                val nomeParada = selectedStop?.name ?: "Stop"
                val bottomSheet = PredictionBottomSheet(nomeParada, list) {
                    openRouteInMaps()
                }
                bottomSheet.show(parentFragmentManager, PredictionBottomSheet.Companion.TAG)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressbarStop.isVisible = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (isVisible) error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showLineSelectionDialog(lines: List<Line>) {
        val bottomSheet = LineSelectionBottomSheet(lines) { selectedLine ->
            binding.edittextSearchStop.setText(selectedLine.fullSign)
            stopsMarkerCollection.clear()
            viewModel.clearSearchResults()

            viewModel.loadStops(selectedLine.lineCode)
        }
        bottomSheet.show(parentFragmentManager, LineSelectionBottomSheet.Companion.TAG)
    }

    private fun openRouteInMaps() {
        val stop = selectedStop ?: return
        val uri = "http://maps.google.com/maps?daddr=${stop.latitude},${stop.longitude}&dirflg=w".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.google_maps_error_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
