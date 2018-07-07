package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.TitleFragment
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.playlist.*
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.user.UserServer
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import com.grarak.ytfetcher.views.recyclerview.ProgressItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import com.grarak.ytfetcher.views.recyclerview.UserItem
import java.util.*

class UsersFragment : RecyclerViewFragment<TitleFragment>() {

    private var userServer: UserServer? = null
    private var playlistServer: PlaylistServer? = null
    private var youtubeServer: YoutubeServer? = null
    private var page: Int = 0
    private var limitReached: Boolean = false
    private var loading: Boolean = false

    private var userPlaylist: User? = null
    private var playlists: Playlists? = null

    private val progressItem = ProgressItem(object : ProgressItem.ProgressListener {
        override fun onBind() {
            loadNextPage()
        }
    })

    override val emptyViewsMessage: String?
        get() = getString(R.string.no_users)

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(requireActivity())
    }

    override fun init(savedInstanceState: Bundle?) {
        if (userServer != null) return

        userServer = UserServer(requireActivity())
        playlistServer = PlaylistServer(requireActivity())
        youtubeServer = YoutubeServer(requireActivity())
        loadNextPage()
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {}

    private fun loadNextPage() {
        if (limitReached || loading) return

        loading = true
        userServer!!.list(user, ++page, object : UserServer.UsersCallback {
            override fun onSuccess(users: List<User>) {
                if (!isAdded) return

                removeItem(progressItem)
                if (users.isEmpty()) {
                    limitReached = true
                    return
                }

                for (user in users) {
                    addItem(UserItem(this@UsersFragment.user.admin, user, object : UserItem.UserListener {
                        override fun onClick(item: UserItem) {
                            if (user.name == this@UsersFragment.user.name) return

                            showProgress()

                            val playlist = Playlist()
                            playlist.apikey = this@UsersFragment.user.apikey
                            playlist.name = user.name

                            playlistServer!!.listPublic(playlist, object : PlaylistServer.PlaylistListCallback {
                                override fun onSuccess(playlists: Playlists) {
                                    if (!isAdded) return
                                    dismissProgress()
                                    if (playlists.items.size == 0) {
                                        Utils.toast(R.string.no_public_playlists, activity!!)
                                        return
                                    }
                                    showPlaylists(user, playlists)
                                }

                                override fun onFailure(code: Int) {
                                    if (!isAdded) return
                                    dismissProgress()
                                    Utils.toast(R.string.server_offline, activity!!)
                                }
                            })
                        }

                        override fun onVerified(item: UserItem, verified: Boolean) {
                            val newUser = User()
                            newUser.apikey = this@UsersFragment.user.apikey
                            newUser.name = user.name
                            newUser.verified = verified

                            userServer!!.setVerification(newUser, object : GenericCallback {
                                override fun onSuccess() {}

                                override fun onFailure(code: Int) {
                                    if (!isAdded) return
                                    item.setVerified(!verified)
                                }
                            })
                        }
                    }))
                }
                loading = false
                addItem(progressItem)
            }

            override fun onFailure(code: Int) {
                if (!isAdded) return

                limitReached = true
                removeItem(progressItem)
                Utils.toast(R.string.server_offline, activity!!)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (userPlaylist != null && playlists != null) {
            showPlaylists(userPlaylist!!, playlists!!)
        }
    }

    private fun showPlaylists(user: User, playlists: Playlists) {
        userPlaylist = user
        this.playlists = playlists

        val playlistNames = ArrayList<String>()
        for (playlist in playlists) {
            playlistNames.add(playlist.name!!)
        }

        AlertDialog.Builder(activity!!)
                .setTitle(R.string.public_playlists)
                .setItems(playlistNames.toTypedArray()) { _, which ->
                    showProgress()

                    val playlistPublic = PlaylistPublic()
                    playlistPublic.apikey = this@UsersFragment.user.apikey
                    playlistPublic.name = user.name
                    playlistPublic.playlist = playlistNames[which]
                    playlistServer!!.listPlaylistIdsPublic(playlistPublic, object : PlaylistServer.PlayListIdsCallback {
                        override fun onSuccess(ids: List<String>) {
                            if (ids.isEmpty()) {
                                dismissProgress()
                                Utils.toast(R.string.no_songs, activity!!)
                                return
                            }

                            val results = HashMap<String, YoutubeSearchResult>()
                            for (id in ids) {
                                val youtube = Youtube()
                                youtube.apikey = playlistPublic.apikey
                                youtube.id = id

                                youtubeServer!!.getInfo(youtube, object : YoutubeServer.YoutubeResultCallback {
                                    override fun onSuccess(result: YoutubeSearchResult) {
                                        synchronized(results) {
                                            results[id] = result

                                            if (results.size == ids.size) {
                                                if (!isAdded) return
                                                val playlistResults = PlaylistResults()
                                                playlistResults.name = playlistPublic.playlist
                                                playlistResults.songs = ArrayList()
                                                for (id in ids) {
                                                    playlistResults.songs!!.add(results[id]!!)
                                                }

                                                playlistResults.save(activity!!)
                                                showPlaylist(playlistResults)
                                                dismissProgress()
                                            }
                                        }
                                    }

                                    override fun onFailure(code: Int) {
                                        if (!isAdded) return

                                        youtubeServer!!.close()
                                    }
                                })
                            }
                        }

                        override fun onFailure(code: Int) {
                            if (!isAdded) return
                            dismissProgress()
                            Utils.toast(R.string.server_offline, activity!!)
                        }

                        private fun showPlaylist(playlistResults: PlaylistResults) {
                            showForegroundFragment(
                                    PlaylistIdsFragment.newInstance(this@UsersFragment.user,
                                            playlistResults, true))
                        }
                    })
                }
                .setOnDismissListener { _ ->
                    userPlaylist = null
                    this.playlists = null
                }.show()
    }

    override fun onDestroy() {
        super.onDestroy()

        userServer?.close()
    }
}
