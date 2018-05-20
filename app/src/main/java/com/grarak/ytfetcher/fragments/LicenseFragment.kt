package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

class LicenseFragment : BaseFragment() {

    private var webView: WebView? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        webView = WebView(activity)
        webView!!.loadUrl("file:///android_asset/licenses.html")

        if (savedInstanceState != null) {
            webView!!.scrollTo(savedInstanceState.getInt("scrollX"),
                    savedInstanceState.getInt("scrollY"))
        }

        return webView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("scrollX", webView!!.scrollX)
        outState.putInt("scrollY", webView!!.scrollY)
    }
}
