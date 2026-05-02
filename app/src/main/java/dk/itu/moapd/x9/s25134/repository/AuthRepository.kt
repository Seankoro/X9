package dk.itu.moapd.x9.s25134.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import dk.itu.moapd.x9.s25134.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

// Repository layer to abstract the operations with Firebase Auth.
// Rest of application will interact with Firebase Auth through this repository
class AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    private val auth: FirebaseAuth = Firebase.auth

    // Repository owns the auth state listener and converts FirebaseUser to the domain User model.
    // The ViewModel reads currentUser directly from here instead of observing FirebaseAuth itself.
    private val _currentUser = MutableStateFlow(auth.currentUser?.toDomainUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser?.toDomainUser()
        Log.d(TAG, "Auth state changed: user=${firebaseAuth.currentUser?.uid}")
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun cleanup() {
        auth.removeAuthStateListener(authStateListener)
    }

    suspend fun signInWithEmail(email: String, password: String) {
        Log.d(TAG, "signInWithEmail: $email")
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun registerWithEmail(displayName: String, email: String, password: String) {
        Log.d(TAG, "registerWithEmail: $email")
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        result.user?.updateProfile(profileUpdates)?.await()
        Log.d(TAG, "registerWithEmail success, displayName=$displayName")
    }

    suspend fun signInWithGoogle(idToken: String) {
        Log.d(TAG, "signInWithGoogle")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        Log.d(TAG, "signOut: ${auth.currentUser?.uid}")
        auth.signOut()
    }
}

// Kept private to this file — FirebaseUser is a Firebase SDK type and must not leak
// into the domain model layer or ViewModel layer.
private fun FirebaseUser.toDomainUser(): User = User(
    uid = uid,
    displayName = displayName ?: email?.substringBefore("@") ?: "",
    email = email ?: "",
    photoUrl = photoUrl?.toString() ?: ""
)
