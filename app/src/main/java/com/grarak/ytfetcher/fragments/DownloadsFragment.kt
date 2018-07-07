package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.grarak.ytfetcher.MainActivity
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.TitleFragment
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import com.grarak.ytfetcher.views.recyclerview.DownloadItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

class DownloadsFragment : RecyclerViewFragment<TitleFragment>() {

    companion object {
        fun newInstance(user: User): DownloadsFragment {
            val args = Bundle()
            args.putSerializable(MainActivity.USER_INTENT, user)
            val fragment = DownloadsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var server: YoutubeServer? = null
    private val ids = ArrayList<String>()

    private var deleteView: View? = null
    private var selectionMode: Boolean = false
    private val selected = HashSet<YoutubeSearchResult>()
    private var deleteDialog: Boolean = false

    override var layoutXml: Int = R.layout.fragment_downloads

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun init(savedInstanceState: Bundle?) {
        deleteView = rootView!!.findViewById(R.id.delete_btn)
        deleteView!!.setOnClickListener { deleteDialog() }

        val files = Utils.getDownloadFolder(activity!!).listFiles()
        if (files == null || files.isEmpty()) {
            removeForegroundFragment(this)
            Utils.toast(R.string.no_downloads, activity!!)
            return
        }

        if (server != null) return

        showProgress()
        server = YoutubeServer(activity!!)

        ids.clear()
        for (file in files) {
            if (file.name.endsWith(".downloading")) {
                continue
            }

            val index = file.name.indexOf('.')
            if (index < 0) {
                continue
            }

            ids.add(file.name.substring(0, index))
        }

        if (ids.size == 0) {
            removeForegroundFragment(this)
            Utils.toast(R.string.no_downloads, activity!!)
            return
        }

        val fetchedCount = AtomicInteger()
        for (id in ids) {
            val youtube = Youtube()
            youtube.apikey = user!!.apikey
            youtube.id = id

            server!!.getInfo(youtube, object : YoutubeServer.YoutubeResultCallback {
                override fun onSuccess(result: YoutubeSearchResult) {
                    result.save(activity!!)
                    if (fetchedCount.incrementAndGet() == ids.size) {
                        addItems()
                        dismissProgress()
                    }
                }

                override fun onFailure(code: Int) {
                    if (fetchedCount.incrementAndGet() == ids.size) {
                        addItems()
                        dismissProgress()
                    }
                }
            })
        }
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {}

    override fun onResume() {
        super.onResume()

        if (deleteDialog) {
            deleteDialog()
        }
    }

    private fun enableSelectionMode() {
        selectionMode = true
        deleteView!!.visibility = View.VISIBLE
    }

    private fun disableSelectionMode() {
        selectionMode = false
        deleteView!!.visibility = View.INVISIBLE
    }

    private fun deleteDialog() {
        deleteDialog = true

        AlertDialog.Builder(activity!!)
                .setMessage(R.string.sure_question)
                .setPositiveButton(R.string.yes) { _, _ ->
                    for (result in selected) {
                        deleteResult(result)
                    }

                    server = null
                    selected.clear()
                    disableSelectionMode()
                    clearItems()
                    init(null)
                }
                .setNegativeButton(R.string.no, null)
                .setOnDismissListener { deleteDialog = false }.show()
    }

    private fun addItems() {
        for (id in ids) {
            val result = YoutubeSearchResult.restore(id, activity!!) ?: continue

            addItem(DownloadItem(result, object : DownloadItem.DownloadListener {
                override fun onClick(item: DownloadItem) {
                    if (selectionMode) {
                        item.toggleSelection()
                        if (selected.contains(result)) {
                            selected.remove(result)
                        } else {
                            selected.add(result)
                        }
                        if (selected.size == 0) {
                            disableSelectionMode()
                        }
                    } else {
                        musicManager!!.play(result)
                    }
                }

                override fun onLongClick(item: DownloadItem) {
                    if (!selectionMode) {
                        enableSelectionMode()
                        onClick(item)
                    }
                }

                override fun onDelete(item: DownloadItem) {
                    if (deleteResult(result)) {
                        removeItem(item)
                    }
                }
            }))
        }
    }
}
