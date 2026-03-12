package dk.itu.moapd.x9.s25134

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// In-memory report list, scoped to Activity — survives fragment swaps but not process death
class ReportListViewModel : ViewModel() {

    private val _reports = MutableLiveData<List<TrafficReport>>()
    val reports: LiveData<List<TrafficReport>> = _reports

    // Seed data for demo purposes
    init {
        _reports.value = listOf(
            TrafficReport("Accident", "Multi-car collision blocking two lanes on highway E45 near exit 12", 5),
            TrafficReport("Heavy Traffic", "Slow-moving traffic on Lyngbyvejen towards city centre", 3),
            TrafficReport("Speed Camera", "Mobile speed camera spotted near Nørreport station", 1),
            TrafficReport("Road Work", "Lane closure on Amager Strandvej due to road resurfacing", 4),
            TrafficReport("Heavy Traffic", "Congestion building on Hillerødmotorvejen after morning rush", 2),
            TrafficReport("Accident", "Minor fender-bender on Østerbrogade, right lane blocked", 3),
            TrafficReport("Road Work", "Utility maintenance causing delays on Vesterbrogade", 2),
            TrafficReport("Speed Camera", "Fixed speed camera active at Folehaven 60 km/h zone", 1)
        )
    }

    fun addReport(report: TrafficReport) {
        val current = _reports.value.orEmpty()
        _reports.value = listOf(report) + current
    }
}