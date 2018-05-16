package com.grarak.ytfetcher.utils;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.grarak.ytfetcher.R;

import java.io.File;

public class ExoPlayerWrapper implements Player.EventListener {

    private enum State {
        PREPARING,
        PLAYING,
        PAUSED,
        IDLE
    }

    public interface OnPlayerListener {
        void onPrepared(ExoPlayerWrapper exoPlayer);

        void onAudioSessionIdChanged(ExoPlayerWrapper exoPlayer, int id);

        void onCompletion(ExoPlayerWrapper exoPlayer);

        void onError(ExoPlayerWrapper exoPlayer, ExoPlaybackException error);
    }

    private SimpleExoPlayer exoPlayer;
    private DataSource.Factory dataSourceFactory;
    private PowerManager.WakeLock wakeLock;

    private final Object stateLock = new Object();
    private State state = State.IDLE;

    private OnPlayerListener onPlayerListener;
    private int sessionId;

    public ExoPlayerWrapper(Context context) {
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context,
                null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, new DefaultTrackSelector());
        exoPlayer.addListener(this);
        exoPlayer.addAudioDebugListener(new AudioRendererEventListener() {
            @Override
            public void onAudioEnabled(DecoderCounters counters) {
            }

            @Override
            public void onAudioSessionId(int audioSessionId) {
                if (sessionId != audioSessionId) {
                    sessionId = audioSessionId;
                    if (onPlayerListener != null) {
                        onPlayerListener.onAudioSessionIdChanged(
                                ExoPlayerWrapper.this, audioSessionId);
                    }
                }
            }

            @Override
            public void onAudioDecoderInitialized(String decoderName,
                                                  long initializedTimestampMs,
                                                  long initializationDurationMs) {
            }

            @Override
            public void onAudioInputFormatChanged(Format format) {
            }

            @Override
            public void onAudioSinkUnderrun(int bufferSize,
                                            long bufferSizeMs,
                                            long elapsedSinceLastFeedMs) {
            }

            @Override
            public void onAudioDisabled(DecoderCounters counters) {
            }
        });
        dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getString(R.string.app_name)));

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                ExoPlayerWrapper.class.getSimpleName());
    }

    public void setUrl(String url) {
        setState(State.PREPARING);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(url));
        exoPlayer.prepare(mediaSource, true, true);
    }

    public void setFile(File file) {
        setState(State.PREPARING);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.fromFile(file));
        exoPlayer.prepare(mediaSource, true, true);
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    public void seekTo(long position) {
        exoPlayer.seekTo(position);
    }

    public void setAudioAttributes(AudioAttributes audioAttributes) {
        exoPlayer.setAudioAttributes(audioAttributes);
    }

    public void setVolume(float volume) {
        exoPlayer.setVolume(volume);
    }

    public void play() {
        setState(State.PLAYING);
        exoPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        setState(State.PAUSED);
        exoPlayer.setPlayWhenReady(false);
    }

    public boolean isPlaying() {
        return getState() == State.PLAYING;
    }

    public int getAudioSessionId() {
        return exoPlayer.getAudioSessionId();
    }

    public void setOnPlayerListener(OnPlayerListener onPlayerListener) {
        this.onPlayerListener = onPlayerListener;
    }

    public void release() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        exoPlayer.release();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                if (getState() == State.PREPARING) {
                    setState(State.IDLE);
                    if (onPlayerListener != null) {
                        onPlayerListener.onPrepared(this);
                    }
                }
                break;
            case Player.STATE_ENDED:
                if (getDuration() == 0 || getCurrentPosition() != 0) {
                    setState(State.IDLE);
                    if (onPlayerListener != null) {
                        onPlayerListener.onCompletion(this);
                    }
                }
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (onPlayerListener != null) {
            onPlayerListener.onError(this, error);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    }

    @Override
    public void onSeekProcessed() {
    }

    private void setState(State state) {
        synchronized (stateLock) {
            this.state = state;
            if (state == State.PLAYING) {
                wakeLock.acquire();
            } else if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private State getState() {
        synchronized (stateLock) {
            return state;
        }
    }
}
