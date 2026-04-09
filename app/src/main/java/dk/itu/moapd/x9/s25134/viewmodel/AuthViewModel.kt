package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val repository = AuthRepository()

    // Auth state is owned by the repository; ViewModel exposes it directly.
    val currentUser: StateFlow<User?> = repository.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Auth error messages are emitted here and collected in the UI via LaunchedEffect.
    // Using SharedFlow avoids passing Compose-scoped lambdas into the ViewModel.
    private val _authError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authError: SharedFlow<String> = _authError.asSharedFlow()

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }

    fun signInWithEmail(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.signInWithEmail(email, password)
                Log.d(TAG, "signInWithEmail success")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "signInWithEmail error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_auth_generic))
            } catch (e: Exception) {
                Log.e(TAG, "signInWithEmail unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_auth_generic))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerWithEmail(displayName: String, email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.registerWithEmail(displayName, email, password)
                Log.d(TAG, "registerWithEmail success")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "registerWithEmail error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_auth_generic))
            } catch (e: Exception) {
                Log.e(TAG, "registerWithEmail unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_auth_generic))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.signInWithGoogle(idToken)
                Log.d(TAG, "signInWithGoogle success")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "signInWithGoogle error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_google_sign_in_failed))
            } catch (e: Exception) {
                Log.e(TAG, "signInWithGoogle unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_google_sign_in_failed))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        repository.signOut()
    }

    private fun getString(@StringRes id: Int): String = getApplication<Application>().getString(id)
}
