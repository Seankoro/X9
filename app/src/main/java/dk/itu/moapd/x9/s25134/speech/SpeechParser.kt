package dk.itu.moapd.x9.s25134.speech

import dk.itu.moapd.x9.s25134.model.Severity
import java.util.Locale

object SpeechParser {

    // Ordered so multi-word keywords appear before their single-word subsets.
    // Within each mapping group, order does not matter — leftmost-in-utterance wins.
    private val typeKeywords: List<Pair<String, String>> = listOf(
        "speed camera" to "Speed Camera",
        "road works"   to "Road Work",
        "road work"    to "Road Work",
        "congestion"   to "Heavy Traffic",
        "collision"    to "Accident",
        "construction" to "Road Work",
        "accident"     to "Accident",
        "traffic"      to "Heavy Traffic",
        "camera"       to "Speed Camera",
        "crash"        to "Accident",
        "works"        to "Road Work",
        "jam"          to "Heavy Traffic"
    )

    private val severityKeywords: List<Pair<String, Severity>> = listOf(
        "critical" to Severity.CRITICAL,
        "severe"   to Severity.CRITICAL,
        "danger"   to Severity.CRITICAL,
        "serious"  to Severity.HIGH,
        "moderate" to Severity.MODERATE,
        "medium"   to Severity.MODERATE,
        "minor"    to Severity.MINOR,
        "high"     to Severity.HIGH,
        "low"      to Severity.LOW
    )

    fun parse(utterance: String): SpeechResult {
        val lower = utterance.lowercase(Locale.ENGLISH)

        val type = typeKeywords
            .filter { (keyword, _) -> lower.contains(keyword) }
            .minByOrNull { (keyword, _) -> lower.indexOf(keyword) }
            ?.second

        val severity = severityKeywords
            .filter { (keyword, _) -> lower.contains(keyword) }
            .minByOrNull { (keyword, _) -> lower.indexOf(keyword) }
            ?.second

        return SpeechResult(type = type, severity = severity)
    }
}
