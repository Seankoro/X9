package dk.itu.moapd.x9.s25134.model

import android.util.Log
import com.google.firebase.database.DataSnapshot

private const val TAG = "TrafficReportSerializer"

/**
 * Converts a TrafficReport to a Firebase-compatible map.
 * Severity is stored as its Int level because Firebase cannot serialize Kotlin enums directly.
 */
fun TrafficReport.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "type" to type,
    "description" to description,
    "severity" to severity.level,
    "location" to location,
    "creatorId" to creatorId,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

/**
 * Reconstructs a TrafficReport object from a Firebase DataSnapshot.
 * Returns null if any required field is missing or the severity level is unrecognized
 *
 * Severity is read as Long (Firebase internal representation) then converted to Int.
 * Purely utility function, since firebase does not offer ORM, we need the serializer to convert
 * between what is stored in firebase snapshots and the actual report objects.
 */
fun DataSnapshot.toTrafficReport(): TrafficReport? {
    return try {
        val id = child("id").getValue(String::class.java) ?: return null
        val type = child("type").getValue(String::class.java) ?: return null
        val description = child("description").getValue(String::class.java) ?: return null
        val severityLevel = child("severity").getValue(Long::class.java)?.toInt() ?: return null
        val severity = Severity.entries.firstOrNull { it.level == severityLevel } ?: return null
        val location = child("location").getValue(String::class.java) ?: ""
        val creatorId = child("creatorId").getValue(String::class.java) ?: ""
        val createdAt = child("createdAt").getValue(Long::class.java) ?: 0L
        val updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L
        TrafficReport(
            id = id,
            type = type,
            description = description,
            severity = severity,
            location = location,
            creatorId = creatorId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to deserialize snapshot key=$key: ${e.message}", e)
        null
    }
}
