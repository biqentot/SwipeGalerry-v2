package com.biqael.swipegallery.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.biqael.swipegallery.R
import com.biqael.swipegallery.util.LocaleHelper

class OnboardingActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var dots: LinearLayout
    private lateinit var backBtn: Button
    private lateinit var nextBtn: Button

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.bg))
        }
        setContentView(root)

        pager = ViewPager2(this)
        pager.adapter = OnboardingAdapter()
        root.addView(pager, LinearLayout.LayoutParams(-1, 0, 1f))

        dots = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        root.addView(dots)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 48)
        }
        root.addView(btnRow)

        backBtn = Button(this).apply {
            text = getString(R.string.onb_back)
            setOnClickListener { pager.currentItem = pager.currentItem - 1 }
        }
        btnRow.addView(backBtn, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginEnd = 12
        })

        nextBtn = Button(this).apply {
            text = getString(R.string.onb_next)
            setOnClickListener {
                if (pager.currentItem < 2) {
                    pager.currentItem = pager.currentItem + 1
                } else {
                    startActivity(Intent(this@OnboardingActivity, PermissionActivity::class.java))
                    finish()
                }
            }
        }
        btnRow.addView(nextBtn, LinearLayout.LayoutParams(0, -2, 1f))

        updateDots(0)
        updateButtons(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButtons(position)
            }
        })
    }

    private fun updateDots(pos: Int) {
        dots.removeAllViews()
        for (i in 0 until 3) {
            val d = View(this).apply {
                setBackgroundColor(if (i == pos) getColor(R.color.text) else getColor(R.color.textMuted))
            }
            val size = (8 * resources.displayMetrics.density).toInt()
            dots.addView(d, LinearLayout.LayoutParams(size, size).apply {
                setMargins(8, 0, 8, 0)
            })
        }
    }

    private fun updateButtons(pos: Int) {
        backBtn.visibility = if (pos == 0) View.INVISIBLE else View.VISIBLE
        nextBtn.text = if (pos == 2) getString(R.string.onb_start) else getString(R.string.onb_next)
    }

    private inner class OnboardingAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.VH>() {

        private val titles = intArrayOf(R.string.onb1_title, R.string.onb2_title, R.string.onb3_title)
        private val bodies = intArrayOf(R.string.onb1_body, R.string.onb2_body, R.string.onb3_body)
        private val emojis = arrayOf("👆", "🗑 ♥", "📁")

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            return VH(LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            })
        }

        override fun getItemCount() = 3

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(position)
        }

        inner class VH(val container: LinearLayout) : androidx.recyclerview.widget.RecyclerView.ViewHolder(container) {
            fun bind(pos: Int) {
                container.removeAllViews()

                val emoji = TextView(container.context).apply {
                    text = emojis[pos]
                    textSize = 72f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 48)
                }
                container.addView(emoji)

                val title = TextView(container.context).apply {
                    text = container.context.getString(titles[pos])
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(container.context.getColor(R.color.text))
                    setPadding(0, 0, 0, 24)
                }
                container.addView(title)

                val body = TextView(container.context).apply {
                    text = container.context.getString(bodies[pos])
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(container.context.getColor(R.color.textDim))
                }
                container.addView(body)
            }
        }
    }
}
