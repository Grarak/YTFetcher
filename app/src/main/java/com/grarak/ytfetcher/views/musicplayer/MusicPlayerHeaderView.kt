package com.grarak.ytfetcher.views.musicplayer

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import com.bumptech.glide.Glide
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.MusicManager
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

class MusicPlayerHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private var musicManager: MusicManager? = null

    private val titleView: TextView
    private val playPauseView: AppCompatImageView
    private val progressView: View
    private val thumbnailView: AppCompatImageView

    private val playDrawable: Drawable?
    private val pauseDrawable: Drawable?

    private var playing: Boolean = false

    init {
        orientation = LinearLayout.HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_music_player_header, this)

        titleView = findViewById(R.id.title)
        playPauseView = findViewById(R.id.play_pause_btn)
        progressView = findViewById(R.id.progress)
        thumbnailView = findViewById(R.id.thumbnail)

        playDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play)
        pauseDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause)

        playPauseView.setOnClickListener {
            if (playing) {
                musicManager!!.pause()
            } else {
                musicManager!!.resume()
            }
        }
    }

    internal fun setMusicManager(musicManager: MusicManager) {
        this.musicManager = musicManager
    }

    internal fun onFetch(results: List<YoutubeSearchResult>, position: Int) {
        onPlay(results, position)
        playPauseView.visibility = View.INVISIBLE
        progressView.visibility = View.VISIBLE
    }

    internal fun onFailure(results: List<YoutubeSearchResult>, position: Int) {
        onNoMusic()
    }

    internal fun onPlay(results: List<YoutubeSearchResult>, position: Int) {
        playing = true
        playPauseView.setImageDrawable(pauseDrawable)
        playPauseView.visibility = View.VISIBLE
        progressView.visibility = View.INVISIBLE
        titleView.text = results[position].title
        thumbnailView.visibility = View.VISIBLE
        Glide.with(this).load(results[position].thumbnail).into(thumbnailView)
    }

    internal fun onPause(results: List<YoutubeSearchResult>, position: Int) {
        onPlay(results, position)
        playing = false
        playPauseView.setImageDrawable(playDrawable)
    }

    internal fun onNoMusic() {
        titleView.text = resources.getString(R.string.no_music)
        thumbnailView.visibility = View.INVISIBLE
        playPauseView.visibility = View.INVISIBLE
        progressView.visibility = View.INVISIBLE
    }
}
