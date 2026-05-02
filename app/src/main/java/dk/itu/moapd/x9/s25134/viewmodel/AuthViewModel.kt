package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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

// Handles sign-in (email and Google), registration, and sign-out.
// Extends AndroidViewModel for access to Application.getString() — needed to resolve
// string resources from coroutine callbacks outside the Compose tree.
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val repository = AuthRepository()

    // Auth state is owned by the repository (Firebase listener lives there).
    // The ViewModel exposes it directly rather than copying into a separate StateFlow.
    val currentUser: StateFlow<User?> = repository.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Auth error messages are one-shot events — SharedFlow is used instead of StateFlow
    // so the UI doesn't re-show an old error on recomposition.
    // extraBufferCapacity = 1 ensures tryEmit never silently drops an error if the
    // collector is momentarily suspended.
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

    // context must be an Activity context — CredentialManager.getCredential() needs to
    // anchor the system bottom sheet to a running Activity, not the Application.
    fun signInWithGoogleCredential(context: Context) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // false = show all Google accounts on the device, not just previously
                    // authorized ones. Required for first-time sign-in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context = context, request = request)
                val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                repository.signInWithGoogle(idToken)
                Log.d(TAG, "signInWithGoogleCredential success")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "signInWithGoogleCredential credential error", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_google_sign_in_failed))
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "signInWithGoogleCredential firebase error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_google_sign_in_failed))
            } catch (e: Exception) {
                Log.e(TAG, "signInWithGoogleCredential unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: getString(R.string.error_google_sign_in_failed))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Firebase signOut() is synchronous — no coroutine or loading state needed.
    fun signOut() {
        repository.signOut()
    }

    // Convenience wrapper so callers don't need to repeat getApplication<Application>().
    private fun getString(@StringRes id: Int): String = getApplication<Application>().getString(id)
}
