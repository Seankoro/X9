package dk.itu.moapd.x9.s25134

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// V1 — single-screen form for submitting traffic reports (in-memory only, nothing persisted yet)
class MainActivity : AppCompatActivity() {

    private val TAG = "TrafficReportActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Form inputs
        val spinnerType = findViewById<Spinner>(R.id.spinner_report_type)
        val editDescription = findViewById<EditText>(R.id.edit_text_description)
        val seekBarSeverity = findViewById<SeekBar>(R.id.seek_bar_severity)
        val buttonSubmit = findViewById<Button>(R.id.button_submit)
        val textOutput = findViewById<TextView>(R.id.text_view_output)
        val inputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.input_layout_description)

        buttonSubmit.setOnClickListener {
            val type = spinnerType.selectedItem.toString()
            val description = editDescription.text.toString().trim()
            val severity = seekBarSeverity.progress

            // Don't allow empty descriptions
            if (description.isEmpty()) {
                inputLayout.error = getString(R.string.error_empty_description)
                return@setOnClickListener
            } else {
                inputLayout.error = null
            }

            val report = TrafficReport(type, description, severity)

            // Log report to Logcat for debugging
            Log.d(TAG, "--- Traffic Report Summary ---")
            Log.d(TAG, "Type: ${report.type}")
            Log.d(TAG, "Severity: ${report.severity}")
            Log.d(TAG, "Description: ${report.description}")
            Log.d(TAG, "------------------------------")

            // Show the submitted report on screen
            val summaryText = getString(R.string.summary_header) + "\n" +
                    getString(R.string.summary_type, report.type) + "\n" +
                    getString(R.string.summary_severity, report.severity) + "\n" +
                    getString(R.string.summary_description, report.description)

            textOutput.text = summaryText

            Toast.makeText(this, R.string.report_submitted_toast, Toast.LENGTH_SHORT).show()

            // Reset form for the next report
            editDescription.text.clear()
            seekBarSeverity.progress = 0
        }
    }
}
