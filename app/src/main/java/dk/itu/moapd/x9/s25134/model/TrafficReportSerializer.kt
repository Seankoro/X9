package dk.itu.moapd.x9.s25134.model

import android.util.Log
import com.google.firebase.database.DataSnapshot

private const val TAG = "TrafficReportSerializer"

fun TrafficReport.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "type" to type,
    "description" to description,
    "severity" to severity.level,
    "latitude" to latitude,
    "longitude" to longitude,
    "locationName" to locationName,
    "creatorId" to creatorId,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "imageUrl" to imageUrl
)

fun DataSnapshot.toTrafficReport(): TrafficReport? {
    return try {
        val id = child("id").getValue(String::class.java) ?: return null
        val type = child("type").getValue(String::class.java) ?: return null
        val description = child("description").getValue(String::class.java) ?: return null
        val severityLevel = child("severity").getValue(Long::class.java)?.toInt() ?: return null
        val severity = Severity.entries.firstOrNull { it.level == severityLevel } ?: return null
        // Firebase stores fractional Doubles correctly, but rounds whole numbers to Long
        // (e.g. 55.0 becomes 55). Reading as Any and casting via Number handles both cases.
        val latitude = child("latitude").getValue(Any::class.java)?.let { (it as? Number)?.toDouble() }
        val longitude = child("longitude").getValue(Any::class.java)?.let { (it as? Number)?.toDouble() }
        val locationName = child("locationName").getValue(String::class.java) ?: ""
        val creatorId = child("creatorId").getValue(String::class.java) ?: ""
        val createdAt = child("createdAt").getValue(Long::class.java) ?: 0L
        val updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L
        val imageUrl = child("imageUrl").getValue(String::class.java)
        TrafficReport(
            id = id,
            type = type,
            description = description,
            severity = severity,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            creatorId = creatorId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            imageUrl = imageUrl
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to deserialize snapshot key=$key: ${e.message}", e)
        null
    }
}
