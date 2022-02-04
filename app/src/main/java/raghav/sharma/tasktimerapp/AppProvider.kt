package raghav.sharma.tasktimerapp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log

private const val TAG = "appProvider"
const val CONTENT_AUTHORITY = "raghav.sharma.tasktimerapp.provider"
private const val TASKS = 100
private const val TASKS_ID = 101

private const val TIMINGS = 200
private const val TIMINGS_ID = 201

private const val CURRENT_TIMING = 300

private const val TASK_DURATION = 400

private const val PARAMETERS = 500
private const val PARAMETERS_ID = 501

val CONTENT_AUTHORITY_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

class AppProvider: ContentProvider() {

    private val uriMatcher: UriMatcher by lazy { buildUriMatcher() }

    private fun buildUriMatcher(): UriMatcher{
        Log.d(TAG, "building UriMatcher")
        val matcher = UriMatcher(UriMatcher.NO_MATCH)

        matcher.addURI(CONTENT_AUTHORITY, TasksContract.TABLE_NAME, TASKS)
        matcher.addURI(CONTENT_AUTHORITY, "${TasksContract.TABLE_NAME}/#", TASKS_ID)

        matcher.addURI(CONTENT_AUTHORITY, TimingsContract.TABLE_NAME, TIMINGS)
        matcher.addURI(CONTENT_AUTHORITY, "${TimingsContract.TABLE_NAME}/#", TIMINGS_ID)

        matcher.addURI(CONTENT_AUTHORITY, CurrentTimingContract.VIEW_NAME, CURRENT_TIMING)

        matcher.addURI(CONTENT_AUTHORITY, DurationsContract.VIEW_NAME, TASK_DURATION)

        matcher.addURI(CONTENT_AUTHORITY, ParametersContract.TABLE_NAME, PARAMETERS)
        matcher.addURI(CONTENT_AUTHORITY, "${ParametersContract.TABLE_NAME}/#", PARAMETERS_ID)

        return matcher
    }
    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String {
        return when(uriMatcher.match(uri)){
            TASKS -> TasksContract.CONTENT_TYPE
            TASKS_ID -> TasksContract.CONTENT_ITEM_TYPE
            TIMINGS -> TimingsContract.CONTENT_TYPE
            TIMINGS_ID -> TimingsContract.CONTENT_ITEM_TYPE
            CURRENT_TIMING -> CurrentTimingContract.CONTENT_ITEM_TYPE
            TASK_DURATION -> DurationsContract.CONTENT_TYPE
            PARAMETERS -> ParametersContract.CONTENT_TYPE
            PARAMETERS_ID -> ParametersContract.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Unknown Uri: $uri")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        Log.d(TAG, "Provide query starts")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "match is $match")

        val queryBuilder = SQLiteQueryBuilder()

        when(match){
            TASKS -> queryBuilder.tables = TasksContract.TABLE_NAME
            TASKS_ID ->{
                queryBuilder.tables = TasksContract.TABLE_NAME
                val taskId = TasksContract.getId(uri)
                queryBuilder.appendWhere("${TasksContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$taskId")
            }
            TIMINGS -> queryBuilder.tables = TimingsContract.TABLE_NAME
            TIMINGS_ID ->{
                queryBuilder.tables = TasksContract.TABLE_NAME
                val timeId = TimingsContract.getId(uri)
                queryBuilder.appendWhere("${TimingsContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$timeId")
            }
            CURRENT_TIMING -> {
                queryBuilder.tables = CurrentTimingContract.VIEW_NAME
            }
            TASK_DURATION -> queryBuilder.tables = DurationsContract.VIEW_NAME

            PARAMETERS -> queryBuilder.tables = ParametersContract.TABLE_NAME
            PARAMETERS_ID ->{
                queryBuilder.tables = ParametersContract.TABLE_NAME
                val parameterId = ParametersContract.getId(uri)
                queryBuilder.appendWhere("${ParametersContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$parameterId")
            }

            else -> throw IllegalArgumentException("Unknown Uri: $uri")
        }
        val db = AppDatabase.getInstance(context!!).readableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        Log.d(TAG, "query: Rows in returned cursor = ${cursor.count}")
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        Log.d(TAG, "Insertion starts")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "match is $match")

        val recordId: Long
        val returnUri: Uri

        when(match){
            TASKS ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                recordId = db.insert(TasksContract.TABLE_NAME, null, values)
                if(recordId!= -1L) returnUri = TasksContract.buildUriFromId(recordId)
                else throw SQLiteException("Failed to insert in ${TasksContract.TABLE_NAME}: $uri")
            }
            TIMINGS ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                recordId = db.insert(TimingsContract.TABLE_NAME, null, values)
                if(recordId!= -1L) returnUri = TimingsContract.buildUriFromId(recordId)
                else throw SQLiteException("Failed to insert in ${TimingsContract.TABLE_NAME}: $uri")
            }
            else -> throw IllegalArgumentException("Unknown uri: $uri")
        }
        if(recordId>0){
            Log.d(TAG, "Insert: setting notify change with $uri")
            context?.contentResolver?.notifyChange(uri, null)
        }
        Log.d(TAG, "Exiting Insert with uri: $returnUri")
        return returnUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "deletion starts")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "match is $match")

        val deletedRows: Int
        var selectionCriteria: String
        when(match){
            TASKS ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                deletedRows = db.delete(TasksContract.TABLE_NAME, selection, selectionArgs)
            }
            TASKS_ID ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val rowId = TasksContract.getId(uri)
                selectionCriteria = "${TasksContract.Columns.ID} = $rowId"
                if(!selection.isNullOrEmpty()) selectionCriteria+=" AND ($selection)"
                deletedRows = db.delete(TasksContract.TABLE_NAME, selectionCriteria, selectionArgs)
            }
            TIMINGS ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                deletedRows = db.delete(TimingsContract.TABLE_NAME, selection, selectionArgs)
            }
            TIMINGS_ID->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val rowId = TimingsContract.getId(uri)
                selectionCriteria = "${TimingsContract.Columns.ID} = $rowId"
                if(!selection.isNullOrEmpty()) selectionCriteria += " AND ($selection)"
                deletedRows = db.delete(TimingsContract.TABLE_NAME, selectionCriteria, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unknown uri $uri")
        }
        if(deletedRows>0){
            Log.d(TAG, "Delete: setting notify change with $uri")
            context?.contentResolver?.notifyChange(uri, null)
        }
        Log.d(TAG, "Deleted $deletedRows rows.")
        return deletedRows
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        Log.d(TAG, "updating starts")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "match is $match")

        val updatedRows: Int
        var selectionCriteria: String
        when(match){
            TASKS ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                updatedRows = db.update(TasksContract.TABLE_NAME, values, selection, selectionArgs)
            }
            TASKS_ID ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val rowId = TasksContract.getId(uri)
                selectionCriteria = "${TasksContract.Columns.ID} = $rowId"
                if(!selection.isNullOrEmpty()) selectionCriteria+=" AND ($selection)"
                updatedRows = db.update(TasksContract.TABLE_NAME, values, selectionCriteria, selectionArgs)
            }
            TIMINGS ->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                updatedRows = db.update(TimingsContract.TABLE_NAME, values, selection, selectionArgs)
            }
            TIMINGS_ID->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val rowId = TimingsContract.getId(uri)
                selectionCriteria = "${TimingsContract.Columns.ID} = $rowId"
                if(!selection.isNullOrEmpty()) selectionCriteria += " AND ($selection)"
                updatedRows = db.update(TimingsContract.TABLE_NAME, values, selectionCriteria, selectionArgs)
            }
            PARAMETERS_ID->{
                val db = AppDatabase.getInstance(context!!).writableDatabase
                val rowId = ParametersContract.getId(uri)
                selectionCriteria = "${ParametersContract.Columns.ID} = $rowId"
                if(!selection.isNullOrEmpty()) selectionCriteria += " AND ($selection)"
                updatedRows = db.update(ParametersContract.TABLE_NAME, values, selectionCriteria, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unknown uri $uri")
        }
        if(updatedRows>0){
            Log.d(TAG, "Update: setting notify change with $uri")
            context?.contentResolver?.notifyChange(uri, null)
        }
        Log.d(TAG, "Updated $updatedRows rows.")
        return updatedRows
    }
}