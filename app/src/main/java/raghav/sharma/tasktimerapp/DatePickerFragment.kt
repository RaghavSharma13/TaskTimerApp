package raghav.sharma.tasktimerapp

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import java.util.*

private const val TAG = "DatePicker"

const val DATE_PICKER_ID = "ID"
const val DATE_PICKER_TITLE = "TITLE"
const val DATE_PICKER_DATE = "DATE"
const val DATE_PICKER_FDOW = "FIRST DAY OF WEEK"

class DatePickerFragment: AppCompatDialogFragment(), DatePickerDialog.OnDateSetListener {
    private var dialogId = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val cal = GregorianCalendar()
        var title: String? = null
        val arguments = arguments

        if(arguments!=null){
            dialogId = arguments.getInt(DATE_PICKER_ID)
            title = arguments.getString(DATE_PICKER_TITLE)

            val givenDate = arguments.getSerializable(DATE_PICKER_DATE) as Date?
            if(givenDate!=null){
                cal.time = givenDate
                Log.d(TAG, "In oncreate dialog recieved date $givenDate")
            }
        }
        val year = cal.get(GregorianCalendar.YEAR)
        val month = cal.get(GregorianCalendar.MONTH)
        val day = cal.get(GregorianCalendar.DAY_OF_MONTH)
        val dpd = DatePickerDialog(requireContext(), this, year, month, day)
        if(title!=null) dpd.setTitle(title)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            val firstDayofWeek = arguments?.getInt(DATE_PICKER_FDOW, cal.firstDayOfWeek)?: cal.firstDayOfWeek
            dpd.datePicker.firstDayOfWeek = firstDayofWeek
        }
        return dpd
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context !is DatePickerDialog.OnDateSetListener) throw ClassCastException("$context must implement the onDateSetListener Interface")
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        Log.d(TAG, "Entering onDateSet")

        view.tag = dialogId
        (context as DatePickerDialog.OnDateSetListener?)?.onDateSet(view, year, month, dayOfMonth)
        Log.d(TAG, "Exiting onDateSet")
    }
}