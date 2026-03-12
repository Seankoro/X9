package dk.itu.moapd.x9.s25134

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

// New-report form — sends result back via Fragment Result API
class ReportFragment : Fragment() {

    companion object {
        private const val TAG = "ReportFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called")

        val spinnerType = view.findViewById<Spinner>(R.id.spinner_report_type)
        val editDescription = view.findViewById<EditText>(R.id.edit_text_description)
        val seekBarSeverity = view.findViewById<SeekBar>(R.id.seek_bar_severity)
        val textSeverityValue = view.findViewById<TextView>(R.id.text_severity_value)
        val buttonSubmit = view.findViewById<Button>(R.id.button_submit)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.input_layout_description)

        // Sync the chip with the slider's initial value
        textSeverityValue.text = getString(R.string.severity_value, seekBarSeverity.progress)
        updateSeverityChipColor(textSeverityValue, seekBarSeverity.progress)

        seekBarSeverity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textSeverityValue.text = getString(R.string.severity_value, progress)
                updateSeverityChipColor(textSeverityValue, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Clear validation error once the user starts typing
        editDescription.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) inputLayout.error = null
        }

        // Confirm before discarding if the user has typed something
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (editDescription.text.isNotEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Discard Report?")
                    .setMessage("You have unsaved changes. Are you sure you want to go back?")
                    .setNegativeButton("Keep Editing", null)
                    .setPositiveButton("Discard") { _, _ ->
                        parentFragmentManager.popBackStack()
                    }
                    .show()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        buttonSubmit.setOnClickListener {
            val type = spinnerType.selectedItem.toString()
            val description = editDescription.text.toString().trim()
            val severity = seekBarSeverity.progress

            if (description.isEmpty()) {
                inputLayout.error = getString(R.string.error_empty_description)
                Log.w(TAG, "Submission blocked — description is empty")
                return@setOnClickListener
            }

            val report = TrafficReport(type, description, severity)
            Log.d(TAG, "Report submitted — Type: $type | Severity: $severity/5 | Description: $description")

            // Send report back to DashboardFragment and pop back
            parentFragmentManager.setFragmentResult(
                "report_result",
                bundleOf("report" to report)
            )
            parentFragmentManager.popBackStack()
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

    private fun updateSeverityChipColor(chip: TextView, progress: Int) {
        val colorRes = when (progress) {
            1, 2 -> R.color.severity_low
            3    -> R.color.severity_medium
            else -> R.color.severity_high
        }
        chip.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
    }
}
