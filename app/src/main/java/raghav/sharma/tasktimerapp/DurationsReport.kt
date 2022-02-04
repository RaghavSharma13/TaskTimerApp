package raghav.sharma.tasktimerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.task_durations.*
import java.util.*

private const val TAG = "DurationsReport"

private const val DIALOG_FILTER = 1
private const val DIALOG_DELETE = 2
private const val DELETION_DATE = "DELETION_DATE"

class DurationsReport : AppCompatActivity(), DatePickerDialog.OnDateSetListener, DialogFragment.DialogEvents {

    private val reportAdapter by lazy { DurationRVAdapter(null, this) }
    private val viewModel by lazy { ViewModelProvider(this).get(DurationsViewModel::class.java) }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_report, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val id = menu.findItem(R.id.rm_filter_period)
        if(viewModel.displayWeek){
            id.setIcon(R.drawable.ic_baseline_filter_1_24)
            id.setTitle(R.string.rm_title_filter_day)
        }else{
            id.setIcon(R.drawable.ic_baseline_filter_7_24)
            id.setTitle(R.string.rm_title_filter_week)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.rm_filter_period->{
                viewModel.toggleDisplayWeek()
                invalidateOptionsMenu()
                return true
            }
            R.id.rm_filter_date ->{
                showDatePickerDialog(getString(R.string.date_title_filter), DIALOG_FILTER)
                return true
            }
            R.id.rm_delete -> {
                showDatePickerDialog(getString(R.string.date_title_delete), DIALOG_DELETE)
                return true
            }
            R.id.rm_settings -> {
                val dialog = SettingsDialog()
                dialog.show(supportFragmentManager, "settings")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDatePickerDialog(title: String, dialogId: Int){
        val dialogFragment = DatePickerFragment()
        val arguments = Bundle()

        arguments.putInt(DATE_PICKER_ID, dialogId)
        arguments.putString(DATE_PICKER_TITLE, title)
        arguments.putSerializable(DATE_PICKER_DATE, viewModel.getFilterDate())
        arguments.putInt(DATE_PICKER_FDOW, viewModel.firstDayOfWeek)
        dialogFragment.arguments = arguments
        dialogFragment.show(supportFragmentManager, "DatePicker")
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        when(view.tag as Int){
            DIALOG_FILTER -> viewModel.setReportDate(year, month, dayOfMonth)
            DIALOG_DELETE -> {
                val cal = GregorianCalendar()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                val formDate = DateFormat.getDateFormat(this).format(cal.time)

                val dialog = DialogFragment()
                val args = Bundle()
                args.apply {
                    putInt(DIALOG_ID, DIALOG_DELETE)
                    putString(DIALOG_MESSAGE, getString(R.string.delete_timings_message, formDate))
                    putLong(DELETION_DATE, cal.timeInMillis)
                }
                dialog.arguments = args
                dialog.show(supportFragmentManager, null)
            }
            else -> throw IllegalArgumentException("Invalid mode when receiving DatePicker Dialog")
        }
    }

    override fun onPositiveCallback(dialogId: Int, args: Bundle) {
        Log.d(TAG, "onPositiveCallback called with dialogId: $dialogId")
        if(dialogId == DIALOG_DELETE){
            val deleteDate = args.getLong(DELETION_DATE)
            viewModel.deleteRecords(deleteDate)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_durations_report)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        td_list.layoutManager = LinearLayoutManager(this)
        td_list.adapter = reportAdapter

        viewModel.cursor.observe(this, { cursor -> reportAdapter.swapCursor(cursor)?.close()})

        td_name_heading.setOnClickListener {
            updateSortOrder(it.id)
        }
        td_description_heading?.setOnClickListener {
            updateSortOrder(it.id)
        }
        td_start_heading.setOnClickListener {
            updateSortOrder(it.id)
        }
        td_duration_heading.setOnClickListener {
            updateSortOrder(it.id)
        }
    }

    private fun updateSortOrder(view: Int){
        // update the sortOrder
        viewModel.sortOrder = when(view){
            R.id.td_name_heading -> SortColumns.NAME
            R.id.td_description_heading -> SortColumns.DESCRIPTION
            R.id.td_start_heading -> SortColumns.START_DATE
            R.id.td_duration_heading -> SortColumns.DURATION
            else -> throw IllegalArgumentException("Unknown View clicked")
        }
    }
}