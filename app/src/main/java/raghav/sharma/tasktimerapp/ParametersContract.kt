package raghav.sharma.tasktimerapp

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns

object ParametersContract {
    const val ID_SHORT_TIMING = 1L

    internal const val TABLE_NAME = "Parameters"

    // uri to access the timings table
    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, TimingsContract.TABLE_NAME)

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.${TimingsContract.TABLE_NAME}"
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.${TimingsContract.TABLE_NAME}"

    object Columns {
        const val ID = BaseColumns._ID
        const val VALUE = "Value"
    }

    fun getId(uri: Uri): Long {
        return ContentUris.parseId(uri)
    }

    fun buildUriFromId(id: Long): Uri {
        return ContentUris.withAppendedId(TimingsContract.CONTENT_URI, id)
    }
}