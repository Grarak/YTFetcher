package com.grarak.ytfetcher.views.musicplayer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.MusicManager;
import com.grarak.ytfetcher.utils.Utils;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MusicPlayerView extends LinearLayout {

    private ViewPager viewPager;
    private TextView titleView;
    private TextView positionTextView;
    private AppCompatSeekBar seekBar;
    private View previousView;
    private FloatingActionButton playPauseView;
    private View nextView;
    private View controls;
    private View progressView;

    private MusicManager musicManager;

    private Drawable playDrawable;
    private Drawable pauseDrawable;

    private Adapter adapter;
    private final List<YoutubeSearchResult> tracks = new ArrayList<>();
    private long duration;
    private AtomicBoolean playing = new AtomicBoolean();

    public MusicPlayerView(Context context) {
        this(context, null);
    }

    public MusicPlayerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.view_music_player, this);

        viewPager = findViewById(R.id.viewpager);
        titleView = findViewById(R.id.title);
        positionTextView = findViewById(R.id.position_text);
        seekBar = findViewById(R.id.seekbar);
        controls = findViewById(R.id.controls_view);
        previousView = findViewById(R.id.previous_btn);
        playPauseView = findViewById(R.id.play_pause_btn);
        nextView = findViewById(R.id.next_btn);
        progressView = findViewById(R.id.progress);

        playDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play);
        pauseDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause);

        playPauseView.setOnClickListener(v -> {
            if (playing.get()) {
                musicManager.pause();
            } else {
                musicManager.resume();
            }
        });

        previousView.setOnClickListener(v ->
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true));
        nextView.setOnClickListener(v ->
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true));

        seekBar.setClickable(true);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                positionTextView.setText(
                        String.format(
                                "%s/%s",
                                Utils.formatSeconds(progress),
                                Utils.formatSeconds(duration)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopCounter();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicManager.seekTo(seekBar.getProgress() * 1000);
                if (playing.get()) {
                    startCounter();
                }
            }
        });

        viewPager.setAdapter(adapter = new Adapter());
        viewPager.setPageMargin(getResources()
                .getDimensionPixelOffset(R.dimen.viewpager_page_margin));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (playing.get()) {
            startCounter();
        }
    }

    private void startCounter() {
        if (getHandler() != null) {
            getHandler().postDelayed(counter, 500);
        }
    }

    private void stopCounter() {
        if (getHandler() != null) {
            getHandler().removeCallbacks(counter);
        }
    }

    private Runnable counter = new Runnable() {
        @Override
        public void run() {
            duration = musicManager.getDuration() / 1000;
            if (duration > 0) {
                long position = musicManager.getCurrentPosition() / 1000;
                positionTextView.setText(
                        String.format(
                                "%s/%s",
                                Utils.formatSeconds(position),
                                Utils.formatSeconds(duration)));
                seekBar.setMax((int) duration);
                seekBar.setProgress((int) position);
            }
            if (playing.get()) {
                startCounter();
            }
        }
    };

    private class Adapter extends PagerAdapter {
        @Override
        public int getCount() {
            return tracks.size();
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            synchronized (tracks) {
                AppCompatImageView imageView = new AppCompatImageView(getContext());
                Glide.with(imageView).load(tracks.get(position).thumbnail).into(imageView);
                container.addView(imageView, 0);
                return imageView;
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    void setMusicManager(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    private ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            musicManager.play(tracks, position);
        }
    };

    private void setViewPager(List<YoutubeSearchResult> results, int position) {
        synchronized (tracks) {
            tracks.clear();
            tracks.addAll(results);
            viewPager.setAdapter(adapter);
            viewPager.removeOnPageChangeListener(onPageChangeListener);
            viewPager.setCurrentItem(position);
            titleView.setText(results.get(position).title);
            viewPager.addOnPageChangeListener(onPageChangeListener);
        }
    }

    void onFetch(List<YoutubeSearchResult> results, int position) {
        stopCounter();
        setViewPager(results, position);
        controls.setVisibility(INVISIBLE);
        seekBar.setVisibility(INVISIBLE);
        progressView.setVisibility(VISIBLE);
        positionTextView.setText("");
        seekBar.setProgress(0);
        playing.set(false);
    }

    void onFailure(List<YoutubeSearchResult> results, int position) {
        stopCounter();
        playing.set(false);
    }

    void onPlay(List<YoutubeSearchResult> results, int position) {
        setViewPager(results, position);
        controls.setVisibility(VISIBLE);
        seekBar.setVisibility(VISIBLE);
        progressView.setVisibility(INVISIBLE);
        playPauseView.setImageDrawable(pauseDrawable);
        playing.set(true);
        counter.run();
    }

    void onPause(List<YoutubeSearchResult> results, int position) {
        stopCounter();
        setViewPager(results, position);
        controls.setVisibility(VISIBLE);
        seekBar.setVisibility(VISIBLE);
        progressView.setVisibility(INVISIBLE);
        playPauseView.setImageDrawable(playDrawable);
        playing.set(false);
        counter.run();
    }

    void onNoMusic() {
        stopCounter();
        playing.set(false);
    }
}
