package com.grarak.ytfetcher.fragments

import android.animation.ValueAnimator
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewAdapter
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import java.lang.ref.WeakReference

abstract class RecyclerViewFragment<TF : BaseFragment> : BaseFragment() {

    protected val items = ArrayList<RecyclerViewItem>()
    protected var recyclerViewAdapter: RecyclerViewAdapter? = null
        private set

    protected var rootView: View? = null
        private set
    protected var recyclerView: RecyclerView? = null
        private set
    private var layoutManager: LinearLayoutManager? = null
    private var messageView: TextView? = null
    private var progressView: View? = null
    private var firstVisibleItem: Int = 0

    private var progressCount: Int = 0

    var titleFragment: TF? = null
        get() = childFragmentManager.findFragmentByTag("title_fragment") as? TF

    private var titleContent: View? = null

    private var itemsLoader: ItemsLoader<TF>? = null

    protected open val layoutXml: Int = R.layout.fragment_recyclerview

    private val onScrollListener = OnScrollListener()

    protected open val titleFragmentClass: Class<TF>? = null

    protected open val emptyViewsMessage: String? = null

    protected open fun createAdapter(): RecyclerViewAdapter {
        return RecyclerViewAdapter(items)
    }

    protected abstract fun createLayoutManager(): LinearLayoutManager

    private inner class OnScrollListener : RecyclerView.OnScrollListener() {
        private var scrollDistance: Int = 0
        private var bottomTranslation: Int = 0

        private fun getScrollDistance(): Int {
            return recyclerView!!.computeVerticalScrollOffset()
        }

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            scrollDistance = getScrollDistance()
            bottomTranslation += dy
            setTranslations()

            firstVisibleItem = layoutManager!!.findLastCompletelyVisibleItemPosition()
        }

        private fun setTranslations() {
            if (titleContent != null) {
                titleContent!!.translationY = (-scrollDistance / 2).toFloat()
            }

            if (bottomNavigationView == null || recyclerView!!.paddingBottom == 0) {
                return
            }
            if (bottomTranslation > bottomNavigationView!!.height) {
                bottomTranslation = bottomNavigationView!!.height
            } else if (bottomTranslation < 0) {
                bottomTranslation = 0
            }
            bottomNavigationView!!.translationY = bottomTranslation.toFloat()
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            if (bottomNavigationView == null || newState != 0 || bottomTranslation == 0
                    || recyclerView!!.paddingBottom == 0) {
                return
            }

            animateBottomNavigation(
                    layoutManager!!.findLastVisibleItemPosition() == itemsSize() - 1 || bottomTranslation < bottomNavigationView!!.height * 0.5f)
        }

