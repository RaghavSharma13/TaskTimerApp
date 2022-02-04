package raghav.sharma.tasktimerapp

import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.settings_dialog.*
import java.util.*

private const val TAG = "SettingDialog"
const val FIRST_DAY_OF_WEEK = "FirstDay"
const val IGNORE_LESS_THAN = "IgnoreLessThan"
const val DEFAULT_IGNORE_LESS_THAN = 0

private val deltas = intArrayOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 900, 1800, 2700)
class SettingsDialog: AppCompatDialogFragment() {
    private val defaultFirstDay = GregorianCalendar(Locale.getDefault()).firstDayOfWeek
    private var firstDay = defaultFirstDay
    private var ignoreLessThan = DEFAULT_IGNORE_LESS_THAN

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.SettingsDialogStyle)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "OnCreateView called")
        return inflater.inflate(R.layout.settings_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated called")
        super.onViewCreated(view, savedInstanceState)

        dialog?.setTitle(R.string.menumain_settings)
        ignoreLessThanBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if(progress<12){
                    ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondTitle, deltas[progress], resources.getQuantityString(R.plurals.settingsLittleUnits, deltas[progress]))
                }else{
                    val minutes = deltas[progress]/60
                    ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondTitle, minutes, resources.getQuantityString(R.plurals.settingsBigUnits, minutes))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                Don't need this
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Nor this
            }
        })

        okButton.setOnClickListener {
            saveValues()
            dismiss()
        }
        cancelButton.setOnClickListener { dismiss() }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewStateRestored called")
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState == null){
            readValues()
            firstDaySpinner.setSelection(firstDay - GregorianCalendar.SUNDAY)
            val seekBarValue = deltas.binarySearch(ignoreLessThan)
            if(seekBarValue<0) throw IndexOutOfBoundsException("ignoreLessThan delta binary search returner $seekBarValue, not found")

            ignoreLessThanBar.max = deltas.size -1
            ignoreLessThanBar.progress = seekBarValue

            if(ignoreLessThan<60){
                ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondTitle, ignoreLessThan, resources.getQuantityString(R.plurals.settingsLittleUnits, ignoreLessThan))
            }else{
                val minutes = ignoreLessThan/60
                ignoreSecondsTitle.text = getString(R.string.settingsIgnoreSecondTitle, minutes, resources.getQuantityString(R.plurals.settingsBigUnits, minutes))
            }
        }
    }

    private fun readValues(){
        Log.d(TAG, "readValues called")
        with(getDefaultSharedPreferences(context)){
            firstDay = getInt(FIRST_DAY_OF_WEEK, defaultFirstDay)
            ignoreLessThan = getInt(IGNORE_LESS_THAN, DEFAULT_IGNORE_LESS_THAN)
        }
        Log.d(TAG, "reading values firstDay $firstDay and ignoreLessThan $ignoreLessThan")

    }

    private fun saveValues(){
        Log.d(TAG, "saveValues called")
        val newFirstDay = firstDaySpinner.selectedItemPosition + GregorianCalendar.SUNDAY
        val newIgnoreLessThan = deltas[ignoreLessThanBar.progress]

        Log.d(TAG, "saving first day $newFirstDay and ignore as $newIgnoreLessThan")

        with(getDefaultSharedPreferences(context).edit()){
            if(newFirstDay != firstDay) putInt(FIRST_DAY_OF_WEEK, newFirstDay)
            if(newIgnoreLessThan != ignoreLessThan) putInt(IGNORE_LESS_THAN, newIgnoreLessThan)
            apply()
        }
    }
}