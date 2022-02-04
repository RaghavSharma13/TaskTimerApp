package raghav.sharma.tasktimerapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_add_edit.*

private const val TAG = "AddEditFrag"
private const val ARG_PARAM1 = "task"

/**
 * A simple [Fragment] subclass.
 * Use the [AddEditFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddEditFragment : Fragment() {
    private var task: Task? = null
    private var listener: OnSaveClicked? = null
//    private val viewModel by lazy { ViewModelProvider(this).get(TaskTimerViewModel::class.java) }
    private val viewModel : TaskTimerViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Starts")
        super.onCreate(savedInstanceState)
        task = arguments?.getParcelable(ARG_PARAM1)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Starts")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_edit, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(savedInstanceState == null){
            val task = task
            if(task!= null){
                Log.d(TAG, "onViewCreated: Task details found, editing task ${task.id}")
                addedit_name.setText(task.name)
                addedit_description.setText(task.description)
                addedit_sortorder.setText(task.sortOrder.toString())

                viewModel.startEditing(task.id)

            }else Log.d(TAG, "onViewCreated: No previous records found, creating a new task.")
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.clear()
    }

    private fun taskFromUi(): Task{
        val sortOrder = if(addedit_sortorder.text.isNotEmpty()) Integer.parseInt(addedit_sortorder.text.toString()) else 0
        val newTask = Task(addedit_name.text.toString(), addedit_description.text.toString(), sortOrder)
        newTask.id = task?.id ?: 0
        return newTask
    }

    fun isDirty(): Boolean{
        val newTask = taskFromUi()
        return ((newTask != task) && (newTask.name.isNotBlank() || newTask.description.isNotBlank() || newTask.sortOrder != 0))
    }

    private fun saveTask(){
        /*
        * Create a newTask object with the details to be saved, then
        * call the viewModel's saveTask function to save it.
        * Task is now a data class so we can compare the new details with the original task,
        * and only save if they are different
        * */
        val newTask = taskFromUi()
        if(newTask != task){
            Log.d(TAG, "SaveTask: saving task, id is ${newTask.id}")
            task = viewModel.saveTask(newTask)
            Log.d(TAG, "Saved Task id is ${task?.id}")
        }
    }

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttack: starts")
        super.onAttach(context)
        if(context is OnSaveClicked) listener = context
        else throw RuntimeException("$context must implement onSaveClicked listener.")
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach: starts")
        super.onDetach()
        listener = null
        viewModel.stopEditing()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(activity is AppCompatActivity){
            val actionbar = (activity as AppCompatActivity?)?.supportActionBar
            actionbar?.setDisplayHomeAsUpEnabled(true)
        }
        addedit_save.setOnClickListener{
            saveTask()
            listener?.onSaveClicked()
        }
    }

    interface OnSaveClicked{
        fun onSaveClicked()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param task The task to be updated on save or null.
         * @return A new instance of fragment AddEditFragment.
         */
        @JvmStatic
        fun newInstance(task: Task?) =
            AddEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PARAM1, task)
                }
            }
    }
}