package dk.itu.moapd.x9.s25134

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

// Report form — sends the result back to MainActivity via setResult()
class ReportActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ReportActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_main)

        val spinnerType = findViewById<Spinner>(R.id.spinner_report_type)
        val editDescription = findViewById<EditText>(R.id.edit_text_description)
        val seekBarSeverity = findViewById<SeekBar>(R.id.seek_bar_severity)
        val textSeverityValue = findViewById<TextView>(R.id.text_severity_value)
        val buttonSubmit = findViewById<Button>(R.id.button_submit)
        val inputLayout = findViewById<TextInputLayout>(R.id.input_layout_description)

        // Sync the severity chip with the slider's initial position
        textSeverityValue.text = getString(R.string.severity_value, seekBarSeverity.progress)
        updateSeverityChipColor(textSeverityValue, seekBarSeverity.progress)

        // Update chip as the user drags
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
            if (!text.isNullOrEmpty()) {
                inputLayout.error = null
            }
        }

        // Confirm before discarding if the user has typed something
        onBackPressedDispatcher.addCallback(this) {
            if (editDescription.text.isNotEmpty()) {
                MaterialAlertDialogBuilder(this@ReportActivity)
                    .setTitle("Discard Report?")
                    .setMessage("You have unsaved changes. Are you sure you want to go back?")
                    .setNegativeButton("Keep Editing", null)
                    .setPositiveButton("Discard") { _, _ ->
                        isEnabled = false
                        finish()
                    }
                    .show()
            } else {
                finish()
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

            val resultIntent = Intent().apply {
                putExtra("EXTRA_REPORT", report)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun updateSeverityChipColor(chip: TextView, progress: Int) {
        val colorRes = when (progress) {
            1, 2 -> R.color.severity_low
            3    -> R.color.severity_medium
            else -> R.color.severity_high
        }
        chip.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
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

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }
}