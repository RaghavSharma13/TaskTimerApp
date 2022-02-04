package raghav.sharma.tasktimerapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import raghav.sharma.tasktimerapp.debug.TestData

private const val TAG = "mainActivity"
private const val DIALOG_ID_CANCEL_ID = 1
class MainActivity : AppCompatActivity(), AddEditFragment.OnSaveClicked, FragmentMain.OnTaskEdit, DialogFragment.DialogEvents {

    // size and orientation for displaying the fragment and fragment pane together or separately
    private var mTwoPane = false
    private var aboutDialog: AlertDialog? = null
//    private val viewModel by lazy { ViewModelProvider(this).get(TaskTimerViewModel::class.java) }
    private val viewModel: TaskTimerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mTwoPane = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val fragment = findFragmentById(R.id.task_details_container)
        if(fragment!=null){
            // meaning an addEdit fragment was created
            showEditPane()
        }else{
            task_details_container.visibility = if(mTwoPane) View.INVISIBLE else View.GONE
            fragment_main.visibility = View.VISIBLE
        }

        viewModel.timingTask.observe(this, { timing ->
            if(timing!=null) current_task.text = getString(R.string.current_timing_message, timing)
            else current_task.text = getString(R.string.no_task_message)
        })
    }

    private fun showEditPane(){
        // showing the task edit pane
        task_details_container.visibility = View.VISIBLE
        // and removing the main fragment if in portrait mode
        fragment_main.visibility = if(mTwoPane) View.VISIBLE else View.GONE
    }

    private fun removeEditFrag(fragment: Fragment? = null){
        Log.d(TAG, "RemoveEditFrag called")
        if(fragment != null) removeFragment(fragment) //supportFragmentManager.beginTransaction().remove(fragment).commit()

        //set visibility of the right hand frame
        task_details_container.visibility = if(mTwoPane) View.INVISIBLE else View.GONE
        // and show the left hand pane
        fragment_main.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onSaveClicked() {
        Log.d(TAG, "onSaveClicked")
        val fragment = findFragmentById(R.id.task_details_container)
        removeEditFrag(fragment)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        if(BuildConfig.DEBUG){
            val generate = menu.findItem(R.id.menumain_generateData)
            generate.isVisible = true
        }

        return true
    }

    private fun showAboutDialog(){
        val messageView = layoutInflater.inflate(R.layout.about_layout, null, false)
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.AppTitle)
        builder.setIcon(R.mipmap.ic_launcher)

        builder.setPositiveButton(R.string.ok){_, _ ->
            Log.d(TAG, "OnPositiveClicked")
            if(aboutDialog!=null && aboutDialog?.isShowing == true) aboutDialog?.dismiss()
        }

        aboutDialog = builder.setView(messageView).create()
        aboutDialog?.setCanceledOnTouchOutside(true)

        messageView.setOnClickListener {
            Log.d(TAG, "messageView: onclickListener called")
            if(aboutDialog!=null && aboutDialog?.isShowing == true) aboutDialog?.dismiss()
        }

        val aboutVersion = messageView.findViewById(R.id.about_version) as TextView
        aboutVersion.text = BuildConfig.VERSION_NAME

        // using a nullable type because TextView won't exist on api 21 or higher
        val aboutUrl: TextView? = messageView.findViewById(R.id.about_web_url_link)
        aboutUrl?.setOnClickListener { v->
            val intent = Intent(Intent.ACTION_VIEW)
            val s = (v as TextView).text.toString()
            intent.data = Uri.parse(s)
            try{
                startActivity(intent)
            }catch(e: ActivityNotFoundException){
                Toast.makeText(this@MainActivity, R.string.about_url_error, Toast.LENGTH_SHORT).show()
            }
        }
        aboutDialog?.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.menumain_addTask -> taskEditRequest(null)
            R.id.menumain_showDurations -> startActivity(Intent(this,DurationsReport::class.java))
            R.id.menumain_settings -> {
                val dialog = SettingsDialog()
                dialog.show(supportFragmentManager, null)
            }
            android.R.id.home ->{
                val fragment = findFragmentById(R.id.task_details_container)
                if((fragment is AddEditFragment) && fragment.isDirty()){
                    showConfirmationDialog(DIALOG_ID_CANCEL_ID,
                    getString(R.string.cancelEdit_message),
                    R.string.cancelEditDiag_positive_caption,
                    R.string.cancelEdit_negative_caption)
                }else{
                    removeEditFrag(fragment)
                }
            }
            R.id.menumain_showAbout -> showAboutDialog()
            R.id.menumain_generateData -> TestData.generateTestData(contentResolver)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val fragment = findFragmentById(R.id.task_details_container)
        if(fragment == null || mTwoPane) super.onBackPressed()
        else {
            if((fragment is AddEditFragment) && fragment.isDirty()){
                showConfirmationDialog(DIALOG_ID_CANCEL_ID,
                    getString(R.string.cancelEdit_message),
                    R.string.cancelEditDiag_positive_caption,
                    R.string.cancelEdit_negative_caption)
            }else{
                removeEditFrag(fragment)
            }
        }
    }

    override fun onPositiveCallback(dialogId: Int, args: Bundle) {
        Log.d(TAG, "onPositiveCallback called with dialog id : $dialogId")
        if(dialogId == DIALOG_ID_CANCEL_ID){
            val fragment = findFragmentById(R.id.task_details_container)
            removeEditFrag(fragment)
        }
    }

    private fun taskEditRequest(task: Task?){
        Log.d(TAG, "taskEditRequest: starts")

        // create a new frag to edit the task
//        val newFrag = AddEditFragment.newInstance(task)
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.task_details_container, newFrag)
//            .commit()
        replaceFragment(AddEditFragment.newInstance(task), R.id.task_details_container)
        showEditPane()
        Log.d(TAG, "Exiting TaskEditRequest")
    }

    override fun onTaskEdit(task: Task) {
        // this only takes care of showing the fragment
        // the update is handled by the add edit fragment itself
        taskEditRequest(task)
    }
}