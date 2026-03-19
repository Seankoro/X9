package dk.itu.moapd.x9.s25134

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportListViewModel : ViewModel() {

    companion object {
        private const val TAG = "ReportListViewModel"
    }

    private val _reports = MutableLiveData<List<TrafficReport>>()
    val reports: LiveData<List<TrafficReport>> = _reports

    private val _authState = MutableStateFlow(FirebaseAuth.getInstance().currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _authState.value = auth.currentUser
    }

    private val ref: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("x9-reports/reports")

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = snapshot.children.mapNotNull { child ->
                child.getValue(TrafficReport::class.java)?.copy(id = child.key ?: "")
            }.sortedByDescending { it.timestamp }
            _reports.postValue(list)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "Database read cancelled: ${error.message}", error.toException())
        }
    }

    init {
        ref.addValueEventListener(listener)
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    fun addReport(report: TrafficReport) {
        val newRef = ref.push()
        val withId = report.copy(id = newRef.key ?: "")
        newRef.setValue(withId)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add report", e)
            }
    }

    fun updateReport(report: TrafficReport) {
        if (report.id.isBlank()) return
        ref.child(report.id).setValue(report)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update report ${report.id}", e)
            }
    }

    fun removeReport(report: TrafficReport) {
        if (report.id.isBlank()) return
        ref.child(report.id).removeValue()
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove report ${report.id}", e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        ref.removeEventListener(listener)
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }
}
