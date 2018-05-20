package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.grarak.ytfetcher.R

open class TitleFragment : BaseFragment() {

    private var titleView: TextView? = null
    var title: CharSequence? = null
        set(value) {
            field = value
            titleView?.text = field
        }

    @LayoutRes
    protected open val layoutXml: Int = R.layout.fragment_title

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(layoutXml, container, false)
        titleView = rootView.findViewById(R.id.title)
        titleView?.text = title
        return rootView
    }
}
