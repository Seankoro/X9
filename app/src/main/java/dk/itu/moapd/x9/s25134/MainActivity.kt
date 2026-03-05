package dk.itu.moapd.x9.s25134

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.IntentCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "x9_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    private lateinit var textOutput: TextView

    private val reportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val report = result.data?.let { intent ->
                IntentCompat.getParcelableExtra(intent, "EXTRA_REPORT", TrafficReport::class.java)
            }

            report?.let {
                val summary = getString(R.string.summary_header) + "\n" +
                        getString(R.string.summary_type, it.type) + "\n" +
                        getString(R.string.summary_severity, it.severity.toInt()) + "\n" +
                        getString(R.string.summary_description, it.description)

                textOutput.text = summary
                Log.d(TAG, "Report received from ReportActivity: $it")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply stored theme preference before layout is inflated
        applyStoredTheme()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_main_dashboard)

        textOutput = findViewById(R.id.text_view_main_output)
        val btnOpenReporter = findViewById<Button>(R.id.button_open_reporter)
        val btnToggleDark = findViewById<ImageButton>(R.id.button_toggle_dark_mode)

        btnOpenReporter.setOnClickListener {
            Log.d(TAG, "Launching ReportActivity via explicit Intent")
            val intent = Intent(this, ReportActivity::class.java)
            reportLauncher.launch(intent)
        }

        btnToggleDark.setOnClickListener {
            val currentlyDark = isCurrentlyInDarkMode()
            val newDark = !currentlyDark

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_DARK_MODE, newDark)
                .apply()

            Log.d(TAG, "Dark mode toggled — dark=$newDark (was $currentlyDark)")

            AppCompatDelegate.setDefaultNightMode(
                if (newDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    /** Reads the stored dark-mode preference and applies it via AppCompatDelegate. */
    private fun applyStoredTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.contains(KEY_DARK_MODE)) {
            val mode = if (prefs.getBoolean(KEY_DARK_MODE, false))
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        // If no preference is stored yet, leave the system default untouched
    }

    /** Returns true if the activity is currently rendered in dark mode. */
    private fun isCurrentlyInDarkMode(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO  -> false
            else -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called — Activity is visible to the user")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called — Activity is in the foreground and interactive")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called — Activity is partially obscured (e.g. another Activity launching)")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called — Activity is no longer visible")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart() called — Activity is returning from stopped state")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called — Activity is being destroyed (rotation or finish)")
    }
}