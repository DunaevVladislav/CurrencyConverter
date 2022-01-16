package com.worldofcurrency.currencyconverter

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class PickImageDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val items = arrayOf("Pick from gallery", "Take photo", "Delete background")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Choose")
                .setItems(
                    items
                ) { _, which ->
                    val mainActivity = (activity as MainActivity)
                    when (which) {
                        0 -> {
                            mainActivity.openGalleryForPickBackground()
                        }
                        1 -> {
                            mainActivity.openCameraForPickBackground()
                        }
                        2 -> {
                            mainActivity.deleteBackground()
                        }
                    }
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
