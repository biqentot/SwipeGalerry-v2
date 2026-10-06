package com.biqael.swipegallery.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.biqael.swipegallery.R
import com.biqael.swipegallery.util.*
import kotlinx.coroutines.launch

class PickerActivity : AppCompatActivity() {

    private enum class Tab { INTERNAL, SD, ALL }

    private lateinit var repo: MediaRepository
    private lateinit var trash: TrashManager
    private lateinit var listView: RecyclerView
    private lateinit var loadingView: LinearLayout
    private lateinit var emptyView: LinearLayout
    private lateinit var emptyTitle: TextView
    private lateinit var emptyBody: TextView
    private lateinit var tabRow: LinearLayout
    private lateinit var tabViews: List<TextView>
    private lateinit var trashBtn: TextView
    private lateinit var continueBtn: Button
    private var currentTab = Tab.INTERNAL
    private var allFolders: List<FolderInfo> = emptyList()
    private var sdVolumes: List<String> = emptyList()
    private var lastFolderId: String? = null
    private var lastFolderName: String? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = MediaRepository(this)
        trash = TrashManager(this)
        buildUi()
        refreshTrashBadge()
        loadFolders()
    }

    override fun onResume() {
        super.onResume()
        refreshTrashBadge()
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
            setPadding(32, 32, 32, 16)
        }
        root.addView(header)

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(getColor(R.color.text))
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        header.addView(title)

        val settingsBtn = TextView(this).apply {
            text = "⚙"
            textSize = 22f
            setTextColor(getColor(R.color.text))
            setPadding(16, 0, 16, 0)
            contentDescription = getString(R.string.cd_settings)
            setOnClickListener { showLanguageDialog() }
        }
        header.addView(settingsBtn)

        trashBtn = TextView(this).apply {
            text = "🗑 0"
            textSize = 16f
            setTextColor(getColor(R.color.text))
            setPadding(16, 0, 0, 0)
            contentDescription = getString(R.string.cd_trash)
            setOnClickListener {
                startActivity(Intent(this@PickerActivity, TrashActivity::class.java))
            }
        }
        header.addView(trashBtn)

        // Tabs
        tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 8, 24, 8)
        }
        root.addView(tabRow)

        val tabLabels = listOf(
            getString(R.string.picker_tab_internal) to Tab.INTERNAL,
            getString(R.string.picker_tab_sd) to Tab.SD,
            getString(R.string.picker_tab_all) to Tab.ALL
        )
        tabViews = tabLabels.map { (label, tab) ->
            TextView(this).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(24, 16, 24, 16)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginEnd = 8
                }
                setOnClickListener {
                    currentTab = tab
                    updateTabs()
                    applyFilter()
                }
            }
        }
        tabViews.forEach { tabRow.addView(it) }
        updateTabs()

        // Continue button
        continueBtn = Button(this).apply {
            visibility = View.GONE
            setOnClickListener {
                val id = lastFolderId ?: return@setOnClickListener
                val name = lastFolderName ?: id
                startActivity(SwipeActivity.intent(this@PickerActivity, id, name))
            }
        }
        root.addView(continueBtn, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(32, 8, 32, 8)
        })

        // Content area (FrameLayout for list/loading/empty)
        val content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        listView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@PickerActivity)
            visibility = View.GONE
        }
        content.addView(listView, FrameLayout.LayoutParams(-1, -1))

        loadingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        content.addView(loadingView, FrameLayout.LayoutParams(-1, -1))
        loadingView.addView(TextView(this).apply {
            text = "⟳"
            textSize = 48f
            setTextColor(getColor(R.color.text))
            gravity = Gravity.CENTER
        })
        loadingView.addView(TextView(this).apply {
            text = getString(R.string.picker_loading)
            setTextColor(getColor(R.color.textDim))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        })

        emptyView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        content.addView(emptyView, FrameLayout.LayoutParams(-1, -1))
        emptyView.addView(TextView(this).apply {
            text = "📭"
            textSize = 72f
            gravity = Gravity.CENTER
        })
        emptyTitle = TextView(this).apply {
            textSize = 20f
            setTextColor(getColor(R.color.text))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        emptyView.addView(emptyTitle)
        emptyBody = TextView(this).apply {
            textSize = 14f
            setTextColor(getColor(R.color.textDim))
            gravity = Gravity.CENTER
            setPadding(48, 0, 48, 0)
        }
        emptyView.addView(emptyBody)

        // Sort all button
        val sortAll = Button(this).apply {
            text = getString(R.string.picker_sort_all)
            setOnClickListener {
                startActivity(SwipeActivity.intent(this@PickerActivity, null, null))
            }
        }
        root.addView(sortAll, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(32, 8, 32, 32)
        })

        loadLastFolder()
    }

    private fun loadLastFolder() {
        val (id, name) = Prefs.getLastFolder(this)
        if (id != null && name != null) {
            lastFolderId = id
            lastFolderName = name
            continueBtn.text = "${getString(R.string.picker_continue)}: $name"
            continueBtn.visibility = View.VISIBLE
        }
    }

    private fun updateTabs() {
        val activeBg = getColor(R.color.surface2)
        val inactiveBg = getColor(R.color.surface)
        val activeText = getColor(R.color.text)
        val inactiveText = getColor(R.color.textDim)
        tabViews.forEachIndexed { i, v ->
            val isActive = (i == 0 && currentTab == Tab.INTERNAL) ||
                    (i == 1 && currentTab == Tab.SD) ||
                    (i == 2 && currentTab == Tab.ALL)
            v.setBackgroundColor(if (isActive) activeBg else inactiveBg)
            v.setTextColor(if (isActive) activeText else inactiveText)
        }
    }

    private fun loadFolders() {
        showLoading(true)
        lifecycleScope.launch {
            sdVolumes = repo.listVolumes().filter { it != "external_primary" }
            allFolders = repo.listFolders(null)
            applyFilter()
            showLoading(false)
        }
    }

    private fun applyFilter() {
        val filtered = when (currentTab) {
            Tab.INTERNAL -> allFolders.filter { it.volumeName == "external_primary" }
            Tab.SD -> allFolders.filter { it.volumeName != "external_primary" }
            Tab.ALL -> allFolders
        }

        if (currentTab == Tab.SD && sdVolumes.isEmpty()) {
            showEmpty(getString(R.string.picker_sd_empty), getString(R.string.picker_sd_empty_body))
            return
        }
        if (filtered.isEmpty()) {
            showEmpty(getString(R.string.picker_empty_title), getString(R.string.picker_empty_body))
            return
        }
        showList(filtered)
    }

    private fun showLoading(loading: Boolean) {
        loadingView.visibility = if (loading) View.VISIBLE else View.GONE
        listView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }

    private fun showEmpty(title: String, body: String) {
        loadingView.visibility = View.GONE
        listView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyTitle.text = title
        emptyBody.text = body
    }

    private fun showList(folders: List<FolderInfo>) {
        loadingView.visibility = View.GONE
        emptyView.visibility = View.GONE
        listView.visibility = View.VISIBLE
        listView.adapter = FolderAdapter(folders) { folder ->
            Prefs.setLastFolder(this, folder.bucketId, folder.name)
            startActivity(SwipeActivity.intent(this, folder.bucketId, folder.name))
        }
    }

    private fun refreshTrashBadge() {
        lifecycleScope.launch {
            val count = trash.getCount()
            trashBtn.text = "🗑 $count"
        }
    }

    private fun showLanguageDialog() {
        val current = LocaleHelper.getLanguage(this)
        val options = arrayOf(getString(R.string.lang_id), getString(R.string.lang_en))
        val checked = if (current == LocaleHelper.LANG_ID) 0 else 1
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_title))
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val lang = if (which == 0) LocaleHelper.LANG_ID else LocaleHelper.LANG_EN
                LocaleHelper.setLanguage(this, lang)
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton(getString(R.string.settings_cancel), null)
            .show()
    }

    private inner class FolderAdapter(
        private val items: List<FolderInfo>,
        private val onClick: (FolderInfo) -> Unit
    ) : RecyclerView.Adapter<FolderAdapter.VH>() {

        inner class VH(val container: LinearLayout) : RecyclerView.ViewHolder(container)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val ll = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 32, 48, 32)
                isClickable = true
                foreground = android.graphics.drawable.ColorDrawable(0x22FFFFFF)
            }
            return VH(ll)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val f = items[position]
            holder.container.removeAllViews()

            val name = TextView(holder.container.context).apply {
                text = f.name
                setTextColor(holder.container.context.getColor(R.color.text))
                textSize = 16f
            }
            holder.container.addView(name)

            val count = TextView(holder.container.context).apply {
                text = "${f.count} · ${f.volumeName}"
                setTextColor(holder.container.context.getColor(R.color.textDim))
                textSize = 12f
                setPadding(0, 4, 0, 0)
            }
            holder.container.addView(count)

            holder.container.setOnClickListener { onClick(f) }
        }
    }
}
