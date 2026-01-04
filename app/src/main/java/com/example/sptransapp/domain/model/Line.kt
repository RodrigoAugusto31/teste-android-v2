package com.example.sptransapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Line(
    val lineCode: Int,
    val fullSign: String,
    val name: String,
    val direction: Int,
) : Parcelable {
    override fun toString(): String = "$fullSign - $name"
}
