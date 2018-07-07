package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.AddFragment
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import com.grarak.ytfetcher.views.recyclerview.MusicItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.util.*

class SearchFragment : RecyclerViewFragment<AddFragment>(), AddFragment.OnOpenListener, AddFragment.OnConfirmListener {

    private var title: String? = null
    private var server: YoutubeServer? = null

    override val titleFragmentClass: Class<AddFragment> = AddFragment::class.java

    override val emptyViewsMessage: String
        get() = getString(R.string.no_songs)

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun init(savedInstanceState: Bundle?) {
        if (server != null) return

        server = YoutubeServer(activity!!)
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {}

    override fun onViewFinished() {
        super.onViewFinished()

        onConfirm(Settings.getLastSearch(activity!!))
    }

    override fun onOpen(fragment: AddFragment) {
        fragment.text = title
    }

    override fun onResume() {
        super.onResume()

        val items = ArrayList(items)
        for (item in items) {
            (item as MusicItem).setDownloaded()
        }
    }

    override fun onDownloaded(result: YoutubeSearchResult) {
        super.onDownloaded(result)

        val items = ArrayList(items)
        for (item in items) {
            val musicItem = item as MusicItem
            if (musicItem.result == result) {
                musicItem.setDownloaded()
            }
        }
    }

    override fun onConfirm(text: CharSequence) {
        if (text.toString().isEmpty() || text.toString() == title) {
            return
        }

        val youtube = Youtube()
        youtube.apikey = user.apikey
        youtube.searchquery = text.toString()
        title = youtube.searchquery
        titleFragment!!.title = title

        server!!.close()
        Settings.setLastSearch(activity!!, title!!)
        showProgress()
        server!!.search(youtube, object : YoutubeServer.YoutubeResultsCallback {
            override fun onSuccess(youtubeSearchResults: List<YoutubeSearchResult>?) {
                if (!isAdded) return
                dismissProgress()
                clearItems()

                for (result in youtubeSearchResults!!) {
                    addItem(MusicItem(result, object : MusicItem.MusicListener {
                        override fun onClick(musicItem: MusicItem) {
                            musicManager!!.play(result)
                        }

                        override fun onAddPlaylist(musicItem: MusicItem) {
                            showPlaylistDialog(result)
                        }

                        override fun onDelete(musicItem: MusicItem) {
                            if (deleteResult(result)) {
                                musicItem.setDownloaded()
                            }
                        }

                        override fun onDownload(musicItem: MusicItem) {
                            queueDownload(result)
                        }
                    }, false))
                }
            }

            override fun onFailure(code: Int) {
                if (!isAdded) return
                dismissProgress()
                clearItems()
            }
        })
    }

    override fun setUpTitleFragment(fragment: AddFragment) {
        super.setUpTitleFragment(fragment)
        var title = this.title
        if (title == null) {
            title = getString(R.string.search)
        }
        fragment.title = title
        fragment.hint = getText(R.string.query)
        fragment.imageResource = R.drawable.ic_search
        fragment.onOpenListener = this
        fragment.onConfirmListener = this
    }

    override fun onDestroy() {
        super.onDestroy()

        server?.close()
    }
}
