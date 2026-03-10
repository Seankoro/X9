package dk.itu.moapd.x9.s25134

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

class TrafficReportAdapter :
    ListAdapter<TrafficReport, TrafficReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_traffic_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textType: TextView = itemView.findViewById(R.id.text_report_type)
        private val textDescription: TextView = itemView.findViewById(R.id.text_report_description)
        private val chipSeverity: Chip = itemView.findViewById(R.id.chip_severity)

        fun bind(report: TrafficReport) {
            textType.text = report.type
            textDescription.text = report.description

            val (label, colorRes) = when {
                report.severity <= 2 -> "Low" to R.color.severity_low
                report.severity <= 3 -> "Medium" to R.color.severity_medium
                else -> "High" to R.color.severity_high
            }
            chipSeverity.text = "$label (${report.severity}/5)"
            chipSeverity.setChipBackgroundColorResource(colorRes)
            chipSeverity.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.waze_surface_on)
            )
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<TrafficReport>() {
        override fun areItemsTheSame(oldItem: TrafficReport, newItem: TrafficReport): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: TrafficReport, newItem: TrafficReport): Boolean {
            return oldItem == newItem
        }
    }
}