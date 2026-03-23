package dk.itu.moapd.x9.s25134.model

import java.util.UUID

// Define an ENUM to ensure strict data validation for severity levels
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
    val id: String = UUID.randomUUID().toString(), // UUID ensures uniqueness
    val timestamp: Long = System.currentTimeMillis()
)
