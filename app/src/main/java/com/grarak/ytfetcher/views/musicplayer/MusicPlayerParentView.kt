package com.grarak.ytfetcher.views.musicplayer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.MusicManager
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.sothree.slidinguppanel.SlidingUpPanelLayout

class MusicPlayerParentView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private val headerView: MusicPlayerHeaderView
    private val playerView: MusicPlayerView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_music_player_parent, this)

        val a = context.obtainStyledAttributes(attrs,
                R.styleable.MusicPlayerParentView, defStyleAttr, 0)
        val collapsedHeight = a.getDimensionPixelSize(R.styleable.MusicPlayerParentView_collapsedHeight,
                resources.getDimensionPixelSize(R.dimen.musicview_height))

        a.recycle()

        playerView = findViewById(R.id.musicplayer_view)
        headerView = findViewById(R.id.musicplayerheader_view)
        headerView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, collapsedHeight)

        headerView.setOnClickListener { v ->
            if (parent is SlidingUpPanelLayout) {
                val slidingUpPanelLayout = parent as SlidingUpPanelLayout
                if (slidingUpPanelLayout.isTouchEnabled) {
                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
                }
            }
        }
    }

    fun setMusicManager(musicManager: MusicManager) {
        headerView.setMusicManager(musicManager)
        playerView.setMusicManager(musicManager)
    }

    fun setCollapsed(collapsed: Boolean) {
        headerView.visibility = if (collapsed) View.VISIBLE else View.INVISIBLE
    }

    fun onFetch(results: List<YoutubeSearchResult>, position: Int) {
        headerView.onFetch(results, position)
        playerView.onFetch(results, position)
    }

    fun onFailure(code: Int, results: List<YoutubeSearchResult>, position: Int) {
        headerView.onFailure(results, position)
        playerView.onFailure(results, position)
    }

    fun onPlay(results: List<YoutubeSearchResult>, position: Int) {
        headerView.onPlay(results, position)
        playerView.onPlay(results, position)
    }

    fun onPause(results: List<YoutubeSearchResult>, position: Int) {
        headerView.onPause(results, position)
        playerView.onPause(results, position)
    }

    fun onAudioSessionIdChanged(id: Int) {
        playerView.onAudioSessionIdChanged(id)
    }

    fun onNoMusic() {
        headerView.onNoMusic()
        playerView.onNoMusic()
    }
}
