package dk.itu.moapd.x9.s25134.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.auth.GoogleAuthProvider
import dk.itu.moapd.x9.s25134.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val auth: FirebaseAuth = Firebase.auth

    private val _currentUser = MutableStateFlow<User?>(auth.currentUser?.toDomainUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Auth error messages are emitted here and collected in the UI via LaunchedEffect.
    // Using SharedFlow avoids passing Compose-scoped lambdas into the ViewModel.
    private val _authError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authError: SharedFlow<String> = _authError.asSharedFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser?.toDomainUser()
        Log.d(TAG, "Auth state changed: user=${firebaseAuth.currentUser?.uid}")
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun signInWithEmail(email: String, password: String) {
        _isLoading.value = true
        Log.d(TAG, "signInWithEmail: $email")
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                Log.d(TAG, "signInWithEmail success")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "signInWithEmail error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: "Authentication failed")
            } catch (e: Exception) {
                Log.e(TAG, "signInWithEmail unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: "Authentication failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerWithEmail(displayName: String, email: String, password: String) {
        _isLoading.value = true
        Log.d(TAG, "registerWithEmail: $email")
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                result.user?.updateProfile(profileUpdates)?.await()
                Log.d(TAG, "registerWithEmail success, displayName set to: $displayName")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "registerWithEmail error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: "Registration failed")
            } catch (e: Exception) {
                Log.e(TAG, "registerWithEmail unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: "Registration failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _isLoading.value = true
        Log.d(TAG, "signInWithGoogle")
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                Log.d(TAG, "signInWithGoogle success")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "signInWithGoogle error: ${e.errorCode}", e)
                _authError.tryEmit(e.localizedMessage ?: "Google sign-in failed")
            } catch (e: Exception) {
                Log.e(TAG, "signInWithGoogle unexpected error", e)
                _authError.tryEmit(e.localizedMessage ?: "Google sign-in failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut: ${auth.currentUser?.uid}")
        auth.signOut()
    }
}

private fun FirebaseUser.toDomainUser(): User = User(
    uid = uid,
    displayName = displayName ?: email?.substringBefore("@") ?: "",
    email = email ?: "",
    photoUrl = photoUrl?.toString() ?: ""
)
