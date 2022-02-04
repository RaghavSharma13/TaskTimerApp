package raghav.sharma.tasktimerapp

import android.util.Log
import java.util.*

private const val TAG = "Timing"

class Timing(val taskId: Long, val startTime: Long = Date().time / 1000, var id: Long = 0) {
    var duration: Long = 0L
    private set

    fun setDuration(){
        // calculate duration from startTime to endTime
        duration = Date().time / 1000 - startTime
        Log.d(TAG, "$taskId start time is : $startTime and duration is $duration")
    }
}