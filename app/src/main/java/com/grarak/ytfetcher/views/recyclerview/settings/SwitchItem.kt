package com.grarak.ytfetcher.views.recyclerview.settings

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.TextView
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem

class SwitchItem(private val switchListener: SwitchListener) : RecyclerViewItem() {

    var text: CharSequence? = null
        set(value) {
            field = value
            setup()
        }
    var checked: Boolean = false
        set(value) {
            field = value
            setup()
        }

    private var viewHolder: RecyclerView.ViewHolder? = null

    override val layoutXml = R.layout.item_switch

    interface SwitchListener {
        fun onCheckedChanged(checked: Boolean)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        this.viewHolder = viewHolder

        val switchCompat = viewHolder.itemView.findViewById<SwitchCompat>(R.id.switch_compat)
        viewHolder.itemView.setOnClickListener {
            checked = !switchCompat.isChecked
            switchCompat.isChecked = checked
            switchListener.onCheckedChanged(checked)
        }

        setup()
    }

    private fun setup() {
        viewHolder?.run {
            (itemView.findViewById<View>(R.id.text) as TextView).text = text
            (itemView.findViewById<View>(R.id.switch_compat) as SwitchCompat).isChecked = checked
        }
    }
}
