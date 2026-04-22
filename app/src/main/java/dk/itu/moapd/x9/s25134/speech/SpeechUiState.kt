package dk.itu.moapd.x9.s25134.speech

sealed class SpeechUiState {
    data object Idle : SpeechUiState()
    data object Listening : SpeechUiState()
    data object NoMatch : SpeechUiState()
    data class Error(val message: String) : SpeechUiState()
}
