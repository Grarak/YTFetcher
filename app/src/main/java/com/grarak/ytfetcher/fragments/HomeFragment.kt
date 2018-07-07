package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.TitleFragment
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeCharts
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import com.grarak.ytfetcher.views.recyclerview.MusicItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.util.*

class HomeFragment : RecyclerViewFragment<TitleFragment>() {

    private var server: YoutubeServer? = null

    override val titleFragmentClass: Class<TitleFragment> = TitleFragment::class.java

    override val emptyViewsMessage: String
        get() = getString(R.string.charts_failed)

    override fun createLayoutManager(): LinearLayoutManager {
        return GridLayoutManager(activity, 2)
    }

    override fun init(savedInstanceState: Bundle?) {
        if (server != null) return

        showProgress()
        server = YoutubeServer(activity!!)

        val youtube = Youtube()
        youtube.apikey = user.apikey
        server!!.getCharts(youtube, object : YoutubeServer.YoutubeChartsCallback {

            override fun onSuccess(youtubeCharts: YoutubeCharts) {
                addViews(youtubeCharts)
            }

            override fun onFailure(code: Int) {
                addViews()
            }

            private fun addViews(youtubeCharts: YoutubeCharts = YoutubeCharts.restore(activity!!)) {
                if (!isAdded) return
                dismissProgress()
                for (result in youtubeCharts) {
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
                    }, true))
                }
                youtubeCharts.save(requireActivity())
            }
        })
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {}

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

    override fun setUpTitleFragment(fragment: TitleFragment) {
        super.setUpTitleFragment(fragment)
        fragment.title = getString(R.string.popular_music)
    }

    override fun onDestroy() {
        super.onDestroy()

        server?.close()
    }
}
