package com.biqael.swipegallery.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.biqael.swipegallery.R
import com.biqael.swipegallery.data.TrashItem
import com.biqael.swipegallery.util.LocaleHelper
import com.biqael.swipegallery.util.TrashManager
import com.biqael.swipegallery.util.formatSize
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.io.File

class TrashActivity : AppCompatActivity() {

    private lateinit var trash: TrashManager
    private lateinit var listView: RecyclerView
    private lateinit var infoText: TextView
    private lateinit var emptyView: LinearLayout
    private lateinit var actionBar: LinearLayout
    private lateinit var restoreBtn: Button
    private lateinit var deleteBtn: Button
    private var items: List<TrashItem> = emptyList()
    private val selected = mutableSetOf<Long>()
    private var selectMode = false

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trash = TrashManager(this)
        buildUi()
        load()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.bg))
        }
        setContentView(root)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 32, 24, 16)
        }
        root.addView(header)

        val back = TextView(this).apply {
            text = "←"
            textSize = 26f
            setTextColor(getColor(R.color.text))
            setPadding(8, 0, 24, 0)
            setOnClickListener { finish() }
        }
        header.addView(back)

        val title = TextView(this).apply {
            text = getString(R.string.trash_title)
            setTextColor(getColor(R.color.text))
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        header.addView(title)

        val selectBtn = TextView(this).apply {
            text = getString(R.string.trash_select)
            setTextColor(getColor(R.color.text))
            textSize = 14f
            setPadding(16, 8, 16, 8)
            setOnClickListener { toggleSelectMode() }
        }
        header.addView(selectBtn)

        infoText = TextView(this).apply {
            setTextColor(getColor(R.color.textDim))
            textSize = 13f
            setPadding(32, 0, 32, 16)
        }
        root.addView(infoText)

        // Content
        val content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        listView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@TrashActivity, 3)
            visibility = View.GONE
        }
        content.addView(listView, FrameLayout.LayoutParams(-1, -1))

        emptyView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        content.addView(emptyView, FrameLayout.LayoutParams(-1, -1))
        emptyView.addView(TextView(this).apply {
            text = "🗑"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        emptyView.addView(TextView(this).apply {
            text = getString(R.string.trash_empty_title)
            setTextColor(getColor(R.color.text))
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        })
        emptyView.addView(TextView(this).apply {
            text = getString(R.string.trash_empty_body)
            setTextColor(getColor(R.color.textDim))
            textSize = 14f
            gravity = Gravity.CENTER
        })

        // Action bar
        actionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 32)
            visibility = View.GONE
        }
        root.addView(actionBar)

        restoreBtn = Button(this).apply {
            text = getString(R.string.trash_restore)
            setOnClickListener { confirmRestore() }
        }
        actionBar.addView(restoreBtn, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginEnd = 8
        })

        deleteBtn = Button(this).apply {
            text = getString(R.string.trash_delete)
            setOnClickListener { confirmDelete() }
        }
        actionBar.addView(deleteBtn, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun load() {
        lifecycleScope.launch {
            trash.autoClean(
                maxAgeMs = 30L * 24 * 60 * 60 * 1000,
                maxBytes = 500L * 1024 * 1024
            )
            items = trash.getAll()
            selected.clear()
            selectMode = false
            updateUi()
        }
    }

    private fun updateUi() {
        val totalSize = items.sumOf { it.sizeBytes }
        infoText.text = getString(R.string.trash_count, items.size, formatSize(totalSize))

        if (items.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            listView.visibility = View.GONE
            actionBar.visibility = View.GONE
            return
        }

        emptyView.visibility = View.GONE
        listView.visibility = View.VISIBLE

        if (selectMode) {
            actionBar.visibility = View.VISIBLE
            restoreBtn.text = if (selected.isEmpty()) getString(R.string.trash_restore)
                else getString(R.string.trash_restore_n, selected.size)
            deleteBtn.text = if (selected.isEmpty()) getString(R.string.trash_delete)
                else getString(R.string.trash_delete_n, selected.size)
            restoreBtn.isEnabled = selected.isNotEmpty()
            deleteBtn.isEnabled = selected.isNotEmpty()
        } else {
            actionBar.visibility = View.GONE
        }

        listView.adapter = TrashAdapter()
    }

    private fun toggleSelectMode() {
        selectMode = !selectMode
        selected.clear()
        updateUi()
    }

    private fun confirmRestore() {
        if (selected.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dlg_restore_title))
            .setMessage(getString(R.string.dlg_restore_body, selected.size))
            .setPositiveButton(getString(R.string.dlg_yes)) { _, _ -> doRestore() }
            .setNegativeButton(getString(R.string.dlg_batal), null)
            .show()
    }

    private fun doRestore() {
        val ids = selected.toList()
        lifecycleScope.launch {
            var ok = 0
            for (id in ids) {
                val item = items.find { it.id == id } ?: continue
                if (trash.restore(item)) ok++
            }
            Toast.makeText(this@TrashActivity, getString(R.string.toast_restored, ok), Toast.LENGTH_SHORT).show()
            load()
        }
    }

    private fun confirmDelete() {
        if (selected.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dlg_delete_perm_title))
            .setMessage(getString(R.string.dlg_delete_perm_body, selected.size))
            .setPositiveButton(getString(R.string.dlg_delete)) { _, _ -> doDelete() }
            .setNegativeButton(getString(R.string.dlg_batal), null)
            .show()
    }

    private fun doDelete() {
        val ids = selected.toList()
        lifecycleScope.launch {
            for (id in ids) {
                val item = items.find { it.id == id } ?: continue
                trash.deletePermanently(item)
            }
            Toast.makeText(this@TrashActivity, getString(R.string.toast_deleted, ids.size), Toast.LENGTH_SHORT).show()
            load()
        }
    }

    private fun trashFile(name: String): File = File(filesDir, "trash/$name")

    private inner class TrashAdapter : RecyclerView.Adapter<TrashAdapter.VH>() {

        inner class VH(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val frame = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (parent.resources.displayMetrics.widthPixels / 3)
                ).apply { setMargins(4, 4, 4, 4) }
            }
            return VH(frame)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val ctx = holder.frame.context
            holder.frame.removeAllViews()

            val img = ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(-1, -1)
            }
            holder.frame.addView(img)
            Glide.with(ctx).load(trashFile(item.trashFileName)).into(img)

            if (selectMode) {
                val cb = CheckBox(ctx).apply {
                    isChecked = selected.contains(item.id)
                    layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
                }
                holder.frame.addView(cb)
                holder.frame.setOnClickListener {
                    if (selected.contains(item.id)) selected.remove(item.id)
                    else selected.add(item.id)
                    cb.isChecked = selected.contains(item.id)
                    updateUi()
                }
            } else {
                holder.frame.setOnClickListener {
                    selectMode = true
                    selected.add(item.id)
                    updateUi()
                }
            }
        }
    }
}
