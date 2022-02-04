package raghav.sharma.tasktimerapp

import android.net.Uri

object DurationsContract {

    internal const val VIEW_NAME = "vwTaskDurations"

    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, VIEW_NAME)
    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$VIEW_NAME"

    object Columns{
        const val NAME = TasksContract.Columns.TASK_NAME
        const val DESCRIPTION = TasksContract.Columns.TASK_DESCRIPTION
        const val START_TIME = TimingsContract.Columns.TIMING_START_TIME
        const val START_DATE = "StartDate"
        const val DURATION = TimingsContract.Columns.TIMING_DURATION
    }
}