package dk.itu.moapd.x9.s25134.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.speech.SpeechParser
import dk.itu.moapd.x9.s25134.speech.SpeechResult
import dk.itu.moapd.x9.s25134.speech.SpeechUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

// Manages the Android SpeechRecognizer lifecycle and bridges raw recognition events
// into domain types. Exposes two flows with different contracts:
//   - uiState (StateFlow): persistent overlay state, always has a value
//   - speechResult (SharedFlow): one-shot event emitted only on a successful parse
class SpeechViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SpeechViewModel"
    }

    private val _uiState = MutableStateFlow<SpeechUiState>(SpeechUiState.Idle)
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    private val _speechResult = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 1)
    val speechResult: SharedFlow<SpeechResult> = _speechResult.asSharedFlow()

    // Nullable because not all devices support speech recognition. Callers check
    // isAvailable() before showing UI that would trigger startListening().
    private var recognizer: SpeechRecognizer? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "raw matches: $matches")
            val utterance = matches?.firstOrNull().orEmpty()
            Log.d(TAG, "utterance -> '$utterance'")
            val result = SpeechParser.parse(utterance)
            Log.d(TAG, "parsed -> type=${result.type}, severity=${result.severity}")
            if (result.isEmpty) {
                _uiState.value = SpeechUiState.NoMatch
            } else {
                // Return to Idle before emitting so the overlay dismisses before
                // the form pre-fill navigates the user to the add screen.
                _uiState.value = SpeechUiState.Idle
                _speechResult.tryEmit(result)
            }
        }

        override fun onError(error: Int) {
            val context = getApplication<Application>()
            // ERROR_NO_MATCH means the engine heard speech but couldn't match it —
            // treated as a soft NoMatch rather than a hard Error so the user gets
            // a retry prompt instead of a failure message.
            if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                _uiState.value = SpeechUiState.NoMatch
                return
            }
            val message = when (error) {
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.error_speech_network)
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT  -> context.getString(R.string.error_speech_timeout)
                else                                    -> context.getString(R.string.error_speech_failed)
            }
            _uiState.value = SpeechUiState.Error(message)
        }

        // Empty implementations are required by RecognitionListener; none of these
        // events are relevant to the app's speech flow.
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                setRecognitionListener(recognitionListener)
            }
        }
    }

    /** Returns true if the device supports speech recognition. */
    fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(getApplication())

    /**
     * Starts listening. Call only from the main thread (Activity/Composable click handler).
     * Checks RECORD_AUDIO permission internally; emits Error state if not granted.
     */
    fun startListening() {
        val context = getApplication<Application>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _uiState.value = SpeechUiState.Error(
                context.getString(R.string.error_mic_permission)
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Hardcoded to English — SpeechParser's keyword list is English-only.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // Cancel any in-progress session before starting a new one, guarding
        // against the user tapping the mic button in rapid succession.
        recognizer?.cancel()
        recognizer?.startListening(intent)
        _uiState.value = SpeechUiState.Listening
    }

    /** Cancels an in-progress recognition session and returns to Idle. */
    fun cancel() {
        recognizer?.cancel()
        _uiState.value = SpeechUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        recognizer?.destroy()
        recognizer = null
    }
}
