package dk.itu.moapd.x9.s25134.speech

import dk.itu.moapd.x9.s25134.model.Severity

data class SpeechResult(
    val type: String?,
    val severity: Severity?
) {
    val isEmpty: Boolean get() = type == null && severity == null
}
