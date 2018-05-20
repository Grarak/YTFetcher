package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.grarak.ytfetcher.R
import java.util.*

class SelectorItem(private val listener: SelectorListener) : RecyclerViewItem() {

    private var viewHolder: RecyclerView.ViewHolder? = null
    var title: CharSequence? = null
        set(value) {
            field = value
            setup()
        }
    var items = ArrayList<String>()
        set(value) {
            field.clear()
            field.addAll(value)
            setup()
        }
    var position: Int = 0
        set(value) {
            field = value
            setup()
            listener.onItemSelected(this, Collections.unmodifiableList(items), value)
        }

    override val layoutXml: Int
        get() = R.layout.item_selector

    interface SelectorListener {
        fun onItemSelected(item: SelectorItem, items: List<String>, position: Int)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        this.viewHolder = viewHolder

        val itemView = viewHolder.itemView.findViewById<TextView>(R.id.selected_item)
        viewHolder.itemView.setOnClickListener { v ->
            val popupMenu = PopupMenu(v.context, itemView)
            val menu = popupMenu.menu
            for (i in items.indices) {
                menu.add(0, i, 0, items[i])
            }
            popupMenu.setOnMenuItemClickListener { item ->
                position = item.itemId
                itemView.text = items[position]
                listener.onItemSelected(this, Collections.unmodifiableList(items), position)

                true
            }
            popupMenu.show()
        }

        setup()
    }

    private fun setup() {
        viewHolder?.let {
            val title = it.itemView.findViewById<TextView>(R.id.title)
            val item = it.itemView.findViewById<TextView>(R.id.selected_item)

            title.text = this.title
            item.text = items[position]
        }
    }
}
