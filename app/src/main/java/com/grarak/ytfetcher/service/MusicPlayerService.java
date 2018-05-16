package com.grarak.ytfetcher.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.grarak.ytfetcher.utils.ExoPlayerWrapper;
import com.grarak.ytfetcher.utils.server.GenericCallback;
import com.grarak.ytfetcher.utils.server.Status;
import com.grarak.ytfetcher.utils.server.history.History;
import com.grarak.ytfetcher.utils.server.history.HistoryServer;
import com.grarak.ytfetcher.utils.server.user.User;
import com.grarak.ytfetcher.utils.server.youtube.Youtube;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicPlayerService extends Service
        implements AudioManager.OnAudioFocusChangeListener,
        ExoPlayerWrapper.OnPlayerListener {

    private static final String NAME = MusicPlayerService.class.getName();
    public static final String ACTION_MUSIC_PLAYER_STOP = NAME + ".ACTION.MUSIC_PLAYER_STOP";
    public static final String ACTION_MUSIC_PLAY_PAUSE = NAME + ".ACTION.MUSIC_PLAY_PAUSE";
    public static final String ACTION_MUSIC_PREVIOUS = NAME + ".ACTION.MUSIC_PREVIOUS";
    public static final String ACTION_MUSIC_NEXT = NAME + ".ACTION.MUSIC_NEXT";

    private MusicPlayerBinder binder = new MusicPlayerBinder();

    private YoutubeServer youtubeServer;
    private HistoryServer historyServer;
    private ExoPlayerWrapper exoPlayer;
    private MusicPlayerNotification notification;
    private MusicPlayerListener listener;

    private AudioFocusRequest audioFocusRequest;

    private final Object focusLock = new Object();
    private boolean playbackDelayed;
    private boolean resumeOnFocusGain;

    private final Object trackLock = new Object();
    private List<YoutubeSearchResult> tracks = new ArrayList<>();
    private User user;
    private boolean preparing;
    private int trackPosition = -1;
    private long lastMusicPosition;

    public class MusicPlayerBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_MUSIC_PLAYER_STOP)) {
                stopForeground(true);
                if (listener != null) {
                    listener.onDisconnect();
                } else {
                    stopSelf();
                }
            } else if (intent.getAction().equals(ACTION_MUSIC_PLAY_PAUSE)) {
                if (isPlaying()) {
                    pauseMusic();
                } else {
                    requestAudioFocus();
                }
            } else if (intent.getAction().equals(ACTION_MUSIC_PREVIOUS)) {
                synchronized (trackLock) {
                    if (trackPosition - 1 >= 0 && trackPosition - 1 < tracks.size()) {
                        playMusic(user, tracks, trackPosition - 1);
                    }
                }
            } else if (intent.getAction().equals(ACTION_MUSIC_NEXT)) {
                synchronized (trackLock) {
                    if (trackPosition != -1 && trackPosition + 1 < tracks.size()) {
                        playMusic(user, tracks, trackPosition + 1);
                    }
                }
            } else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                pauseMusic();
            }
        }
    };

    public synchronized void playMusic(User user, List<YoutubeSearchResult> results, int position) {
        pauseMusic();
        youtubeServer.close();

        synchronized (trackLock) {
            this.user = user;
            preparing = true;
            trackPosition = position;
            lastMusicPosition = 0;
            if (tracks != results) {
                tracks.clear();
                tracks.addAll(results);
            }
        }

        if (listener != null) {
            listener.onPreparing(tracks, position);
        }

        YoutubeSearchResult result = tracks.get(position);
        notification.showProgress(result);

        File file = result.getDownloadPath(this);
        if (file.exists()) {
            History history = new History();
            history.apikey = user.apikey;
            history.id = result.id;
            historyServer.add(history, new GenericCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int code) {
                }
            });

            exoPlayer.setFile(file);
            return;
        }

        Youtube youtube = new Youtube();
        youtube.apikey = user.apikey;
        youtube.id = result.id;
        youtube.addhistory = true;
        youtubeServer.fetchSong(youtube, new YoutubeServer.YoutubeSongIdCallback() {
            @Override
            public void onSuccess(String url) {
                exoPlayer.setUrl(url);
            }

            @Override
            public void onFailure(int code) {
                if (listener != null) {
                    listener.onFailure(code, results, position);
                }
                synchronized (trackLock) {
                    if (moveOn()) {
                        playMusic(user, tracks, trackPosition + 1);
                    } else {
                        trackPosition = -1;
                    }
                }
                notification.showFailure(result);
            }
        });
    }

    public void resumeMusic() {
        requestAudioFocus();
    }

    private void playMusic() {
        synchronized (trackLock) {
            if (trackPosition < 0) {
                return;
            }
            seekTo(lastMusicPosition);
            exoPlayer.play();
            notification.showPlay(tracks.get(trackPosition));
            if (listener != null) {
                listener.onPlay(tracks, trackPosition);
            }
        }
    }

    public void pauseMusic() {
        synchronized (trackLock) {
            lastMusicPosition = getCurrentPosition();
            exoPlayer.pause();
            notification.showPause();
            synchronized (focusLock) {
                resumeOnFocusGain = false;
            }
            if (listener != null && trackPosition >= 0) {
                listener.onPause(tracks, trackPosition);
            }
        }
    }

    public void seekTo(long position) {
        lastMusicPosition = position;
        exoPlayer.seekTo(position);
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public int getTrackPosition() {
        return trackPosition;
    }

    public List<YoutubeSearchResult> getTracks() {
        synchronized (trackLock) {
            return Collections.unmodifiableList(tracks);
        }
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    public boolean isPreparing() {
        synchronized (trackLock) {
            return preparing;
        }
    }

    public int getAudioSessionId() {
        return exoPlayer.getAudioSessionId();
    }

    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int ret;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ret = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            ret = audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        synchronized (focusLock) {
            if (ret == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                playbackDelayed = false;
            } else if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                playbackDelayed = false;
                playMusic();
            } else if (ret == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                playbackDelayed = true;
            }
        }
    }

    @Override
    public void onCompletion(ExoPlayerWrapper exoPlayer) {
        pauseMusic();
        synchronized (trackLock) {
            if (moveOn()) {
                playMusic(user, tracks, trackPosition + 1);
            } else {
                lastMusicPosition = 0;
            }
        }
    }

    private boolean moveOn() {
        return trackPosition >= 0 && trackPosition + 1 < tracks.size() && user != null;
    }

    @Override
    public void onError(ExoPlayerWrapper exoPlayer, ExoPlaybackException error) {
        synchronized (trackLock) {
            if (trackPosition >= 0) {
                if (listener != null) {
                    listener.onFailure(Status.ServerOffline, tracks, trackPosition);
                }
                notification.showFailure(tracks.get(trackPosition));
                if (moveOn()) {
                    playMusic(user, tracks, trackPosition + 1);
                } else {
                    trackPosition = -1;
                }
            }
        }
    }

    @Override
    public void onPrepared(ExoPlayerWrapper exoPlayer) {
        synchronized (trackLock) {
            preparing = false;
            requestAudioFocus();
        }
    }

    @Override
    public void onAudioSessionIdChanged(ExoPlayerWrapper exoPlayer, int id) {
        if (listener != null) {
            listener.onAudioSessionIdChanged(id);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        try {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (playbackDelayed || resumeOnFocusGain) {
                        synchronized (focusLock) {
                            playbackDelayed = false;
                            resumeOnFocusGain = false;
                        }
                        exoPlayer.setVolume(1.0f);
                        playMusic();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    synchronized (focusLock) {
                        resumeOnFocusGain = false;
                        playbackDelayed = false;
                    }
                    pauseMusic();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    synchronized (focusLock) {
                        resumeOnFocusGain = isPlaying();
                        playbackDelayed = false;
                    }
                    pauseMusic();
                    break;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        youtubeServer = new YoutubeServer(this);
        historyServer = new HistoryServer(this);

        exoPlayer = new ExoPlayerWrapper(this);
        exoPlayer.setOnPlayerListener(this);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA).build();
        exoPlayer.setAudioAttributes(audioAttributes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }

        notification = new MusicPlayerNotification(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MUSIC_PLAYER_STOP);
        filter.addAction(ACTION_MUSIC_PLAY_PAUSE);
        filter.addAction(ACTION_MUSIC_PREVIOUS);
        filter.addAction(ACTION_MUSIC_NEXT);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        notification.stop();

        exoPlayer.release();
        youtubeServer.close();
        historyServer.close();
        unregisterReceiver(receiver);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(this);
    }

    public void setListener(MusicPlayerListener listener) {
        this.listener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
