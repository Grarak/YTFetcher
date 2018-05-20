package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.grarak.ytfetcher.MainActivity
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.history.HistoryServer
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import com.grarak.ytfetcher.views.recyclerview.MusicItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.util.*

class HistoryFragment : RecyclerViewFragment<TitleFragment>() {

    companion object {
        fun newInstance(user: User): HistoryFragment {
            val args = Bundle()
            args.putSerializable(MainActivity.USER_INTENT, user)
            val fragment = HistoryFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var server: HistoryServer? = null

    override val layoutXml: Int = R.layout.fragment_history

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun init(savedInstanceState: Bundle?) {
        if (server != null) return

        showProgress()
        server = HistoryServer(activity!!)
        server!!.httpGet(user!!.apikey!!, object : HistoryServer.HistoryCallback {
            override fun onSuccess(history: List<String>) {
                if (history.isEmpty()) {
                    dismissProgress()
                    Utils.toast(R.string.no_history, activity!!)
                    return
                }

                val server = YoutubeServer(activity!!)
                val results = HashMap<String, YoutubeSearchResult>()
                for (id in history) {
                    val youtube = Youtube()
                    youtube.apikey = user!!.apikey
                    youtube.id = id

                    server.getInfo(youtube, object : YoutubeServer.YoutubeResultCallback {
                        override fun onSuccess(result: YoutubeSearchResult) {
                            synchronized(results) {
                                results[id] = result

                                if (results.size == history.size) {
                                    if (!isAdded) return

                                    for (id in history) {
                                        addItem(MusicItem(results[id]!!, object : MusicItem.MusicListener {
                                            override fun onClick(musicItem: MusicItem) {
                                                musicManager!!.play(musicItem.result)
                                            }

                                            override fun onAddPlaylist(musicItem: MusicItem) {
                                                showPlaylistDialog(musicItem.result)
                                            }

                                            override fun onDelete(musicItem: MusicItem) {
                                                if (deleteResult(musicItem.result)) {
                                                    musicItem.setDownloaded()
                                                }
                                            }

                                            override fun onDownload(musicItem: MusicItem) {
                                                queueDownload(musicItem.result)
                                            }
                                        }, false))
                                    }

                                    dismissProgress()
                                }
                            }
                        }

                        override fun onFailure(code: Int) {
                            failure()
                            server.close()
                        }
                    })
                }
            }

            override fun onFailure(code: Int) {
                failure()
            }

            private fun failure() {
                if (!isAdded) return
                Utils.toast(R.string.server_offline, activity!!)
                removeForegroundFragment(this@HistoryFragment)
            }
        })
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {}

    override fun onDestroy() {
        super.onDestroy()

        server?.close()
    }
}
