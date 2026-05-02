package dk.itu.moapd.x9.s25134.speech

// Sealed class to define the different states of the Speech recognition workflow
sealed class SpeechUiState {
    // Speech recognition is inactive, speech overlay is not shown
    data object Idle : SpeechUiState()

    // Speech recognition is active and picking up user's speech input
    data object Listening : SpeechUiState()

    // Speech recognition failed, users can retry if they want to
    data object NoMatch : SpeechUiState()

    // Speech recognition failed due to an error
    data class Error(val message: String) : SpeechUiState()
}
