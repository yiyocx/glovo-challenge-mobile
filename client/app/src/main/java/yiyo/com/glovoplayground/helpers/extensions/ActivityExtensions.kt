package yiyo.com.glovoplayground.helpers.extensions

import android.app.Activity
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import yiyo.com.glovoplayground.R

fun Activity.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Activity.requestPermission(
    permission: String,
    title: String,
    message: String,
    requestCode: Int,
    onCancel: () -> Unit
) {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.allow)) { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                onCancel.invoke()
            }
            .show()
    } else {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }
}