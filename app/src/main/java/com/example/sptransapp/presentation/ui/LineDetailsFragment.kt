package com.example.sptransapp.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.sptransapp.R
import com.example.sptransapp.data.api.RetrofitClient
import com.example.sptransapp.data.repository.BusRepositoryImpl
import com.example.sptransapp.databinding.FragmentLineDetailsBinding
import com.example.sptransapp.domain.model.Stop
import com.example.sptransapp.presentation.viewmodel.MapViewModel
import com.example.sptransapp.presentation.viewmodel.MapViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolylineManager

class LineDetailsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLineDetailsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

    private var currentKmlLayer: com.google.maps.android.data.kml.KmlLayer? = null
    private var selectedLayerId: Int? = null
    private var selectedStop: Stop? = null

    private val viewModel: MapViewModel by activityViewModels {
        MapViewModelFactory(
            repository = BusRepositoryImpl(
                api = RetrofitClient.api,
                context = requireContext().applicationContext
            ),
            context = requireContext().applicationContext
        )
    }

    private lateinit var markerManager: MarkerManager
    private lateinit var busMarkerCollection: MarkerManager.Collection
    private lateinit var stopsMarkerCollection: MarkerManager.Collection
    private lateinit var polylineManager: PolylineManager
    private lateinit var routePolylineCollection: PolylineManager.Collection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLineDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.clearPredictions()
        setupUI()
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnLayers.setOnClickListener {
            val bottomSheet = LayersBottomSheet(selectedLayerId) { newId ->
                selectedLayerId = newId
                handleLayerSelection(newId)
            }
            bottomSheet.show(parentFragmentManager, LayersBottomSheet.TAG)
        }

        viewModel.selectedLine.value?.let { line ->
            binding.textviewSign.text = line.fullSign
            binding.textviewName.text = line.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun handleLayerSelection(id: Int?) {
        if (id == null) {
            currentKmlLayer?.removeLayerFromMap()
            currentKmlLayer = null
            Toast.makeText(requireContext(),
                getString(R.string.hidden_layers_message), Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(),
            getString(R.string.loading_layer_message), Toast.LENGTH_SHORT).show()
        currentKmlLayer?.removeLayerFromMap()
        currentKmlLayer = null

        when (id) {
            0 -> viewModel.loadCorridorsMap()
            1 -> viewModel.loadOtherLanesMap()
            2 -> viewModel.loadGeneralMap()
        }
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
                Toast.makeText(requireContext(),
                    getString(R.string.search_predictions_message), Toast.LENGTH_SHORT).show()
                viewModel.fetchStopPredictions(tag.stopCode)
                true
            } else {
                false
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.busList.observe(viewLifecycleOwner) { busList ->
            busMarkerCollection.clear()
            if (busList.isNotEmpty() && viewModel.deveMoverCamera) {
                val primeiro = busList[0]
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(primeiro.latitude, primeiro.longitude), 14f))
                viewModel.deveMoverCamera = false
            }
            busList.forEach { bus ->
                busMarkerCollection.addMarker(
                    MarkerOptions()
                        .position(LatLng(bus.latitude, bus.longitude))
                        .title(bus.prefix)
                        .snippet(bus.fullSign)
                )?.tag = "ONIBUS"
            }
        }

        viewModel.stopList.observe(viewLifecycleOwner) { stopList ->
            stopsMarkerCollection.clear()
            routePolylineCollection.clear()
            if (stopList.isNotEmpty()) {
                stopList.forEach { stop ->
                    stopsMarkerCollection.addMarker(
                        MarkerOptions()
                            .position(LatLng(stop.latitude, stop.longitude))
                            .title(stop.name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )?.tag = stop
                }
            }
        }

        viewModel.predictions.observe(viewLifecycleOwner) { list ->
            if (isVisible && list.isNotEmpty()) {
                val stopName = selectedStop?.name ?: getString(R.string.stop_label)

                val bottomSheet = PredictionBottomSheet(stopName, list) {
                    openRouteInMaps()
                }
                bottomSheet.show(parentFragmentManager, PredictionBottomSheet.TAG)
            } else if (isVisible && selectedStop != null) {
                Toast.makeText(requireContext(),
                    getString(R.string.no_stop_prediction_message), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.kmlData.observe(viewLifecycleOwner) { inputStream ->
            if (inputStream != null && googleMap != null) {
                try {
                    currentKmlLayer?.removeLayerFromMap()

                    val layer = com.google.maps.android.data.kml.KmlLayer(
                        googleMap,
                        inputStream,
                        requireContext(),
                        markerManager,
                        null, null, null, null
                    )
                    layer.addLayerToMap()
                    currentKmlLayer = layer
                    Toast.makeText(requireContext(),
                        getString(R.string.layer_loaded_message), Toast.LENGTH_SHORT).show()

                    layer.setOnFeatureClickListener { feature ->
                        val name = feature.getProperty("name") ?: getString(R.string.info_label)
                        val rawDescription = feature.getProperty("description") ?: ""
                        val formattedDesc = android.text.Html.fromHtml(rawDescription, android.text.Html.FROM_HTML_MODE_COMPACT)
                        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_info_kml, null)
                        val textViewTitle = dialogView.findViewById<android.widget.TextView>(R.id.textview_tittle)
                        val textViewDescription = dialogView.findViewById<android.widget.TextView>(R.id.textview_description)
                        val buttonClone = dialogView.findViewById<View>(R.id.btn_close)

                        textViewTitle.text = name

                        if (formattedDesc.isNullOrBlank()) {
                            textViewDescription.text = getString(R.string.no_description_message)
                        } else {
                            textViewDescription.text = formattedDesc
                        }

                        val dialog = AlertDialog.Builder(requireContext())
                            .setView(dialogView)
                            .create()

                        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

                        buttonClone.setOnClickListener {
                            dialog.dismiss()
                        }

                        dialog.show()
                    }


                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        getString(R.string.kml_error_message), Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { visible ->
            binding.progressBar.isVisible = visible
        }
    }

    private fun openRouteInMaps() {
        val stop = selectedStop ?: return
        val uri = "google.navigation:q=${stop.latitude},${stop.longitude}&mode=w".toUri()
        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                getString(R.string.google_maps_error_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearPredictions()
        _binding = null
    }
}
