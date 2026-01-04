package com.example.sptransapp.presentation.ui.lines

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sptransapp.R
import com.example.sptransapp.databinding.FragmentLineDetailsBinding
import com.example.sptransapp.domain.model.Bus
import com.example.sptransapp.domain.model.Line
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.presentation.ui.common.LatLngInterpolator
import com.example.sptransapp.presentation.ui.common.MarkerAnimator
import com.example.sptransapp.presentation.ui.common.PredictionBottomSheet
import com.example.sptransapp.presentation.viewmodel.LineDetailsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolylineManager
import com.google.maps.android.data.kml.KmlLayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LineDetailsFragment :
    Fragment(),
    OnMapReadyCallback {
    private var _binding: FragmentLineDetailsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var currentKmlLayer: KmlLayer? = null
    private var selectedLayerId: Int? = null
    private var selectedStop: Stop? = null

    private var currentLine: Line? = null

    private val viewModel: LineDetailsViewModel by viewModels()

    private lateinit var markerManager: MarkerManager
    private lateinit var busMarkerCollection: MarkerManager.Collection
    private lateinit var stopsMarkerCollection: MarkerManager.Collection
    private lateinit var polylineManager: PolylineManager
    private lateinit var routePolylineCollection: PolylineManager.Collection

    private val busMarkersMap = mutableMapOf<String, com.google.android.gms.maps.model.Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLineDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { bundle ->
            currentLine = BundleCompat.getParcelable(bundle, "selectedLine", Line::class.java)
        }

        if (currentLine == null) {
            Toast.makeText(context, "Erro ao carregar linha", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel.startMonitoring(currentLine!!.lineCode)
        setupUI()

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnLayers.setOnClickListener {
            val bottomSheet =
                LayersBottomSheet(selectedLayerId) { newId ->
                    selectedLayerId = newId
                    handleLayerSelection(newId)
                }
            bottomSheet.show(parentFragmentManager, LayersBottomSheet.Companion.TAG)
        }

        currentLine?.let { line ->
            binding.textviewSign.text = line.fullSign
            binding.textviewName.text = line.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun handleLayerSelection(id: Int?) {
        if (id == null) {
            currentKmlLayer?.removeLayerFromMap()
            currentKmlLayer = null
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.hidden_layers_message),
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }

        Toast
            .makeText(
                requireContext(),
                getString(R.string.loading_layer_message),
                Toast.LENGTH_SHORT,
            ).show()
        currentKmlLayer?.removeLayerFromMap()
        currentKmlLayer = null

        viewModel.loadMapLayer(id)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        markerManager = MarkerManager(map)
        polylineManager = PolylineManager(map)

        busMarkerCollection = markerManager.newCollection()
        stopsMarkerCollection = markerManager.newCollection()
        routePolylineCollection = polylineManager.newCollection()

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.5505, -46.6333), 12f))

        stopsMarkerCollection.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is Stop) {
                selectedStop = tag
                Toast
                    .makeText(
                        requireContext(),
                        getString(R.string.search_predictions_message),
                        Toast.LENGTH_SHORT,
                    ).show()
                viewModel.fetchStopPredictions(tag.stopCode)
                true
            } else {
                false
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.busList.collect { busList ->
                    updateBusMarkers(busList)
                }
            }
        }

        viewModel.stopList.observe(viewLifecycleOwner) { stopList ->
            stopsMarkerCollection.clear()

            if (stopList.isNotEmpty()) {
                stopList.forEach { stop ->
                    stopsMarkerCollection
                        .addMarker(
                            MarkerOptions()
                                .position(LatLng(stop.latitude, stop.longitude))
                                .title(stop.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
                        )?.tag = stop
                }
            }
        }

        viewModel.predictions.observe(viewLifecycleOwner) { list ->
            if (isVisible && list.isNotEmpty()) {
                val stopName = selectedStop?.name ?: getString(R.string.stop_label)
                val bottomSheet =
                    PredictionBottomSheet(stopName, list) {
                        openRouteInMaps()
                    }
                bottomSheet.show(parentFragmentManager, PredictionBottomSheet.Companion.TAG)
            } else if (isVisible && selectedStop != null) {
                Toast
                    .makeText(
                        requireContext(),
                        getString(R.string.no_stop_prediction_message),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

        viewModel.kmlData.observe(viewLifecycleOwner) { inputStream ->
            if (inputStream != null && googleMap != null) {
                try {
                    currentKmlLayer?.removeLayerFromMap()

                    val layer =
                        KmlLayer(
                            googleMap,
                            inputStream,
                            requireContext(),
                            markerManager,
                            null,
                            null,
                            null,
                            null,
                        )
                    layer.addLayerToMap()
                    currentKmlLayer = layer
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.layer_loaded_message),
                            Toast.LENGTH_SHORT,
                        ).show()

                    layer.setOnFeatureClickListener { feature ->
                        val name = feature.getProperty("name") ?: getString(R.string.info_label)
                        val rawDescription = feature.getProperty("description") ?: ""
                        val formattedDesc = Html.fromHtml(rawDescription, Html.FROM_HTML_MODE_COMPACT)
                        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_info_kml, null)
                        val textViewTitle = dialogView.findViewById<TextView>(R.id.textview_tittle)
                        val textViewDescription = dialogView.findViewById<TextView>(R.id.textview_description)
                        val buttonClone = dialogView.findViewById<View>(R.id.btn_close)

                        textViewTitle.text = name

                        if (formattedDesc.isNullOrBlank()) {
                            textViewDescription.text = getString(R.string.no_description_message)
                        } else {
                            textViewDescription.text = formattedDesc
                        }

                        val dialog =
                            AlertDialog
                                .Builder(requireContext())
                                .setView(dialogView)
                                .create()

                        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

                        buttonClone.setOnClickListener {
                            dialog.dismiss()
                        }

                        dialog.show()
                    }
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.kml_error_message),
                            Toast.LENGTH_SHORT,
                        ).show()
                    e.printStackTrace()
                }
            }
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFav ->
            val icon = if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            binding.btnFavorite.setImageResource(icon)
        }

        binding.btnFavorite.setOnClickListener {
            currentLine?.let { line ->
                viewModel.toggleFavorite(line)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { visible ->
            binding.progressBar.isVisible = visible
        }
    }

    private fun openRouteInMaps() {
        val stop = selectedStop ?: return
        val uri = "http://maps.google.com/maps?daddr=${stop.latitude},${stop.longitude}&dirflg=w".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (_: Exception) {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.google_maps_error_message),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    private fun updateBusMarkers(busList: List<Bus>) {
        if (googleMap == null) return

        if (busList.isNotEmpty() && viewModel.shouldMoveCamera) {
            val first = busList[0]
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 14f))
            viewModel.shouldMoveCamera = false
        }

        val currentBusIds = busList.map { it.prefix }.toSet()

        val iterator = busMarkersMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in currentBusIds) {
                entry.value.remove()
                iterator.remove()
            }
        }

        busList.forEach { bus ->
            val existingMarker = busMarkersMap[bus.prefix]

            if (existingMarker != null) {
                if (existingMarker.position.latitude != bus.latitude ||
                    existingMarker.position.longitude != bus.longitude
                ) {
                    MarkerAnimator.animateMarkerToGB(
                        existingMarker,
                        LatLng(bus.latitude, bus.longitude),
                        LatLngInterpolator.Linear(),
                    )
                }
                existingMarker.snippet = bus.fullSign
            } else {
                val markerOptions =
                    MarkerOptions()
                        .position(LatLng(bus.latitude, bus.longitude))
                        .title(bus.prefix)
                        .snippet(bus.fullSign)

                val marker = busMarkerCollection.addMarker(markerOptions)

                if (marker != null) {
                    marker.tag = "ONIBUS"
                    busMarkersMap[bus.prefix] = marker
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearPredictions()
        _binding = null
    }
}
