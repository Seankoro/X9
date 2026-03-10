package dk.itu.moapd.x9.s25134

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class DashboardFragment : Fragment() {

    companion object {
        private const val TAG = "DashboardFragment"
        private const val KEY_LAYOUT_STATE = "recycler_layout_state"
    }

    private lateinit var viewModel: ReportListViewModel
    private lateinit var adapter: TrafficReportAdapter
    private var pendingLayoutState: Parcelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        viewModel = ViewModelProvider(requireActivity())[ReportListViewModel::class.java]

        // Register to receive the completed report from ReportFragment.
        parentFragmentManager.setFragmentResultListener("report_result", this) { _, bundle ->
            val report = BundleCompat.getParcelable(bundle, "report", TrafficReport::class.java)
            report?.let {
                viewModel.addReport(it)
                Log.d(TAG, "Report received from ReportFragment: $it")

                view?.let { root ->
                    Snackbar.make(root, R.string.report_submitted_toast, Snackbar.LENGTH_LONG).show()
                    root.findViewById<RecyclerView>(R.id.recycler_reports)
                        ?.smoothScrollToPosition(0)
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
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_reports)

        // Set up RecyclerView
        adapter = TrafficReportAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        // Restore scroll position after a configuration change
        savedInstanceState?.let {
            pendingLayoutState = BundleCompat.getParcelable(
                it, KEY_LAYOUT_STATE, Parcelable::class.java
            )
        }

        // Observe LiveData — lifecycle-aware, updates list automatically
        viewModel.reports.observe(viewLifecycleOwner) { reports ->
            adapter.submitList(reports) {
                // Restore scroll position once the list is laid out
                pendingLayoutState?.let { state ->
                    layoutManager.onRestoreInstanceState(state)
                    pendingLayoutState = null
                }
            }
        }

        // Navigate to ReportFragment via a Fragment transaction, adding to back stack
        btnOpenReporter.setOnClickListener {
            Log.d(TAG, "Navigating to ReportFragment via Fragment transaction")
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save RecyclerView scroll position
        view?.findViewById<RecyclerView>(R.id.recycler_reports)?.layoutManager?.let {
            outState.putParcelable(KEY_LAYOUT_STATE, it.onSaveInstanceState())
        }
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
