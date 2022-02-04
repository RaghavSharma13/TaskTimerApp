package raghav.sharma.tasktimerapp

import android.net.Uri

object CurrentTimingContract {
    internal const val VIEW_NAME = "vwCurrentTiming"

    // uri to access the timings table
    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, VIEW_NAME)

    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$VIEW_NAME"

    object Columns {
        const val TIMING_ID = TimingsContract.Columns.ID
        const val TASK_ID = TimingsContract.Columns.TIMING_TASK_ID
        const val START_TIME = TimingsContract.Columns.TIMING_START_TIME
        const val TASK_NAME = TasksContract.Columns.TASK_NAME
    }
}