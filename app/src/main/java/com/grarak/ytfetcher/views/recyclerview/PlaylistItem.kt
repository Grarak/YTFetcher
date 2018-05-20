package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.server.playlist.Playlist

class PlaylistItem(val playlist: Playlist, private val playlistListener: PlaylistListener) : RecyclerViewItem() {

    private var publicSwitch: SwitchCompat? = null

    override val layoutXml = R.layout.item_playlist

    private val publicSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        playlistListener.onPublic(this@PlaylistItem, isChecked)
    }

    interface PlaylistListener {
        fun onClick(item: PlaylistItem)

        fun onPublic(item: PlaylistItem, isPublic: Boolean)

        fun onDelete(item: PlaylistItem)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val title = viewHolder.itemView.findViewById<TextView>(R.id.title)
        publicSwitch = viewHolder.itemView.findViewById(R.id.public_switch)
        val menu = viewHolder.itemView.findViewById<View>(R.id.menu)

        title.text = playlist.name
        publicSwitch!!.isChecked = playlist.isPublic

        viewHolder.itemView.setOnClickListener { playlistListener.onClick(this) }
        publicSwitch!!.setOnCheckedChangeListener(publicSwitchListener)

        viewHolder.itemView.setOnLongClickListener {
            menu.performLongClick()
            true
        }
        menu.setOnClickListener { v ->
            val popupMenu = PopupMenu(v.context, v)
            val menu1 = popupMenu.menu
            menu1.add(0, 0, 0, R.string.delete)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> {
                        playlistListener.onDelete(this)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popupMenu.show()
        }
    }

    fun setPublic(isPublic: Boolean) {
        publicSwitch!!.setOnCheckedChangeListener(null)
        publicSwitch!!.isChecked = isPublic
        publicSwitch!!.setOnCheckedChangeListener(publicSwitchListener)
    }
}
