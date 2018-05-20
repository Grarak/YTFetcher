package com.grarak.ytfetcher.views.recyclerview

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView

abstract class RecyclerViewItem {

    @get:LayoutRes
    abstract val layoutXml: Int

    abstract fun bindViewHolder(viewHolder: RecyclerView.ViewHolder)
}
