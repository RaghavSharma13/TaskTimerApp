package raghav.sharma.tasktimerapp

import android.app.Application
import android.content.*
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
import java.util.*

enum class SortColumns{
    NAME,
    DESCRIPTION,
    START_DATE,
    DURATION
}

private const val TAG = "DurationsViewModel"

class DurationsViewModel(application: Application): AndroidViewModel(application) {

    private val contentObserver = object : ContentObserver(Handler()){
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "contentObserver.onChange: called, uri is $uri")
            loadData()
        }
    }

    private val broadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "broadcastReceiver called with intent $intent")
            val action = intent?.action
            if(action == Intent.ACTION_TIMEZONE_CHANGED || action == Intent.ACTION_LOCALE_CHANGED){
                val currentTime = calendar.timeInMillis
                calendar = GregorianCalendar()
                calendar.timeInMillis = currentTime
                _firstDayOfWeek = settings.getInt(FIRST_DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.firstDayOfWeek = firstDayOfWeek
                applyFilter()
            }
        }
    }

    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener {sharedPrefs, key ->
        when(key){
            FIRST_DAY_OF_WEEK -> {
                _firstDayOfWeek = sharedPrefs.getInt(key, calendar.firstDayOfWeek)
                calendar.firstDayOfWeek = firstDayOfWeek
                Log.d(TAG, "firstDayOfWeek is now $firstDayOfWeek")
                // re-querying data
                applyFilter()
            }
        }
    }

    private var calendar = GregorianCalendar()
    private val settings = PreferenceManager.getDefaultSharedPreferences(application)
    private var _firstDayOfWeek = settings.getInt(FIRST_DAY_OF_WEEK, calendar.firstDayOfWeek)
    private var databaseCursor = MutableLiveData<Cursor>()
    private val selection = "${DurationsContract.Columns.START_DATE} Between ? AND ?"
    private var selectionArgs = emptyArray<String>()
    private var _displayWeek: Boolean = true

    val firstDayOfWeek: Int
    get() = _firstDayOfWeek

    val cursor: LiveData<Cursor>
    get() = databaseCursor

    var sortOrder = SortColumns.NAME
    set(order){
        if(field!=order){
            field = order
            loadData()
        }
    }

    val displayWeek: Boolean
        get() = _displayWeek

    init{
        calendar.firstDayOfWeek = firstDayOfWeek

        application.contentResolver.registerContentObserver(TimingsContract.CONTENT_URI, true, contentObserver)
        application.contentResolver.registerContentObserver(ParametersContract.CONTENT_URI, true, contentObserver)

        val broadcastFilter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        broadcastFilter.addAction(Intent.ACTION_LOCALE_CHANGED)
        application.registerReceiver(broadcastReceiver, broadcastFilter)

        settings.registerOnSharedPreferenceChangeListener(settingsListener)
        applyFilter()
    }

    fun getFilterDate(): Date{
        return calendar.time
    }

    fun setReportDate(year: Int, month: Int, dayOfMonth: Int){
        if(calendar.get(GregorianCalendar.YEAR)!= year || calendar.get(GregorianCalendar.MONTH)!= month || calendar.get(GregorianCalendar.DAY_OF_MONTH)!= dayOfMonth){
            calendar.set(year, month, dayOfMonth, 0, 0, 0)
            applyFilter()
        }
    }

    fun toggleDisplayWeek(){
        _displayWeek = !_displayWeek
        applyFilter()
    }

    private fun applyFilter(){
        Log.d(TAG, "entering applyFilter")

        val currentCalendarDate = calendar.timeInMillis

        if(displayWeek){
            val weekStart = calendar.firstDayOfWeek
            Log.d(TAG, "applyFilter: first day of calendar week is $weekStart")
            Log.d(TAG, "applyFilter: day of week is ${calendar.get(GregorianCalendar.DAY_OF_WEEK)}")
            Log.d(TAG, "applyFilter: date is ${calendar.time}")

            calendar.set(GregorianCalendar.DAY_OF_WEEK, weekStart)
            calendar.set(GregorianCalendar.HOUR_OF_DAY, 0)
            calendar.set(GregorianCalendar.MINUTE, 0)
            calendar.set(GregorianCalendar.SECOND, 0)
            val startDate = calendar.timeInMillis/1000

            calendar.add(GregorianCalendar.DATE, 6)
            calendar.set(GregorianCalendar.HOUR_OF_DAY, 23)
            calendar.set(GregorianCalendar.MINUTE, 59)
            calendar.set(GregorianCalendar.SECOND, 59)
            val endDate = calendar.timeInMillis/1000

            selectionArgs = arrayOf(startDate.toString(), endDate.toString())
            Log.d(TAG, "inApplyingFilter(7) Start Date is $startDate and end date is $endDate")
        }else{
            calendar.set(GregorianCalendar.HOUR_OF_DAY, 0)
            calendar.set(GregorianCalendar.MINUTE, 0)
            calendar.set(GregorianCalendar.SECOND, 0)
            val startDate = calendar.timeInMillis/1000

            calendar.set(GregorianCalendar.HOUR_OF_DAY, 23)
            calendar.set(GregorianCalendar.MINUTE, 59)
            calendar.set(GregorianCalendar.SECOND, 59)
            val endDate = calendar.timeInMillis/1000

            selectionArgs = arrayOf(startDate.toString(), endDate.toString())
            Log.d(TAG, "inApplyingFilter(1) Start Date is $startDate and end date is $endDate")
        }

        calendar.timeInMillis = currentCalendarDate
        loadData()
    }

    private fun loadData(){
        val order = when(sortOrder){
            SortColumns.NAME -> DurationsContract.Columns.NAME
            SortColumns.DESCRIPTION -> DurationsContract.Columns.DESCRIPTION
            SortColumns.START_DATE -> DurationsContract.Columns.START_TIME
            SortColumns.DURATION -> DurationsContract.Columns.DURATION
        }
        Log.d(TAG, "order is $order")
        viewModelScope.launch(Dispatchers.IO) {
            val cursor = getApplication<Application>().contentResolver.query(
                DurationsContract.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                order
            )
            databaseCursor.postValue(cursor)
        }
    }

    fun deleteRecords(timeInMilli: Long){
        Log.d(TAG, "Entering deleteRecords")
        val longDate = timeInMilli/1000
        val selection = "${TimingsContract.Columns.TIMING_START_TIME} < ?"
        val selectionArgs = arrayOf(longDate.toString())
        Log.d(TAG, "Deleting Records prior to $longDate")
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().contentResolver.delete(TimingsContract.CONTENT_URI, selection, selectionArgs)
        }
        Log.d(TAG, "Exiting DeleteRecords")
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
        getApplication<Application>().unregisterReceiver(broadcastReceiver)
        settings.unregisterOnSharedPreferenceChangeListener(settingsListener)
        databaseCursor.value?.close()
    }
}