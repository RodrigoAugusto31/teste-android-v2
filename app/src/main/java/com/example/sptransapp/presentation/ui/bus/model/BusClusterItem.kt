package com.example.sptransapp.presentation.ui.bus.model

import com.example.sptransapp.domain.model.Bus
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class BusClusterItem(val bus: Bus) : ClusterItem {

    override fun getPosition(): LatLng {
        return LatLng(bus.latitude, bus.longitude)
    }

    override fun getTitle(): String {
        return "${bus.fullSign} - ${bus.destination}"
    }

    override fun getSnippet(): String {
        return "Prefixo: ${bus.prefix}"
    }

    override fun getZIndex(): Float {
        return 0f
    }
}
