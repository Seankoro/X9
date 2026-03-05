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

class MainActivity : AppCompatActivity() {

    // Requirement: Use the android logging system
    private val TAG = "TrafficReportActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinnerType = findViewById<Spinner>(R.id.spinner_report_type)
        val editDescription = findViewById<EditText>(R.id.edit_text_description)
        val seekBarSeverity = findViewById<SeekBar>(R.id.seek_bar_severity)
        val buttonSubmit = findViewById<Button>(R.id.button_submit)
        val textOutput = findViewById<TextView>(R.id.text_view_output)
        val inputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.input_layout_description)

        buttonSubmit.setOnClickListener {
            // Read Values
            val type = spinnerType.selectedItem.toString()
            val description = editDescription.text.toString().trim()
            val severity = seekBarSeverity.progress

            // Requirement: Validate user input / Prevent empty submissions
            if (description.isEmpty()) {
                inputLayout.error = getString(R.string.error_empty_description)
                return@setOnClickListener
            } else {
                // Clear the error if input is valid
                inputLayout.error = null
            }

            // Requirement: Process the data (Stored in memory as an object)
            val report = TrafficReport(type, description, severity.toFloat())

            // Requirement: Use logging system to output summary
            Log.d(TAG, "--- Traffic Report Summary ---")
            Log.d(TAG, "Type: ${report.type}")
            Log.d(TAG, "Severity: ${report.severity.toInt()}")
            Log.d(TAG, "Description: ${report.description}")
            Log.d(TAG, "------------------------------")

            // Display result in UI using string resources with placeholders
            val summaryText = getString(R.string.summary_header) + "\n" +
                    getString(R.string.summary_type, report.type) + "\n" +
                    getString(R.string.summary_severity, report.severity.toInt()) + "\n" +
                    getString(R.string.summary_description, report.description)

            textOutput.text = summaryText

            // Success feedback
            Toast.makeText(this, R.string.report_submitted_toast, Toast.LENGTH_SHORT).show()

            // Clear input for next entry
            editDescription.text.clear()
            seekBarSeverity.progress = 0
        }
    }
}
