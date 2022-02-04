package raghav.sharma.tasktimerapp

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.task_list_items.view.*

class TaskHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer{

    lateinit var task: Task

    fun bind(task: Task, listener: TaskRecyclerAdapter.OnTaskClickListener){
        this.task = task
        containerView.tli_name.text = task.name
        containerView.tli_description.text = task.description
        containerView.tli_edit.visibility = View.VISIBLE

        containerView.tli_edit.setOnClickListener {
//            Log.d(TAG, "tli_edit clicked")
            listener.onEditClickListener(task)
        }
        containerView.setOnLongClickListener {
//            Log.d(TAG, "Long Clicked task ${task.name}")
            listener.onLongClickListener(task)
            true
        }
    }
}

private const val TAG = "TaskRecyclerAdapter"

class TaskRecyclerAdapter(private var cursor: Cursor?, private val listener: OnTaskClickListener): RecyclerView.Adapter<TaskHolder>() {

    interface OnTaskClickListener{
        fun onEditClickListener(task: Task)
        fun onLongClickListener(task: Task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        Log.d(TAG, "onCreateViewHolder: Creating View")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_list_items, parent, false)
        return TaskHolder(view)
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
//        Log.d(TAG, "onBindViewHolder: called")
        val cursor = cursor
        if(cursor == null || cursor.count == 0){
            Log.d(TAG, "Providing Instructions")
            holder.containerView.tli_name.setText(R.string.instructions_heading)
            holder.containerView.tli_description.setText(R.string.instructions_body)
            holder.containerView.tli_edit.visibility = View.GONE
        }else{
            if(!cursor.moveToPosition(position)) throw IllegalStateException("Couldn't move cursor to position $position")
            with(cursor){
                val task = Task(
                    getString(getColumnIndex(TasksContract.Columns.TASK_NAME)),
                    getString(getColumnIndex(TasksContract.Columns.TASK_DESCRIPTION)),
                    getInt(getColumnIndex(TasksContract.Columns.SORT_ORDER))
                )
                task.id = getLong(getColumnIndex(TasksContract.Columns.ID))

                holder.bind(task, listener)
            }
        }
    }

    override fun getItemCount(): Int {
//        Log.d(TAG, "getItemCount: Starts")
        val cursor = cursor
        //        Log.d(TAG, "getItemCount: returning $count")
        return if (cursor == null || cursor.count == 0) 1 else cursor.count
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.
     * The returned old Cursor is *not* closed.
     *
     * @param newCursor The new cursor to be used
     * @return Returns the previously set Cursor, or null if there wasn't
     * one.
     * If the given new Cursor is the same instance as the previously set
     * Cursor, null is also returned.
     */

    @SuppressLint("NotifyDataSetChanged")
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