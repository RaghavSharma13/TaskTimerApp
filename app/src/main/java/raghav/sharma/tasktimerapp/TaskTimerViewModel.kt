package raghav.sharma.tasktimerapp

import android.app.Application
import android.content.ContentValues
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "TaskTimerViewModel"

class TaskTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "contentObserver.onChange: called, uri is $uri")
            loadTasks()
        }
    }

    private var currentTiming: Timing? = null

    private val timedTask = MutableLiveData<String>()
    val timingTask: LiveData<String>
        get() = timedTask

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor: LiveData<Cursor>
        get() = databaseCursor

    private val settings = PreferenceManager.getDefaultSharedPreferences(application)
    private var ignoreLessThan = settings.getInt(IGNORE_LESS_THAN, DEFAULT_IGNORE_LESS_THAN)
    private val settingsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                IGNORE_LESS_THAN -> {
                    ignoreLessThan = sharedPreferences.getInt(key, DEFAULT_IGNORE_LESS_THAN)
                    Log.d(TAG, "New ignoreLessThan value is $ignoreLessThan")

                    val values = ContentValues()
                    values.put(ParametersContract.Columns.VALUE, ignoreLessThan)
                    viewModelScope.launch(Dispatchers.IO) {
                        val uri = getApplication<Application>().contentResolver.update(
                            ParametersContract.buildUriFromId(ParametersContract.ID_SHORT_TIMING),
                            values,
                            null,
                            null
                        )
                    }
                }
            }
        }

    var editingTask = 0L
        private set


    init {
        Log.d(TAG, "Class Created")
        getApplication<Application>().contentResolver.registerContentObserver(
            TasksContract.CONTENT_URI,
            true,
            contentObserver
        )
        currentTiming = retrieveTiming()

        settings.registerOnSharedPreferenceChangeListener(settingsListener)

        loadTasks()
    }

    private fun loadTasks() {
        val projection = arrayOf(
            TasksContract.Columns.ID,
            TasksContract.Columns.TASK_NAME,
            TasksContract.Columns.TASK_DESCRIPTION,
            TasksContract.Columns.SORT_ORDER
        )
        val sortOrder = "${TasksContract.Columns.SORT_ORDER}, ${TasksContract.Columns.TASK_NAME} COLLATE NOCASE"

        viewModelScope.launch(Dispatchers.IO) {
            val cursor = getApplication<Application>().contentResolver.query(
                TasksContract.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
            databaseCursor.postValue(cursor)
        }
    }

    fun saveTask(task: Task): Task {
        val values = ContentValues()

        if (task.name.isNotEmpty()) {
            values.put(TasksContract.Columns.TASK_NAME, task.name)
            values.put(TasksContract.Columns.TASK_DESCRIPTION, task.description)
            values.put(TasksContract.Columns.SORT_ORDER, task.sortOrder)

            if (task.id == 0L) {
                viewModelScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "SaveTask: adding new Task")
                    val uri = getApplication<Application>().contentResolver?.insert(
                        TasksContract.CONTENT_URI,
                        values
                    )
                    if (uri != null) {
                        task.id = TasksContract.getId(uri)
                        Log.d(TAG, "saveTask new id is ${task.id}")
                    }
                }
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "SaveTask updating task")
                    getApplication<Application>().contentResolver?.update(
                        TasksContract.buildUriFromId(
                            task.id
                        ), values, null, null
                    )
                }
            }
        }
        return task
    }

    fun startEditing(taskId: Long) {
        if (BuildConfig.DEBUG && editingTask != 0L) throw IllegalStateException(" startEditing called without ending the previous task with id $taskId")
        editingTask = taskId
    }

    fun stopEditing() {
        editingTask = 0L
    }

    fun deleteTask(taskId: Long) {
        Log.d(TAG, "currentTimings id is ${currentTiming?.id} and taskId is $taskId")
        if (currentTiming?.taskId == taskId) {
            Log.d(TAG, "Deleting task being timed")
            currentTiming = null
            timedTask.value = null
        }
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().contentResolver?.delete(
                TasksContract.buildUriFromId(
                    taskId
                ), null, null
            )
        }
    }

    fun timeTask(task: Task) {
        Log.d(TAG, "timeTask called")
        val timingRecord = currentTiming
        if (timingRecord == null) {
            currentTiming = Timing(task.id)
            saveTiming(currentTiming!!)
        } else {
            timingRecord.setDuration()
            saveTiming(timingRecord)
            if (task.id == timingRecord.taskId) currentTiming =
                null // same task was clicked i.e, wanting to end
            else {
                // new Task was clicked
                currentTiming = Timing(task.id)
                saveTiming(currentTiming!!)
            }
        }
        timedTask.value = if (currentTiming != null) task.name else null
    }

    private fun saveTiming(timingRecord: Timing) {
        Log.d(TAG, "save Timing called")
        // this tells if we should insert a new row or update
        // updates only happens if the same task was started in a row
        // or a task being timed ends
        // because timingRecord updates if the new task is different from a previous task
        val inserting = (timingRecord.duration == 0L)

        val values = ContentValues().apply {
            if (inserting) {
                put(TimingsContract.Columns.TIMING_TASK_ID, timingRecord.taskId)
                put(TimingsContract.Columns.TIMING_START_TIME, timingRecord.startTime)
            }
            put(TimingsContract.Columns.TIMING_DURATION, timingRecord.duration)
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (inserting) {
                val uri = getApplication<Application>().contentResolver.insert(
                    TimingsContract.CONTENT_URI,
                    values
                )
                if (uri != null) timingRecord.id = TimingsContract.getId(uri)
            } else {
                Log.d(TAG, "updating a row with duration ${timingRecord.duration}")
                getApplication<Application>().contentResolver.update(
                    TimingsContract.buildUriFromId(timingRecord.id),
                    values,
                    null,
                    null
                )
            }
        }

    }

    private fun retrieveTiming(): Timing? {
        Log.d(TAG, "retrieveTiming called")
        val timing: Timing?
        val timeCursor: Cursor? = getApplication<Application>().contentResolver.query(
            CurrentTimingContract.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (timeCursor != null && timeCursor.moveToFirst()) {
            val id =
                timeCursor.getLong(timeCursor.getColumnIndex(CurrentTimingContract.Columns.TIMING_ID))
            val taskId =
                timeCursor.getLong(timeCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_ID))
            val startTime =
                timeCursor.getLong(timeCursor.getColumnIndex(CurrentTimingContract.Columns.START_TIME))
            val taskName =
                timeCursor.getString(timeCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_NAME))
            timing = Timing(taskId, startTime, id)
            timedTask.value = taskName
        } else {
            timing = null
        }
        timeCursor?.close()
        Log.d(TAG, "retrieveTiming ends")
        return timing
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
        settings.unregisterOnSharedPreferenceChangeListener(settingsListener)
        databaseCursor.value?.close()
    }
}