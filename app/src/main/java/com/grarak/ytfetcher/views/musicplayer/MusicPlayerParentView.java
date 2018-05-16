package com.grarak.ytfetcher.views.musicplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.MusicManager;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.List;

public class MusicPlayerParentView extends FrameLayout {

    private MusicPlayerHeaderView headerView;
    private MusicPlayerView playerView;

    public MusicPlayerParentView(Context context) {
        this(context, null);
    }

    public MusicPlayerParentView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicPlayerParentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.view_music_player_parent, this);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MusicPlayerParentView, defStyleAttr, 0);
        int collapsedHeight = a.getDimensionPixelSize(R.styleable.MusicPlayerParentView_collapsedHeight,
                getResources().getDimensionPixelSize(R.dimen.musicview_height));

        a.recycle();

        playerView = findViewById(R.id.musicplayer_view);
        headerView = findViewById(R.id.musicplayerheader_view);
        headerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, collapsedHeight));

        headerView.setOnClickListener(v -> {
            if (getParent() instanceof SlidingUpPanelLayout) {
                SlidingUpPanelLayout slidingUpPanelLayout = (SlidingUpPanelLayout) getParent();
                if (slidingUpPanelLayout.isTouchEnabled()) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
            }
        });
    }

    public void setMusicManager(MusicManager musicManager) {
        headerView.setMusicManager(musicManager);
        playerView.setMusicManager(musicManager);
    }

    public void setCollapsed(boolean collapsed) {
        headerView.setVisibility(collapsed ? VISIBLE : INVISIBLE);
    }

    public void onFetch(List<YoutubeSearchResult> results, int position) {
        headerView.onFetch(results, position);
        playerView.onFetch(results, position);
    }

    public void onFailure(int code, List<YoutubeSearchResult> results, int position) {
        headerView.onFailure(results, position);
        playerView.onFailure(results, position);
    }

    public void onPlay(List<YoutubeSearchResult> results, int position) {
        headerView.onPlay(results, position);
        playerView.onPlay(results, position);
    }

    public void onPause(List<YoutubeSearchResult> results, int position) {
        headerView.onPause(results, position);
        playerView.onPause(results, position);
    }

    public void onAudioSessionIdChanged(int id) {
        playerView.onAudioSessionIdChanged(id);
    }

    public void onNoMusic() {
        headerView.onNoMusic();
        playerView.onNoMusic();
    }
}
