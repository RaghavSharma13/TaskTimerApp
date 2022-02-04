package raghav.sharma.tasktimerapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_main.*

private const val TAG = "FragmentMain"
private const val DIALOG_ID_DELETE = 1
private const val DIALOG_TASK_ID = "TASK ID"
private const val TASK_POSITION = "TASK POSITION"

class FragmentMain : Fragment(), TaskRecyclerAdapter.OnTaskClickListener, DialogFragment.DialogEvents {

//    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(TaskTimerViewModel::class.java) }
    private val viewModel : TaskTimerViewModel by activityViewModels()
    private val mAdapter = TaskRecyclerAdapter(null, this)

    interface OnTaskEdit{
        fun onTaskEdit(task: Task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called.")
        super.onCreate(savedInstanceState)

        viewModel.cursor.observe(this, { cursor -> mAdapter.swapCursor(cursor)?.close() })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called.")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: starts")
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mAdapter

        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT){
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    Log.d(TAG, "onSwiped Called")
                    if(direction == ItemTouchHelper.LEFT){
                        val task = (viewHolder as TaskHolder).task
                        onDeleteClickListener(task, viewHolder.adapterPosition)
                    }
                }
            }
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttack: starts")
        super.onAttach(context)
        if(context !is OnTaskEdit) throw RuntimeException("$context must implement OnTaskEdit listener.")
    }

    override fun onEditClickListener(task: Task) {
        Log.d(TAG, "onEdit clicked")
        (activity as OnTaskEdit).onTaskEdit(task)
    }

    fun onDeleteClickListener(task: Task, position:Int) {
        if(task.id == viewModel.editingTask){
            Log.d(TAG, "Checking if task ${task.id} is being edited. Task position is $position")
            Toast.makeText(context, getString(R.string.delete_editing_task), Toast.LENGTH_SHORT).show()
            mAdapter.notifyItemChanged(position)
        }else{
            val args = Bundle().apply {
                putInt(DIALOG_ID, DIALOG_ID_DELETE)
                putString(DIALOG_MESSAGE, getString(R.string.deldiag_message, task.id, task.name))
                putInt(DIALOG_POSITIVE_ID, R.string.deldiag_positive_caption)
                putLong(DIALOG_TASK_ID, task.id)
                putInt(TASK_POSITION, position)
            }
            val dialog = DialogFragment()
            dialog.arguments = args
            dialog.show(childFragmentManager, null)
        }
    }

    override fun onLongClickListener(task: Task) {
        Log.d(TAG, "onLongClickListener started")
        viewModel.timeTask(task)
    }

    override fun onPositiveCallback(dialogId: Int, args: Bundle) {
        Log.d(TAG, "OnPositiveCallback called with $dialogId")
        if(dialogId == DIALOG_ID_DELETE){
            val taskId = args.getLong(DIALOG_TASK_ID)
            if(BuildConfig.DEBUG && taskId == 0L) throw AssertionError("Task id is 0.")
            viewModel.deleteTask(taskId)
        }
    }

    override fun onNegativeCallback(dialogId: Int, args: Bundle) {
        if(dialogId == DIALOG_ID_DELETE){
            Log.d(TAG, "onNegativeCallback called on Delete Dialog")
            val position = args.getInt(TASK_POSITION)
            Log.d(TAG, "Restoring Task at position $position")
            mAdapter.notifyItemChanged(position)
        }
    }
}