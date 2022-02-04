package raghav.sharma.tasktimerapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

fun FragmentActivity.findFragmentById(fragId: Int): Fragment?{
    return supportFragmentManager.findFragmentById(fragId)
}

fun FragmentActivity.showConfirmationDialog(
    id: Int,
    message: String,
    positiveCaption: Int = R.string.ok,
    negativeCaption: Int = R.string.cancel
){
    val args = Bundle().apply {
        putInt(DIALOG_ID, id)
        putString(DIALOG_MESSAGE, message)
        putInt(DIALOG_POSITIVE_ID, positiveCaption)
        putInt(DIALOG_NEGATIVE_ID, negativeCaption)
    }
    val dialog = DialogFragment()
    dialog.arguments = args
    dialog.show(supportFragmentManager, null)
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.()-> FragmentTransaction){
    beginTransaction().func().commit()
}
fun FragmentActivity.addFragment(fragment: Fragment, fragmentId: Int){
    supportFragmentManager.inTransaction { add(fragmentId, fragment) }
}
fun FragmentActivity.replaceFragment(fragment: Fragment, fragmentId: Int){
    supportFragmentManager.inTransaction { replace(fragmentId, fragment) }
}
fun FragmentActivity.removeFragment(fragment: Fragment){
    supportFragmentManager.inTransaction { remove(fragment) }
}