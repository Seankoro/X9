package dk.itu.moapd.x9.s25134

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

// Single-Activity host for the fragment container
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val PREFS_NAME = "x9_prefs"
        const val KEY_DARK_MODE = "dark_mode"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        // needs to run before super so the theme is set before any views inflate
        applyStoredTheme()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        auth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "Auth state: signed in as ${user.email}")
            } else {
                Log.d(TAG, "Auth state: signed out")
            }
        }

        setContentView(R.layout.activity_main_host)

        // always start with Dashboard — unauthenticated users can browse
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()
        }
    }

    private fun applyStoredTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.contains(KEY_DARK_MODE)) {
            AppCompatDelegate.setDefaultNightMode(
                if (prefs.getBoolean(KEY_DARK_MODE, false))
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
        auth.addAuthStateListener(authStateListener)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")
        auth.removeAuthStateListener(authStateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }
}
