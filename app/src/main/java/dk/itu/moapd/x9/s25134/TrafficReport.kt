package dk.itu.moapd.x9.s25134

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrafficReport(
    val type: String,
    val description: String,
    val severity: Int
) : Parcelable