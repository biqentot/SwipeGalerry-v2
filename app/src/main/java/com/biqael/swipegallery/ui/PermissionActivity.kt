package com.biqael.swipegallery.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.biqael.swipegallery.R
import com.biqael.swipegallery.util.LocaleHelper
import com.biqael.swipegallery.util.Prefs

class PermissionActivity : AppCompatActivity() {

    private val REQ = 100

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermission()) {
            goNext()
            return
        }

        buildUi()
    }

    private fun hasPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.bg))
            setPadding(48, 48, 48, 48)
        }
        setContentView(root)

        val icon = TextView(this).apply {
            text = "🖼"
            textSize = 96f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        root.addView(icon)

        val title = TextView(this).apply {
            text = getString(R.string.perm_title)
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text))
            setPadding(0, 0, 0, 24)
        }
        root.addView(title)

        val body = TextView(this).apply {
            text = getString(R.string.perm_body)
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.textDim))
            setPadding(0, 0, 0, 48)
        }
        root.addView(body)

        val btn = Button(this).apply {
            text = getString(R.string.perm_button)
            setOnClickListener { requestPerm() }
        }
        root.addView(btn, LinearLayout.LayoutParams(-1, -2))
    }

    private fun requestPerm() {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        requestPermissions(arrayOf(perm), REQ)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                goNext()
            } else {
                android.widget.Toast.makeText(this, getString(R.string.perm_required), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goNext() {
        Prefs.setOnboardingDone(this)
        startActivity(Intent(this, PickerActivity::class.java))
        finish()
    }
}
