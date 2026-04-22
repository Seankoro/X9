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

class SpeechViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SpeechUiState>(SpeechUiState.Idle)
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    private val _speechResult = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 1)
    val speechResult: SharedFlow<SpeechResult> = _speechResult.asSharedFlow()

    private var recognizer: SpeechRecognizer? = null

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

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.startListening(intent)
        _uiState.value = SpeechUiState.Listening
    }

    /** Cancels an in-progress recognition session and returns to Idle. */
    fun cancel() {
        recognizer?.cancel()
        _uiState.value = SpeechUiState.Idle
    }

    override fun onCleared() {
        recognizer?.destroy()
        recognizer = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d("X9Speech", "raw matches: $matches")
            val utterance = matches?.firstOrNull().orEmpty()
            Log.d("X9Speech", "utterance -> '$utterance'")
            val result = SpeechParser.parse(utterance)
            Log.d("X9Speech", "parsed -> type=${result.type}, severity=${result.severity}")
            if (result.isEmpty) {
                _uiState.value = SpeechUiState.NoMatch
            } else {
                _uiState.value = SpeechUiState.Idle
                _speechResult.tryEmit(result)
            }
        }

        override fun onError(error: Int) {
            val context = getApplication<Application>()
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

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
