package com.biqael.swipegallery.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.biqael.swipegallery.R
import com.biqael.swipegallery.util.*
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class SwipeActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_BUCKET_ID = "bucket_id"
        private const val EXTRA_BUCKET_NAME = "bucket_name"
        private const val REQ_DELETE = 500

        fun intent(context: Context, bucketId: String?, bucketName: String?): Intent =
            Intent(context, SwipeActivity::class.java).apply {
                putExtra(EXTRA_BUCKET_ID, bucketId)
                putExtra(EXTRA_BUCKET_NAME, bucketName)
            }
    }

    private lateinit var repo: MediaRepository
    private lateinit var trash: TrashManager

    private val photos = mutableListOf<PhotoInfo>()
    private val kept = mutableSetOf<Long>()
    private val toDelete = mutableSetOf<Long>()
    private val history = mutableListOf<Pair<Long, Boolean>>() // id to (true=keep)
    private var idx = 0
    private var busy = false
    private var token = 0
    private var atEnd = false
    private var bucketId: String? = null
    private var bucketName: String? = null

    private lateinit var headerText: TextView
    private lateinit var card: ImageView
    private lateinit var overlayDelete: LinearLayout
    private lateinit var overlayKeep: LinearLayout
    private lateinit var finishBtn: TextView
    private lateinit var deleteBtn: TextView
    private lateinit var undoBtn: TextView
    private lateinit var keepBtn: TextView
    private lateinit var progressOverlay: LinearLayout
    private lateinit var progressText: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = MediaRepository(this)
        trash = TrashManager(this)
        bucketId = intent.getStringExtra(EXTRA_BUCKET_ID)
        bucketName = intent.getStringExtra(EXTRA_BUCKET_NAME)
        buildUi()
        loadPhotos()
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.bg))
        }
        setContentView(root)

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(main, FrameLayout.LayoutParams(-1, -1))

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 32, 24, 16)
        }
        main.addView(header)

        val back = TextView(this).apply {
            text = "←"
            textSize = 26f
            setTextColor(getColor(R.color.text))
            setPadding(8, 0, 24, 0)
            setOnClickListener { onBackPressed() }
        }
        header.addView(back)

        headerText = TextView(this).apply {
            text = "🗑 0"
            setTextColor(getColor(R.color.text))
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        header.addView(headerText)

        finishBtn = TextView(this).apply {
            text = getString(R.string.swipe_finish)
            setTextColor(getColor(R.color.keep))
            textSize = 16f
            setPadding(16, 8, 16, 8)
            setOnClickListener { confirmFinish() }
        }
        header.addView(finishBtn)

        // Card area
        val cardContainer = FrameLayout(this)
        main.addView(cardContainer, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            setMargins(24, 8, 24, 8)
        })

        card = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(getColor(R.color.surface))
        }
        cardContainer.addView(card, FrameLayout.LayoutParams(-1, -1))

        overlayDelete = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.overlay_delete))
            alpha = 0f
        }
        cardContainer.addView(overlayDelete, FrameLayout.LayoutParams(-1, -1))
        overlayDelete.addView(TextView(this).apply {
            text = "🗑"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        overlayDelete.addView(TextView(this).apply {
            text = getString(R.string.swipe_buang)
            textSize = 28f
            setTextColor(getColor(R.color.text))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        overlayKeep = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.overlay_keep))
            alpha = 0f
        }
        cardContainer.addView(overlayKeep, FrameLayout.LayoutParams(-1, -1))
        overlayKeep.addView(TextView(this).apply {
            text = "♥"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        overlayKeep.addView(TextView(this).apply {
            text = getString(R.string.swipe_simpan)
            textSize = 28f
            setTextColor(getColor(R.color.text))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        card.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                if (busy) return true
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> sx = e.rawX
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - sx
                        v.translationX = dx
                        v.rotation = dx / 40f
                        overlayKeep.alpha = (dx / dp(120)).coerceIn(0f, 1f)
                        overlayDelete.alpha = (-dx / dp(120)).coerceIn(0f, 1f)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val dx = e.rawX - sx
                        if (dx > dp(100)) fling(true)
                        else if (dx < -dp(100)) fling(false)
                        else resetCard()
                    }
                }
                return true
            }
        })

        // Bottom buttons
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 32)
        }
        main.addView(bar)

        deleteBtn = bottomBtn(getString(R.string.swipe_delete), R.color.delete) { fling(false) }
        undoBtn = bottomBtn(getString(R.string.swipe_undo), R.color.textDim) { undo() }
        keepBtn = bottomBtn(getString(R.string.swipe_keep), R.color.keep) { fling(true) }

        bar.addView(deleteBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = 8 })
        bar.addView(undoBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = 8 })
        bar.addView(keepBtn, LinearLayout.LayoutParams(0, -2, 1f))

        // Progress overlay
        progressOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xCC000000.toInt())
            visibility = View.GONE
        }
        root.addView(progressOverlay, FrameLayout.LayoutParams(-1, -1))
        progressOverlay.addView(TextView(this).apply {
            text = "⟳"
            textSize = 56f
            setTextColor(getColor(R.color.text))
            gravity = Gravity.CENTER
        })
        progressText = TextView(this).apply {
            textSize = 15f
            setTextColor(getColor(R.color.text))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        progressOverlay.addView(progressText)

        updateHeader()
    }

    private fun bottomBtn(label: String, colorRes: Int, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(16, 20, 16, 20)
            setTextColor(getColor(colorRes))
            setBackgroundColor(getColor(R.color.surface))
            setOnClickListener { onClick() }
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun loadPhotos() {
        lifecycleScope.launch {
            showProgress(getString(R.string.picker_loading))
            val all = repo.listPhotos(bucketId)
            val keptIds = Prefs.getKept(this@SwipeActivity)
            photos.clear()
            photos.addAll(all.filter { it.id !in keptIds })
            idx = 0
            history.clear()
            kept.clear()
            toDelete.clear()
            hideProgress()
            showCurrent()
        }
    }

    private fun showCurrent() {
    updateHeader()
    if (idx >= photos.size) {
        atEnd = true
        if (history.isNotEmpty()) {
            confirmFinish()
        } else {
            showFolderDone()
        }
        return
    }
    atEnd = false
    val t = ++token
    val photo = photos[idx]
    Glide.with(this).load(photo.uri).into(card)
    card.translationX = 0f
    card.rotation = 0f
    overlayDelete.alpha = 0f
    overlayKeep.alpha = 0f
    card.postDelayed({
        if (t == token) updateHeader()
    }, 100)
}

    private fun updateHeader() {
        val size = toDelete.sumOf { id -> photos.find { it.id == id }?.sizeBytes ?: 0L }
        headerText.text = "🗑 ${toDelete.size} · ${formatSize(size)}"
        undoBtn.isEnabled = history.isNotEmpty()
        undoBtn.alpha = if (history.isNotEmpty()) 1f else 0.4f
    }

    private fun fling(keep: Boolean) {
        if (busy || idx >= photos.size) return
        busy = true
        val dir = if (keep) 1 else -1
        card.animate()
            .translationX(dir * (card.width + dp(100)).toFloat())
            .rotation(dir * 25f)
            .setDuration(220)
            .withEndAction { decide(keep) }
            .start()
    }

    private fun decide(keep: Boolean) {
        val photo = photos[idx]
        history.add(photo.id to keep)
        if (keep) kept.add(photo.id)
        else toDelete.add(photo.id)
        idx++
        busy = false
        showCurrent()
    }

    private fun undo() {
        if (busy || history.isEmpty()) return
        val (id, wasKeep) = history.removeAt(history.lastIndex)
        if (wasKeep) kept.remove(id)
        else toDelete.remove(id)
        idx--
        showCurrent()
    }

    private fun resetCard() {
        card.animate().cancel()
        card.translationX = 0f
        card.rotation = 0f
        overlayDelete.alpha = 0f
        overlayKeep.alpha = 0f
    }

    private fun confirmFinish() {
    if (history.isEmpty()) {
        finish()
        return
    }
    val size = toDelete.sumOf { id -> photos.find { it.id == id }?.sizeBytes ?: 0L }
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.dlg_finish_title))
        .setMessage(getString(R.string.dlg_finish_body, toDelete.size, formatSize(size)))
        .setPositiveButton(getString(R.string.dlg_yes)) { _, _ -> execute() }
        .setNegativeButton(getString(R.string.dlg_batal)) { _, _ ->
            if (atEnd) showFolderDone()
        }
        .show()
}

    override fun onBackPressed() {
        if (history.isEmpty()) {
            super.onBackPressed()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dlg_exit_title))
            .setMessage(getString(R.string.dlg_exit_body, toDelete.size))
            .setPositiveButton(getString(R.string.dlg_cancel_all)) { _, _ ->
                // Batalkan semua
                toDelete.clear()
                kept.clear()
                history.clear()
                finish()
            }
            .setNegativeButton(getString(R.string.dlg_continue_swipe), null)
            .show()
    }

    private fun execute() {
        showProgress(getString(R.string.loading_trash))
        lifecycleScope.launch {
            // Save kept
            val existingKept = Prefs.getKept(this@SwipeActivity)
            existingKept.addAll(kept)
            Prefs.saveKept(this@SwipeActivity, existingKept)

            // Copy to trash
            val uris = mutableListOf<Uri>()
            for (id in toDelete) {
                val photo = photos.find { it.id == id } ?: continue
                val item = trash.copyToTrash(photo, bucketName ?: "Unknown")
                if (item != null) uris.add(photo.uri)
            }

            // Save last folder
            if (bucketId != null && bucketName != null) {
                Prefs.setLastFolder(this@SwipeActivity, bucketId, bucketName)
            }

            hideProgress()

            if (uris.isEmpty()) {
                Toast.makeText(this@SwipeActivity,
                    getString(R.string.toast_done, 0, formatSize(0)),
                    Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Ask system to delete originals
            val pi = MediaStore.createDeleteRequest(contentResolver, uris)
            startIntentSenderForResult(pi.intentSender, REQ_DELETE, null, 0, 0, 0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DELETE) {
            val count = toDelete.size
            val size = toDelete.sumOf { id -> photos.find { it.id == id }?.sizeBytes ?: 0L }
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this,
                    getString(R.string.toast_done, count, formatSize(size)),
                    Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun showFolderDone() {
        card.setImageDrawable(null)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.swipe_folder_done))
            .setMessage(getString(R.string.swipe_folder_done_body))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.swipe_back_folder)) { _, _ -> finish() }
            .show()
    }

    private fun showProgress(msg: String) {
        progressText.text = msg
        progressOverlay.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progressOverlay.visibility = View.GONE
    }
}
