package com.grarak.ytfetcher.views.recyclerview.settings

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem

class ButtonItem(private val onClickListener: View.OnClickListener) : RecyclerViewItem() {
    var text: CharSequence? = null
        set(value) {
            field = value
            setup()
        }
    var textColor = 0
        set(value) {
            field = value
            setup()
        }
    var backgroundColor = 0
        set(value) {
            field = value
            setup()
        }

    private var viewHolder: RecyclerView.ViewHolder? = null

    override val layoutXml = R.layout.item_button

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        this.viewHolder = viewHolder

        setup()
    }

    private fun setup() {
        viewHolder?.run {
            val textView = itemView.findViewById<TextView>(R.id.text)
            textView.text = text
            if (textColor != 0) {
                textView.setTextColor(textColor)
            }
            if (backgroundColor != 0) {
                textView.setBackgroundColor(backgroundColor)
            }

            itemView.setOnClickListener(onClickListener)
        }
    }
}