        fun animateBottomNavigation(show: Boolean) {
            val animator = ValueAnimator.ofInt(bottomTranslation,
                    if (show) 0 else bottomNavigationView!!.height)
            animator.addUpdateListener { animation ->
                bottomTranslation = animation.animatedValue as Int
                if (bottomNavigationView != null) {
                    bottomNavigationView!!.translationY = bottomTranslation.toFloat()
                }
            }
            animator.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        rootView = inflater.inflate(layoutXml, container, false)

        recyclerView = rootView!!.findViewById(R.id.recyclerview)
        recyclerView!!.setHasFixedSize(true)

        layoutManager = createLayoutManager()
        recyclerView!!.layoutManager = layoutManager
        if (recyclerViewAdapter == null) {
            recyclerViewAdapter = createAdapter()
        }
        recyclerView!!.adapter = recyclerViewAdapter

        messageView = rootView!!.findViewById(R.id.message)
        progressView = rootView!!.findViewById(R.id.progress)
        if (messageView != null) {
            messageView!!.text = emptyViewsMessage
        }

        titleContent = rootView!!.findViewById(R.id.content_title)

        var titleFragment = titleFragment
        if (titleFragment == null && titleFragmentClass != null) {
            titleFragment = Fragment.instantiate(activity, titleFragmentClass!!.name) as TF
        }
        titleFragment?.let {
            setUpTitleFragment(it)
            childFragmentManager.beginTransaction()
                    .replace(R.id.content_title, it, "title_fragment").commit()
        }

        init(savedInstanceState)

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("progress_visible")) {
                showProgress()
                synchronized(this) {
                    progressCount--
                }
            } else {
                dismissProgress()
            }
            post()
        } else if (itemsLoader == null) {
            showProgress()
            itemsLoader = ItemsLoader(this)
            itemsLoader!!.execute()
        }
        return rootView
    }

    private class ItemsLoader<TF : BaseFragment> constructor(fragment: RecyclerViewFragment<TF>)
        : AsyncTask<Void, Void, List<RecyclerViewItem>>() {

        private val fragmentRef: WeakReference<RecyclerViewFragment<TF>> = WeakReference(fragment)

        override fun doInBackground(vararg voids: Void): List<RecyclerViewItem>? {
            val fragment = fragmentRef.get()
            if (fragment != null) {
                val items = ArrayList<RecyclerViewItem>()
                fragment.initItems(items)
                return items
            }
            return null
        }

        override fun onPostExecute(recyclerViewItems: List<RecyclerViewItem>?) {
            super.onPostExecute(recyclerViewItems)

            val fragment = fragmentRef.get()
            if (fragment != null && recyclerViewItems != null) {
                fragment.items.addAll(recyclerViewItems)
                fragment.recyclerViewAdapter!!.notifyDataSetChanged()
                fragment.post()
                fragment.dismissProgress()
            }
        }
    }

    private fun post() {
        if (messageView != null) {
            messageView!!.visibility = if (progressView!!.visibility == View.INVISIBLE && itemsSize() == 0)
                View.VISIBLE
            else
                View.INVISIBLE
        }
    }

    override fun onViewFinished() {
        var leftPadding = recyclerView!!.paddingLeft
        var rightPadding = recyclerView!!.paddingRight
        if (Utils.isLandscape(activity!!)) {
            leftPadding = resources.getDimensionPixelSize(
                    R.dimen.recyclerview_padding)
            rightPadding = leftPadding
        }

        var titleHeight = 0
        if (titleContent != null) {
            titleHeight = titleContent!!.height
        }
        recyclerView!!.setPadding(
                leftPadding,
                recyclerView!!.paddingTop + titleHeight,
                rightPadding,
                recyclerView!!.paddingBottom
        )

        recyclerView!!.scrollToPosition(firstVisibleItem)
        if (!recyclerView!!.clipToPadding) {
            recyclerView!!.addOnScrollListener(onScrollListener)
        }

        if (titleContent != null) {
            recyclerView!!.setOnTouchListener { _, event ->
                if (progressView == null || progressView!!.visibility == View.INVISIBLE) {
                    titleContent!!.dispatchTouchEvent(event)
                }
                false
            }
        }
    }

    protected open fun setUpTitleFragment(fragment: TF) {}

    protected abstract fun init(savedInstanceState: Bundle?)

    protected abstract fun initItems(items: ArrayList<RecyclerViewItem>)

    protected fun addItem(item: RecyclerViewItem) {
        items.add(item)
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.notifyItemInserted(items.size - 1)
        }
        if (messageView != null) {
            messageView!!.visibility = View.INVISIBLE
        }
    }

    protected fun removeItem(item: RecyclerViewItem) {
        removeItem(items.indexOf(item))
    }

    protected fun removeItem(index: Int) {
        if (index < 0) return
        items.removeAt(index)
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.notifyItemRemoved(index)
        }
        if (messageView != null && itemsSize() == 0) {
            messageView!!.visibility = View.VISIBLE
        }
    }

    protected fun clearItems() {
        items.clear()
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.notifyDataSetChanged()
        }
        if (messageView != null) {
            messageView!!.visibility = View.VISIBLE
        }
    }

    fun showProgress() {
        if (progressView != null) {
            synchronized(this) {
                progressCount++
                recyclerView!!.visibility = View.INVISIBLE
                if (messageView != null) {
                    messageView!!.visibility = View.INVISIBLE
                }
                progressView!!.visibility = View.VISIBLE
                if (titleContent != null) {
                    titleContent!!.visibility = View.INVISIBLE
                }
            }
        }
    }

    fun dismissProgress() {
        if (progressView != null) {
            synchronized(this) {
                progressCount--
                if (progressCount <= 0) {
                    recyclerView!!.visibility = View.VISIBLE
                    if (messageView != null) {
                        messageView!!.visibility = if (progressView!!.visibility == View.INVISIBLE && itemsSize() == 0)
                            View.VISIBLE
                        else
                            View.INVISIBLE
                    }
                    progressView!!.visibility = View.INVISIBLE
                    if (titleContent != null) {
                        titleContent!!.visibility = View.VISIBLE
                    }
                    progressCount = 0
                }
            }
        }
    }

    fun itemsSize(): Int {
        return items.size
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("progress_visible", progressView != null && progressView!!.visibility == View.VISIBLE)
    }

    override fun onViewPagerResume() {
        super.onViewPagerResume()

        if (titleFragment != null) {
            titleFragment!!.onViewPagerResume()
        }
    }

    override fun onViewPagerPause() {
        super.onViewPagerPause()

        onScrollListener.animateBottomNavigation(true)
        if (titleFragment != null) {
            titleFragment!!.onViewPagerPause()
        }
    }

    override fun onBackPressed(): Boolean {
        return titleFragment != null && titleFragment!!.onBackPressed()
    }
}
