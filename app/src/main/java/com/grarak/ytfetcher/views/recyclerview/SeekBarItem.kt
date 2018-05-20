package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.RecyclerView
import android.widget.SeekBar
import android.widget.TextView
import com.grarak.ytfetcher.R
import java.util.*

class SeekBarItem(private val listener: SeekBarListener) : RecyclerViewItem() {

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
    var current: Int = 0
        set(value) {
            field = value
            setup()
        }

    var enabled = true
        set(value) {
            field = value
            setup()
        }

    override val layoutXml: Int = R.layout.item_seekbar

    private val onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            val progressText = viewHolder!!.itemView.findViewById<TextView>(R.id.progress)
            progressText.text = items[progress]
            if (fromUser) {
                current = progress
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            listener.onSeekStop(this@SeekBarItem,
                    Collections.unmodifiableList(items), current)
        }
    }

    interface SeekBarListener {
        fun onSeekStop(item: SeekBarItem, items: List<String>, position: Int)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        this.viewHolder = viewHolder
        setup()
    }

    private fun setup() {
        viewHolder?.run {
            val titleText = itemView.findViewById<TextView>(R.id.title)
            val progressText = itemView.findViewById<TextView>(R.id.progress)
            val seekBar = itemView.findViewById<AppCompatSeekBar>(R.id.seekbar)

            titleText.text = title
            progressText.text = items[current]

            seekBar.setOnSeekBarChangeListener(null)
            seekBar.max = items.size - 1
            seekBar.progress = current
            seekBar.isEnabled = enabled
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
        }
    }
}
