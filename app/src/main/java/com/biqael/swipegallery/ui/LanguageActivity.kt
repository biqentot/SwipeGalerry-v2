package com.biqael.swipegallery.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.biqael.swipegallery.R
import com.biqael.swipegallery.util.LocaleHelper
import com.biqael.swipegallery.util.Prefs

class LanguageActivity : AppCompatActivity() {

    private var selectedLang: String? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs.isOnboardingDone(this)) {
            startActivity(Intent(this, PickerActivity::class.java))
            finish()
            return
        }

        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.bg))
            setPadding(48, 48, 48, 48)
        }
        setContentView(root)

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(getColor(R.color.text))
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 64)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = getString(R.string.choose_language)
            setTextColor(getColor(R.color.textDim))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        root.addView(subtitle)

        val btnId = langButton("Indonesia", LocaleHelper.LANG_ID)
        val btnEn = langButton("English", LocaleHelper.LANG_EN)
        root.addView(btnId)
        root.addView(btnEn)

        val next = Button(this).apply {
            text = getString(R.string.onb_next)
            isEnabled = false
            alpha = 0.5f
            setOnClickListener {
                val lang = selectedLang ?: return@setOnClickListener
                LocaleHelper.setLanguage(this@LanguageActivity, lang)
                startActivity(Intent(this@LanguageActivity, OnboardingActivity::class.java))
                finish()
            }
        }
        root.addView(next, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 48
        })

        // update next button when lang chosen
        fun updateNext() {
            next.isEnabled = selectedLang != null
            next.alpha = if (selectedLang != null) 1f else 0.5f
        }

        // patch buttons to update next
        btnId.setOnClickListener {
            selectedLang = LocaleHelper.LANG_ID
            highlight(btnId, btnEn)
            updateNext()
        }
        btnEn.setOnClickListener {
            selectedLang = LocaleHelper.LANG_EN
            highlight(btnEn, btnId)
            updateNext()
        }
    }

    private fun langButton(label: String, lang: String): Button {
        return Button(this).apply {
            text = label
            textSize = 18f
            tag = lang
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 12, 0, 12)
            }
        }
    }

    private fun highlight(selected: Button, other: Button) {
        selected.setBackgroundColor(getColor(R.color.surface2))
        selected.setTextColor(getColor(R.color.text))
        other.setBackgroundColor(getColor(R.color.surface))
        other.setTextColor(getColor(R.color.textDim))
    }
}
