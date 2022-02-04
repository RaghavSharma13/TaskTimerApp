package raghav.sharma.tasktimerapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDialogFragment

private const val TAG = "DialogFragment"
const val DIALOG_ID = "ID"
const val DIALOG_MESSAGE = "DIALOG MESSAGE"
const val DIALOG_POSITIVE_ID = "POSITIVE OPTION ID"
const val DIALOG_NEGATIVE_ID = "NEGATIVE OPTION ID"
class DialogFragment: AppCompatDialogFragment() {

    private var dialogEvents: DialogEvents? = null
    internal interface DialogEvents{
        fun onPositiveCallback(dialogId: Int, args: Bundle)
        fun onNegativeCallback(dialogId: Int, args: Bundle){}
        fun onCancelCallback(dialogId: Int){}
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog called")
        val builder = AlertDialog.Builder(requireContext())

        val arguments = arguments
        val dialogId: Int
        val messageString: String?
        var positiveStringId: Int
        var negativeStringId: Int

        if(arguments != null){
            dialogId = arguments.getInt(DIALOG_ID)
            messageString = arguments.getString(DIALOG_MESSAGE)
            if(dialogId == 0 || messageString == null) throw IllegalArgumentException("Dialog id or message are not present in the bundle")

            positiveStringId = arguments.getInt(DIALOG_POSITIVE_ID)
            if(positiveStringId == 0) positiveStringId= R.string.ok

            negativeStringId = arguments.getInt(DIALOG_NEGATIVE_ID)
            if(negativeStringId == 0) negativeStringId= R.string.cancel
        }else throw IllegalArgumentException("Must pass Dialog id and message to the fragment")

        return builder.setMessage(messageString)
            .setPositiveButton(positiveStringId){
                    _, _ -> dialogEvents?.onPositiveCallback(dialogId, arguments)
            }
            .setNegativeButton(negativeStringId){
                    _, _ ->
                Log.d(TAG, "Negative Button Clicked")
             dialogEvents?.onNegativeCallback(dialogId, arguments)
            }
            .create()
    }

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttach: Begins")
        super.onAttach(context)

        // someone should implement the interface
        dialogEvents = try {
            // if a fragment is calling
            parentFragment as DialogEvents
        }catch (e: TypeCastException){
            try{
                // no parent fragment so an activity must be calling
                context as DialogEvents
            }catch (e: ClassCastException){
                // activity is not implementing the interface
                throw ClassCastException("Activity $context must implement DialogEvents interface.")
            }
        }catch (e: ClassCastException){
            // parent fragment is not implementing the interface
            throw ClassCastException("Fragment $parentFragment must implement DialogEvents interface.")
        }
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach called")
        super.onDetach()
        dialogEvents = null
    }

    override fun onCancel(dialog: DialogInterface) {
        Log.d(TAG, "onCancel Called")
        val dialogId = requireArguments().getInt(DIALOG_ID)
        dialogEvents?.onCancelCallback(dialogId)
    }
}