package dk.itu.moapd.x9.s25134

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrafficReport(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val severity: Int = 0,
    val location: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Long = 0L
) : Parcelable