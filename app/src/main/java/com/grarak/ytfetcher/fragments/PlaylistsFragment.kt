package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.AddFragment
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Status
import com.grarak.ytfetcher.utils.server.playlist.Playlist
import com.grarak.ytfetcher.utils.server.playlist.PlaylistResults
import com.grarak.ytfetcher.utils.server.playlist.PlaylistServer
import com.grarak.ytfetcher.utils.server.playlist.Playlists
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import com.grarak.ytfetcher.views.recyclerview.PlaylistHeaderItem
import com.grarak.ytfetcher.views.recyclerview.PlaylistItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.util.*

class PlaylistsFragment : RecyclerViewFragment<AddFragment>(), AddFragment.OnConfirmListener {

    private var server: PlaylistServer? = null

    override val titleFragmentClass: Class<AddFragment> = AddFragment::class.java

    override val emptyViewsMessage by lazy { getString(R.string.no_playlists) }

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun init(savedInstanceState: Bundle?) {
        if (server != null) return

        server = PlaylistServer(activity!!)
        fetchPlaylists()
    }

    private fun fetchPlaylists() {
        showProgress()
        server!!.list(user.apikey!!, object : PlaylistServer.PlaylistListCallback {
            override fun onSuccess(playlists: Playlists) {
                addViews(playlists)
            }

            override fun onFailure(code: Int) {
                addViews(Playlists.restore(requireActivity()))
            }

            private fun addViews(playlists: Playlists?) {
                if (!isAdded) return
                clearItems()
                dismissProgress()
                if (playlists == null) return

                for (playlist in playlists) {
                    addItem(playlist)
                }
                playlists.save(requireActivity())
            }
        })
    }

    private fun addItem(playlist: Playlist) {
        if (itemsSize() == 0) {
            addItem(PlaylistHeaderItem())
        }

        addItem(PlaylistItem(playlist, object : PlaylistItem.PlaylistListener {
            override fun onClick(item: PlaylistItem) {
                showPlaylist(playlist)
            }

            override fun onPublic(item: PlaylistItem, isPublic: Boolean) {
                playlist.apikey = user.apikey
                playlist.isPublic = isPublic

                server!!.setPublic(playlist, object : GenericCallback {
                    override fun onSuccess() {}

                    override fun onFailure(code: Int) {
                        if (!isAdded) return
                        item.setPublic(!isPublic)
                    }
                })
            }

            override fun onDelete(item: PlaylistItem) {
                playlist.apikey = user.apikey
                server!!.delete(playlist, object : GenericCallback {
                    override fun onSuccess() {
                        if (!isAdded) return
                        removeItem(item)

                        if (items.size == 1) {
                            clearItems()
                        }
                    }

                    override fun onFailure(code: Int) {
                        Utils.toast(R.string.server_offline, requireActivity())
                    }
                })
            }
        }))

        val playlists = ArrayList<String>()
        for (item in items) {
            if (item is PlaylistItem) {
                playlists.add(item.playlist.name!!)
            }
        }
        availablePlaylists = playlists
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {}

    override fun onConfirm(text: CharSequence) {
        if (text.toString().isEmpty()) return

        val playlist = Playlist()
        playlist.name = text.toString()
        playlist.apikey = user.apikey

        showProgress()
        server!!.create(playlist, object : GenericCallback {
            override fun onSuccess() {
                if (!isAdded) return
                dismissProgress()
                addItem(playlist)
            }

            override fun onFailure(code: Int) {
                if (!isAdded) return
                dismissProgress()
                if (code == Status.PlaylistIdAlreadyExists) {
                    Utils.toast(R.string.playlist_already_exists, requireActivity())
                } else {
                    Utils.toast(R.string.server_offline, requireActivity())
                }
            }
        })
    }

    private fun showPlaylist(playlist: Playlist) {
        showProgress()

        playlist.apikey = user.apikey
        server!!.listPlaylistIds(playlist, object : PlaylistServer.PlayListIdsCallback {
            override fun onSuccess(ids: List<String>) {
                if (ids.isEmpty()) {
                    dismissProgress()
                    Utils.toast(R.string.no_songs, requireActivity())
                    return
                }

                val server = YoutubeServer(requireActivity())
                val results = HashMap<String, YoutubeSearchResult?>()
                for (id in ids) {
                    val youtube = Youtube()
                    youtube.apikey = playlist.apikey
                    youtube.id = id

                    server.getInfo(youtube, object : YoutubeServer.YoutubeResultCallback {
                        override fun onSuccess(result: YoutubeSearchResult) {
                            checkCompletion(result)
                        }

                        override fun onFailure(code: Int) {
                            checkCompletion(null)
                        }

                        private fun checkCompletion(result: YoutubeSearchResult?) {
                            results[id] = result
                            if (results.size == ids.size) {
                                if (!isAdded) return
                                val playlistResults = PlaylistResults()
                                playlistResults.name = playlist.name
                                playlistResults.songs = ArrayList()
                                for (id in ids) {
                                    results[id]?.run {
                                        playlistResults.songs!!.add(this)
                                    }
                                }

                                playlistResults.save(activity!!)
                                showPlaylist(playlistResults)
                                dismissProgress()
                            }
                        }
                    })
                }
            }

            override fun onFailure(code: Int) {
                failure()
            }

            private fun failure() {
                if (!isAdded) return

                val playlistResults = PlaylistResults.restore(playlist.name!!, requireActivity())
                playlistResults?.run {
                    showPlaylist(this)
                    Utils.toast(R.string.server_offline, requireActivity())
                }
                dismissProgress()
            }

            private fun showPlaylist(playlistResults: PlaylistResults) {
                showForegroundFragment(
                        PlaylistIdsFragment.newInstance(user, playlistResults, false))
            }
        })
    }

    override fun onRemoveForeground() {
        super.onRemoveForeground()
        fetchPlaylists()
    }

    override fun setUpTitleFragment(fragment: AddFragment) {
        super.setUpTitleFragment(fragment)
        fragment.title = getString(R.string.your_playlists)
        fragment.hint = getString(R.string.name)
        fragment.onConfirmListener = this
    }

    override fun onDestroy() {
        super.onDestroy()

        server?.close()
    }
}
