package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import com.grarak.ytfetcher.MainActivity
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Status
import com.grarak.ytfetcher.utils.server.playlist.*
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.views.recyclerview.PlaylistIdItem
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewAdapter
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.util.*

class PlaylistIdsFragment : RecyclerViewFragment<PlayFragment>() {
    companion object {
        fun newInstance(
                user: User,
                playlistResults: PlaylistResults,
                readOnly: Boolean): PlaylistIdsFragment {
            val args = Bundle()
            args.putSerializable(MainActivity.USER_INTENT, user)
            args.putSerializable("playlistResults", playlistResults)
            args.putBoolean("readOnly", readOnly)
            val fragment = PlaylistIdsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var playlistResults: PlaylistResults? = null
    private var readOnly: Boolean = false
    private var server: PlaylistServer? = null

    private var saveName: String? = null

    override val layoutXml: Int = R.layout.fragment_playlist_id

    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return ItemTouchHelper.Callback.makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                    ItemTouchHelper.DOWN or ItemTouchHelper.UP)
        }

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            move(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    })

    override val titleFragmentClass: Class<PlayFragment> = PlayFragment::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistResults = arguments!!.getSerializable("playlistResults") as PlaylistResults
        readOnly = arguments!!.getBoolean("readOnly")
        server = PlaylistServer(activity!!)
    }

    override fun createAdapter(): RecyclerViewAdapter {
        return PlaylistIdItem.Adapter(items)
    }

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun init(savedInstanceState: Bundle?) {
        if (!readOnly) {
            touchHelper.attachToRecyclerView(recyclerView)
        }
    }

    private fun move(oldPosition: Int, newPosition: Int) {
        Collections.swap(playlistResults!!.songs!!, oldPosition, newPosition)
        Collections.swap(items, oldPosition, newPosition)
        recyclerViewAdapter!!.notifyItemMoved(oldPosition, newPosition)
    }

    override fun initItems(items: ArrayList<RecyclerViewItem>) {
        for (result in playlistResults!!.songs!!) {
            items.add(PlaylistIdItem(result, object : PlaylistIdItem.PlaylistLinkListener {
                override fun onClick(item: PlaylistIdItem) {
                    musicManager!!.play(result)
                }

                override fun onRemoveFromPlaylist(item: PlaylistIdItem) {
                    val playlistId = PlaylistId()
                    playlistId.apikey = user!!.apikey
                    playlistId.name = playlistResults!!.name
                    playlistId.id = result.id
                    server!!.deleteFromPlaylist(playlistId, object : GenericCallback {
                        override fun onSuccess() {
                            if (!isAdded) return
                            val index = playlistResults!!.songs!!.indexOf(result)
                            playlistResults!!.songs!!.removeAt(index)
                            removeItem(index)

                            if (this@PlaylistIdsFragment.items.size == 0) {
                                removeForegroundFragment(this@PlaylistIdsFragment)
                            }
                        }

                        override fun onFailure(code: Int) {
                            Utils.toast(R.string.server_offline, activity!!)
                        }
                    })
                }

                override fun onDelete(item: PlaylistIdItem) {
                    if (deleteResult(result)) {
                        item.setDownloaded()
                    }
                }

                override fun onDownload(item: PlaylistIdItem) {
                    queueDownload(result)
                }

                override fun onMoveUp(item: PlaylistIdItem) {
                    val position = this@PlaylistIdsFragment.items.indexOf(item)
                    if (position > 0) {
                        move(position, position - 1)
                    }
                }

                override fun onMoveDown(item: PlaylistIdItem) {
                    val position = this@PlaylistIdsFragment.items.indexOf(item)
                    if (position < this@PlaylistIdsFragment.items.size) {
                        move(position, position + 1)
                    }
                }
            }, readOnly))
        }
    }

    override fun onResume() {
        super.onResume()

        val items = ArrayList(items)
        for (item in items) {
            (item as PlaylistIdItem).setDownloaded()
        }

        saveName?.run {
            showSaveDialog(this)
        }
    }

    override fun onDownloaded(result: YoutubeSearchResult) {
        super.onDownloaded(result)

        val items = ArrayList(items)
        for (item in items) {
            val playlistIdItem = item as PlaylistIdItem
            if (playlistIdItem.result == result) {
                playlistIdItem.setDownloaded()
            }
        }
    }

    override fun setUpTitleFragment(fragment: PlayFragment) {
        super.setUpTitleFragment(fragment)
        fragment.title = playlistResults!!.name
        fragment.readyOnly = readOnly
        fragment.playListener = object : PlayFragment.PlayListener {
            override fun onPlay() {
                musicManager?.play(playlistResults!!.songs!!, 0)
            }

            override fun onShuffle() {
                val results = ArrayList(playlistResults!!.songs!!)
                results.shuffle()
                musicManager!!.play(results, 0)
            }

            override fun onDownload() {
                for (result in playlistResults!!.songs!!) {
                    queueDownload(result)
                }
            }

            override fun onSave() {
                showSaveDialog("")
            }
        }
    }

    private fun showSaveDialog(name: String) {
        saveName = name

        val layout = FrameLayout(activity!!)
        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                16f, resources.displayMetrics).toInt()
        layout.setPadding(padding, padding / 2, padding, padding / 2)

        val editText = EditText(activity)
        layout.addView(editText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT))
        editText.setText(saveName)
        editText.setSelection(saveName!!.length)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                saveName = s.toString()
            }
        })

        AlertDialog.Builder(activity!!)
                .setTitle(R.string.name)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val playlist = Playlist()
                    playlist.apikey = user!!.apikey
                    playlist.name = editText.text.toString()
                    server!!.create(playlist, object : GenericCallback {
                        override fun onSuccess() {
                            server!!.setPlaylistIds(createPlaylistIds(playlist.name), object : GenericCallback {
                                override fun onSuccess() {}

                                override fun onFailure(code: Int) {
                                    Utils.toast(R.string.server_offline, activity!!)
                                }
                            })
                        }

                        override fun onFailure(code: Int) {
                            if (code == Status.PlaylistIdAlreadyExists) {
                                Utils.toast(R.string.playlist_already_exists, activity!!)
                            } else {
                                Utils.toast(R.string.server_offline, activity!!)
                            }
                        }
                    })
                }
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener { saveName = null }.show()
    }

    override fun onPause() {
        super.onPause()
        server?.close()

        if (!readOnly) {
            server?.setPlaylistIds(createPlaylistIds(playlistResults!!.name), object : GenericCallback {
                override fun onSuccess() {}

                override fun onFailure(code: Int) {}
            })
        }
    }

    private fun createPlaylistIds(name: String?): PlaylistIds {
        val playlistIds = PlaylistIds()
        playlistIds.apikey = user!!.apikey
        playlistIds.name = name
        playlistIds.ids = ArrayList()
        for (result in playlistResults!!.songs!!) {
            playlistIds.ids!!.add(result.id!!)
        }
        return playlistIds
    }
}
