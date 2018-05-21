package com.grarak.ytfetcher.views.musicplayer

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.MusicManager
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.views.MusicVisualizerView
import kotlinx.android.parcel.Parcelize
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MusicPlayerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager
    private val titleView: TextView
    private val positionTextView: TextView
    private val seekBar: AppCompatSeekBar
    private val playPauseView: FloatingActionButton
    private val controls: View
    private val progressView: View

    private var musicManager: MusicManager? = null

    private val playDrawable: Drawable?
    private val pauseDrawable: Drawable?

    private val tracks = ArrayList<YoutubeSearchResult>()
    private var duration = 0L
    private val playing = AtomicBoolean()

    private var seassionId = 0
    private var resetSessionId = false
    private val visualizer: MusicVisualizerView

    private var counter: Runnable? = null
        get() {
            if (field == null) {
                field = Runnable {
                    duration = musicManager!!.duration / 1000
                    if (duration > 0) {
                        if (resetSessionId) {
                            onAudioSessionIdChanged(seassionId)
                            resetSessionId = false
                        }
                        val position = musicManager!!.currentPosition / 1000
                        positionTextView.text = String.format(
                                "%s/%s",
                                Utils.formatSeconds(position),
                                Utils.formatSeconds(duration))
                        seekBar.max = duration.toInt()
                        seekBar.progress = position.toInt()
                    }
                    if (playing.get()) {
                        startCounter()
                    }
                }
            }
            return field
        }

    private val onPageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            musicManager!!.play(tracks, position)
        }
    }

    init {

        LayoutInflater.from(context).inflate(R.layout.view_music_player, this)

        viewPager = findViewById(R.id.viewpager)
        titleView = findViewById(R.id.title)
        positionTextView = findViewById(R.id.position_text)
        seekBar = findViewById(R.id.seekbar)
        controls = findViewById(R.id.controls_view)
        val previousView = findViewById<View>(R.id.previous_btn)
        playPauseView = findViewById(R.id.play_pause_btn)
        val nextView = findViewById<View>(R.id.next_btn)
        progressView = findViewById(R.id.progress)
        visualizer = findViewById(R.id.visualizer)

        playDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play)
        pauseDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause)

        playPauseView.setOnClickListener {
            if (playing.get()) {
                musicManager!!.pause()
            } else {
                musicManager!!.resume()
            }
        }

        previousView.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem - 1, true) }
        nextView.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem + 1, true) }

        seekBar.isClickable = true
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                positionTextView.text = String.format(
                        "%s/%s",
                        Utils.formatSeconds(progress.toLong()),
                        Utils.formatSeconds(duration))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopCounter()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                resetSessionId = true
                musicManager!!.seekTo(seekBar.progress * 1000)
                if (playing.get()) {
                    startCounter()
                }
            }
        })

        viewPager.pageMargin = resources
                .getDimensionPixelOffset(R.dimen.viewpager_page_margin)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (playing.get()) {
            startCounter()
        }
    }

    private fun startCounter() {
        visualizer.isEnabled = true
        if (handler != null) {
            handler.postDelayed(counter, 250)
        }
    }

    private fun stopCounter() {
        visualizer.isEnabled = false
        if (handler != null) {
            handler.removeCallbacks(counter)
        }
    }

    inner class Adapter : PagerAdapter() {
        override fun getCount(): Int {
            return tracks.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            synchronized(tracks) {
                val imageView = AppCompatImageView(context)
                Glide.with(imageView).load(tracks[position].thumbnail).into(imageView)
                container.addView(imageView, 0)
                return imageView
            }
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }
    }

    internal fun setMusicManager(musicManager: MusicManager) {
        this.musicManager = musicManager
    }

    private fun setViewPager(results: List<YoutubeSearchResult>, position: Int) {
        synchronized(tracks) {
            tracks.clear()
            tracks.addAll(results)
            viewPager.adapter = Adapter()
            viewPager.removeOnPageChangeListener(onPageChangeListener)
            viewPager.currentItem = position
            val titleFormatted = Utils.formatResultTitle(results[position])
            titleView.text = String.format("%s\n%s", titleFormatted[0], titleFormatted[1])
            viewPager.addOnPageChangeListener(onPageChangeListener)
        }
    }

    internal fun onFetch(results: List<YoutubeSearchResult>, position: Int) {
        stopCounter()
        setViewPager(results, position)
        controls.visibility = View.INVISIBLE
        seekBar.visibility = View.INVISIBLE
        progressView.visibility = View.VISIBLE
        positionTextView.text = ""
        seekBar.progress = 0
        playing.set(false)
    }

    internal fun onFailure(results: List<YoutubeSearchResult>, position: Int) {
        stopCounter()
        playing.set(false)
    }

    internal fun onPlay(results: List<YoutubeSearchResult>, position: Int) {
        setViewPager(results, position)
        controls.visibility = View.VISIBLE
        seekBar.visibility = View.VISIBLE
        progressView.visibility = View.INVISIBLE
        playPauseView.setImageDrawable(pauseDrawable)
        playing.set(true)
        counter!!.run()
    }

    internal fun onPause(results: List<YoutubeSearchResult>, position: Int) {
        stopCounter()
        setViewPager(results, position)
        controls.visibility = View.VISIBLE
        seekBar.visibility = View.VISIBLE
        progressView.visibility = View.INVISIBLE
        playPauseView.setImageDrawable(playDrawable)
        playing.set(false)
        counter!!.run()
        resetSessionId = true
    }

    fun onAudioSessionIdChanged(id: Int) {
        seassionId = id
        visualizer.setAudioSessionId(id)
    }

    internal fun onNoMusic() {
        stopCounter()
        playing.set(false)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        onAudioSessionIdChanged(savedState.sessionId)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val parcelable = super.onSaveInstanceState()
        return SavedState(seassionId, parcelable)
    }

    @Parcelize
    private class SavedState(val sessionId: Int = 0, internal val superState: Parcelable)
        : View.BaseSavedState(superState)
}
