package com.grarak.ytfetcher.fragments

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.grarak.ytfetcher.R

class PlayFragment : TitleFragment() {

    var playListener: PlayListener? = null
    var readyOnly: Boolean = false
        set(value) {
            field = value
            setDownloadButton()
        }
    private var downloadButton: FloatingActionButton? = null
    private var downloadDrawable: Drawable? = null
    private var saveDrawable: Drawable? = null

    override val layoutXml: Int = R.layout.fragment_play

    interface PlayListener {
        fun onPlay()

        fun onShuffle()

        fun onDownload()

        fun onSave()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)

        downloadButton = rootView!!.findViewById(R.id.download)
        setDownloadButton()
        downloadButton!!.setOnClickListener {
            if (readyOnly) {
                playListener?.onSave()
            } else {
                playListener?.onDownload()
            }
        }
        rootView.findViewById<View>(R.id.shuffle).setOnClickListener {
            playListener?.onShuffle()
        }
        rootView.findViewById<View>(R.id.play).setOnClickListener {
            playListener?.onPlay()
        }

        return rootView
    }

    private fun setDownloadButton() {
        downloadButton?.run {
            if (saveDrawable == null) {
                saveDrawable = ContextCompat.getDrawable(activity!!, R.drawable.ic_save)
                DrawableCompat.setTint(saveDrawable!!, Color.WHITE)
            }

            if (downloadDrawable == null) {
                downloadDrawable = ContextCompat.getDrawable(activity!!, R.drawable.ic_download)
                DrawableCompat.setTint(downloadDrawable!!, Color.WHITE)
            }
            setImageDrawable(if (readyOnly) saveDrawable else downloadDrawable)
        }
    }
}
