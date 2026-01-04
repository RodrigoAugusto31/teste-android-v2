package com.example.sptransapp.presentation.ui.common

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

object MarkerAnimator {
    fun animateMarkerToGB(
        marker: Marker,
        finalPosition: LatLng,
        latLngInterpolator: LatLngInterpolator,
    ) {
        val startPosition = marker.position
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 2000
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { animation ->
            try {
                val v = animation.animatedFraction
                val newPosition = latLngInterpolator.interpolate(v, startPosition, finalPosition)
                marker.position = newPosition
            } catch (_: Exception) {
            }
        }
        valueAnimator.start()
    }
}

interface LatLngInterpolator {
    fun interpolate(
        fraction: Float,
        a: LatLng,
        b: LatLng,
    ): LatLng

    class Linear : LatLngInterpolator {
        override fun interpolate(
            fraction: Float,
            a: LatLng,
            b: LatLng,
        ): LatLng {
            val lat = (b.latitude - a.latitude) * fraction + a.latitude
            val lng = (b.longitude - a.longitude) * fraction + a.longitude
            return LatLng(lat, lng)
        }
    }
}
