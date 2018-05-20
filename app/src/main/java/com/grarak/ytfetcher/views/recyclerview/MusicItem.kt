package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

class MusicItem(val result: YoutubeSearchResult,
                private val musicListener: MusicListener,
                private val grid: Boolean) : RecyclerViewItem() {

    private var downloaded: View? = null

    override val layoutXml
        get() = if (grid) R.layout.item_music_grid else R.layout.item_music

    interface MusicListener {
        fun onClick(musicItem: MusicItem)

        fun onAddPlaylist(musicItem: MusicItem)

        fun onDelete(musicItem: MusicItem)

        fun onDownload(musicItem: MusicItem)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val thumbnail = viewHolder.itemView.findViewById<ImageView>(R.id.thumbnail)
        downloaded = viewHolder.itemView.findViewById(R.id.downloaded_text)
        val title = viewHolder.itemView.findViewById<TextView>(R.id.title)
        val summary = viewHolder.itemView.findViewById<TextView>(R.id.summary)
        val menu = viewHolder.itemView.findViewById<AppCompatImageView>(R.id.menu)

        viewHolder.itemView.setOnClickListener { musicListener.onClick(this) }
        viewHolder.itemView.setOnLongClickListener {
            menu.performClick()
            true
        }
        title.text = result.title
        summary.text = result.duration

        Glide.with(thumbnail)
                .load(result.thumbnail)
                .into(thumbnail)

        menu.setOnClickListener { v ->
            val popupMenu = PopupMenu(v.context, v)
            val menu1 = popupMenu.menu
            menu1.add(0, 0, 0, R.string.add_playlist)
            if (downloaded!!.visibility == View.VISIBLE) {
                menu1.add(0, 1, 0, R.string.delete)
            } else {
                menu1.add(0, 2, 0, R.string.download)
            }
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> {
                        musicListener.onAddPlaylist(this)
                        return@setOnMenuItemClickListener true
                    }
                    1 -> {
                        musicListener.onDelete(this)
                        return@setOnMenuItemClickListener true
                    }
                    2 -> {
                        musicListener.onDownload(this)
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
        downloaded?.visibility = if (result.getDownloadPath(downloaded!!.context).exists())
            View.VISIBLE
        else
            View.INVISIBLE
    }
}
