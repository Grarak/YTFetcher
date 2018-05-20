package com.grarak.ytfetcher.views.recyclerview

import android.support.v4.content.ContextCompat
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

class DownloadItem(private val result: YoutubeSearchResult, private val downloadListener: DownloadListener) : RecyclerViewItem() {

    private var viewHolder: RecyclerView.ViewHolder? = null
    private var selected = false

    override val layoutXml = R.layout.item_download

    interface DownloadListener {
        fun onClick(item: DownloadItem)

        fun onLongClick(item: DownloadItem)

        fun onDelete(item: DownloadItem)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        this.viewHolder = viewHolder

        val text = viewHolder.itemView.findViewById<TextView>(R.id.title)
        text.text = result.title

        viewHolder.itemView.findViewById<View>(R.id.menu).setOnClickListener({ v ->
            if (selected) return@setOnClickListener

            val popupMenu = PopupMenu(v.context, v)
            val menu = popupMenu.menu
            menu.add(0, 0, 0, R.string.delete)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> {
                        downloadListener.onDelete(this)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popupMenu.show()
        })

        viewHolder.itemView.setOnClickListener { downloadListener.onClick(this) }
        viewHolder.itemView.setOnLongClickListener {
            downloadListener.onLongClick(this)
            true
        }

        setup()
    }

    fun toggleSelection() {
        selected = !selected
        setup()
    }

    private fun setup() {
        viewHolder?.run {
            if (selected) {
                itemView.setBackgroundColor(ContextCompat.getColor(
                        itemView.context, R.color.semi_transparent))
            } else {
                itemView.setBackgroundColor(0)
            }
        }
    }
}
