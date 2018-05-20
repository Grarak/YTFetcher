package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

class PlaylistIdItem(val result: YoutubeSearchResult,
                     private val playlistLinkListener: PlaylistLinkListener,
                     private val readOnly: Boolean) : RecyclerViewItem() {

    private var viewHolder: ViewHolder? = null

    override val layoutXml = R.layout.item_playlist_id

    private open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val downloaded: View = itemView.findViewById(R.id.downloaded_text)
        val menu: AppCompatImageView = itemView.findViewById(R.id.menu)
    }

    interface PlaylistLinkListener {
        fun onClick(item: PlaylistIdItem)

        fun onRemoveFromPlaylist(item: PlaylistIdItem)

        fun onDelete(item: PlaylistIdItem)

        fun onDownload(item: PlaylistIdItem)

        fun onMoveUp(item: PlaylistIdItem)

        fun onMoveDown(item: PlaylistIdItem)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        this.viewHolder = viewHolder as ViewHolder
        this.viewHolder!!.title.text = result.title

        viewHolder.itemView.setOnClickListener { playlistLinkListener.onClick(this) }

        this.viewHolder!!.menu.setOnClickListener { v ->
            val popupMenu = PopupMenu(v.context, v)
            val menu = popupMenu.menu
            if (!readOnly) {
                menu.add(0, 0, 0, R.string.remove_from_playlist)
            }
            if (this.viewHolder!!.downloaded.visibility == View.VISIBLE) {
                menu.add(0, 1, 0, R.string.delete)
            } else {
                menu.add(0, 2, 0, R.string.download)
            }
            if (!readOnly) {
                menu.add(0, 3, 0, R.string.move_up)
                menu.add(0, 4, 0, R.string.move_down)
            }
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> {
                        playlistLinkListener.onRemoveFromPlaylist(this)
                        return@setOnMenuItemClickListener true
                    }
                    1 -> {
                        playlistLinkListener.onDelete(this)
                        return@setOnMenuItemClickListener true
                    }
                    2 -> {
                        playlistLinkListener.onDownload(this)
                        return@setOnMenuItemClickListener true
                    }
                    3 -> {
                        playlistLinkListener.onMoveUp(this)
                        return@setOnMenuItemClickListener true
                    }
                    4 -> {
                        playlistLinkListener.onMoveDown(this)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popupMenu.show()
        }

        setDownloaded()
    }

    fun setDownloaded() {
        viewHolder?.run {
            downloaded.visibility = if (result.getDownloadPath(downloaded.context).exists())
                View.VISIBLE
            else
                View.GONE
        }
    }

    class Adapter(items: List<RecyclerViewItem>) : RecyclerViewAdapter(items) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : ViewHolder(LayoutInflater.from(
                    parent.context).inflate(
                    R.layout.item_playlist_id, parent, false)) {
            }
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }
    }
}
