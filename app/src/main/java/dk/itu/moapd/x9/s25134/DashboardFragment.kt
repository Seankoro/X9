package dk.itu.moapd.x9.s25134

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Dashboard — shows the latest submitted report, a button to file a new one,
 * and a dark mode toggle.
 */
class DashboardFragment : Fragment() {

    companion object {
        private const val TAG = "DashboardFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        // Listen for reports coming back from ReportFragment
        parentFragmentManager.setFragmentResultListener("report_result", this) { _, bundle ->
            val report = BundleCompat.getParcelable(bundle, "report", TrafficReport::class.java)
            report?.let {
                val summary = getString(R.string.summary_header) + "\n" +
                        getString(R.string.summary_type, it.type) + "\n" +
                        getString(R.string.summary_severity, it.severity) + "\n" +
                        getString(R.string.summary_description, it.description)

                view?.findViewById<TextView>(R.id.text_view_main_output)?.text = summary
                Log.d(TAG, "Report received from ReportFragment: $it")

                view?.let { root ->
                    Snackbar.make(root, R.string.report_submitted_toast, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        return inflater.inflate(R.layout.activity_main_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called")

        val btnOpenReporter = view.findViewById<Button>(R.id.button_open_reporter)
        val btnToggleDark = view.findViewById<ImageButton>(R.id.button_toggle_dark_mode)

        btnOpenReporter.setOnClickListener {
            Log.d(TAG, "Opening ReportFragment")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ReportFragment())
                .addToBackStack("report")
                .commit()
        }

        btnToggleDark.setOnClickListener {
            val currentlyDark = isCurrentlyInDarkMode()
            val newDark = !currentlyDark

            requireActivity()
                .getSharedPreferences(MainActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean(MainActivity.KEY_DARK_MODE, newDark)
                .apply()

            Log.d(TAG, "Dark mode toggled — dark=$newDark (was $currentlyDark)")

            AppCompatDelegate.setDefaultNightMode(
                if (newDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }

    private fun isCurrentlyInDarkMode(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO  -> false
            else -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
