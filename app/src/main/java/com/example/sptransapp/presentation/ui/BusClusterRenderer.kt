package com.example.sptransapp.presentation.ui

import android.content.Context
import com.example.sptransapp.presentation.ui.model.BusClusterItem
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class BusClusterRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<BusClusterItem>
) : DefaultClusterRenderer<BusClusterItem>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: BusClusterItem, markerOptions: MarkerOptions) {
        markerOptions.title(item.title)
        markerOptions.snippet(item.snippet)
    }
}
