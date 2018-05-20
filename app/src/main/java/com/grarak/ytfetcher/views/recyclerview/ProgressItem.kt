package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.RecyclerView

import com.grarak.ytfetcher.R

class ProgressItem(private val progressListener: ProgressListener) : RecyclerViewItem() {

    override val layoutXml = R.layout.item_progress

    interface ProgressListener {
        fun onBind()
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        progressListener.onBind()
    }
}
