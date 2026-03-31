package dk.itu.moapd.x9.s25134.model

import java.util.UUID
import java.time.Instant

enum class Severity(val level: Int) {
    MINOR(1),
    LOW(2),
    MODERATE(3),
    HIGH(4),
    CRITICAL(5)
}

data class TrafficReport(
    val type: String,
    val description: String,
    val severity: Severity,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val id: String = UUID.randomUUID().toString(),
    val creatorId: String = "", // Firebase user ID of the creator of traffic report
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)
