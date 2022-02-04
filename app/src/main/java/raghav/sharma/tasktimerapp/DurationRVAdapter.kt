package raghav.sharma.tasktimerapp

import android.content.Context
import android.database.Cursor
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.task_duration_items.view.*
import java.util.*

class DurationViewHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer{

}

private const val TAG = "DurationRVAdatper"

class DurationRVAdapter(private var cursor: Cursor?, context: Context): RecyclerView.Adapter<DurationViewHolder>() {

    private val dateFormat = DateFormat.getDateFormat(context)

    override fun getItemCount(): Int {
        return cursor?.count ?:0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DurationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_duration_items, parent, false)
        return DurationViewHolder(view)
    }

    override fun onBindViewHolder(holder: DurationViewHolder, position: Int) {
        Log.d(TAG, "onBindHolder called")
        val cursor = cursor
        if(cursor!=null && cursor.count != 0){
            if(!cursor.moveToPosition(position)) throw IllegalStateException("Couldn't move cursor to position $position")
            val name = cursor.getString(cursor.getColumnIndex(DurationsContract.Columns.NAME))
            val description = cursor.getString(cursor.getColumnIndex(DurationsContract.Columns.DESCRIPTION))
            val startDate = cursor.getLong(cursor.getColumnIndex(DurationsContract.Columns.START_TIME))
            val duration = cursor.getLong(cursor.getColumnIndex(DurationsContract.Columns.DURATION))

            val userDate = dateFormat.format(startDate*1000)
            val totalTime = formatDuration(duration)

            holder.containerView.td_name.text = name
            holder.containerView.td_description?.text = description // safe call because it is not present in portrait mode
            holder.containerView.td_start.text = userDate
            holder.containerView.td_duration.text = totalTime
        }
    }

    private fun formatDuration(duration: Long): String{
        // durations is in seconds so we need to convert to hours:min:sec
        // can't use Time object because it doesn't handle greater than 24 hours
        val hours = duration/3600
        val remainder = duration - hours*3600
        val minutes = remainder/60
        val seconds = remainder%60

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun swapCursor(newCursor: Cursor?): Cursor?{
        if(newCursor == cursor) return null
        val items = itemCount
        val oldCursor = cursor
        cursor = newCursor
        if(cursor!= null) notifyDataSetChanged()
        else notifyItemRangeRemoved(0, items)
        return oldCursor
    }
}