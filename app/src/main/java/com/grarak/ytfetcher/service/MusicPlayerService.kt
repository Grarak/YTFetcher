package com.grarak.ytfetcher.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.audio.AudioAttributes
import com.grarak.ytfetcher.utils.EqualizerManager
import com.grarak.ytfetcher.utils.ExoPlayerWrapper
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Status
import com.grarak.ytfetcher.utils.server.history.History
import com.grarak.ytfetcher.utils.server.history.HistoryServer
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import java.util.*

class MusicPlayerService : Service(), AudioManager.OnAudioFocusChangeListener, ExoPlayerWrapper.OnPlayerListener {
    companion object {
        private val NAME = MusicPlayerService::class.java.name
        val ACTION_MUSIC_PLAYER_STOP = "$NAME.ACTION.MUSIC_PLAYER_STOP"
        val ACTION_MUSIC_PLAY_PAUSE = "$NAME.ACTION.MUSIC_PLAY_PAUSE"
        val ACTION_MUSIC_PREVIOUS = "$NAME.ACTION.MUSIC_PREVIOUS"
        val ACTION_MUSIC_NEXT = "$NAME.ACTION.MUSIC_NEXT"
    }

    private val binder = MusicPlayerBinder()

    private var youtubeServer: YoutubeServer? = null
    private var historyServer: HistoryServer? = null
    private var exoPlayer: ExoPlayerWrapper? = null
    var equalizerManager: EqualizerManager? = null
        private set
    private var notification: MusicPlayerNotification? = null
    var listener: MusicPlayerListener? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    private val focusLock = Any()
    private var playbackDelayed: Boolean = false
    private var resumeOnFocusGain: Boolean = false

    private val trackLock = Any()
    private val tracks = ArrayList<YoutubeSearchResult>()
    private var user: User? = null
    private var preparing: Boolean = false
    var trackPosition = -1
        private set
    private var lastMusicPosition: Long = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_MUSIC_PLAYER_STOP) {
                stopForeground(true)
                if (listener != null) {
                    listener!!.onDisconnect()
                } else {
                    stopSelf()
                }
            } else if (intent.action == ACTION_MUSIC_PLAY_PAUSE) {
                if (isPlaying) {
                    pauseMusic()
                } else {
                    requestAudioFocus()
                }
            } else if (intent.action == ACTION_MUSIC_PREVIOUS) {
                synchronized(trackLock) {
                    if (trackPosition - 1 >= 0 && trackPosition - 1 < tracks.size) {
                        playMusic(user, tracks, trackPosition - 1)
                    }
                }
            } else if (intent.action == ACTION_MUSIC_NEXT) {
                synchronized(trackLock) {
                    if (trackPosition != -1 && trackPosition + 1 < tracks.size) {
                        playMusic(user, tracks, trackPosition + 1)
                    }
                }
            } else if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseMusic()
            }
        }
    }

    val isPlaying: Boolean
        get() = synchronized(trackLock) {
            return exoPlayer!!.isPlaying && trackPosition >= 0
        }

    val currentPosition: Long
        get() = exoPlayer!!.currentPosition

    val duration: Long
        get() = exoPlayer!!.duration

    val isPreparing: Boolean
        get() = synchronized(trackLock) {
            return preparing && trackPosition >= 0
        }

    val audioSessionId: Int
        get() = exoPlayer!!.audioSessionId

    inner class MusicPlayerBinder : Binder() {
        val service: MusicPlayerService
            get() = this@MusicPlayerService
    }

    @Synchronized
    fun playMusic(user: User?, results: List<YoutubeSearchResult>, position: Int) {
        pauseMusic()
        youtubeServer!!.close()

        synchronized(trackLock) {
            this.user = user
            preparing = true
            trackPosition = position
            lastMusicPosition = 0
            if (tracks !== results) {
                tracks.clear()
                tracks.addAll(results)
            }
        }

        if (listener != null) {
            listener!!.onPreparing(tracks, position)
        }

        val result = tracks[position]
        notification!!.showProgress(result)

        val file = result.getDownloadPath(this)
        if (file.exists()) {
            val history = History()
            history.apikey = user!!.apikey
            history.id = result.id
            historyServer!!.add(history, object : GenericCallback {
                override fun onSuccess() {}

                override fun onFailure(code: Int) {}
            })

            exoPlayer!!.setFile(file)
            return
        }

        val youtube = Youtube()
        youtube.apikey = user!!.apikey
        youtube.id = result.id
        youtube.addhistory = true
        youtubeServer!!.fetchSong(youtube, object : YoutubeServer.YoutubeSongIdCallback {
            override fun onSuccess(url: String) {
                exoPlayer!!.setUrl(url)
            }

            override fun onFailure(code: Int) {
                if (listener != null) {
                    listener!!.onFailure(code, results, position)
                }
                synchronized(trackLock) {
                    if (moveOn()) {
                        playMusic(user, tracks, trackPosition + 1)
                    } else {
                        trackPosition = -1
                    }
                }
                notification!!.showFailure(result)
            }
        })
    }

    fun resumeMusic() {
        requestAudioFocus()
    }

    private fun playMusic() {
        synchronized(trackLock) {
            if (trackPosition < 0) {
                return
            }
            seekTo(lastMusicPosition)
            exoPlayer!!.play()
            notification!!.showPlay(tracks[trackPosition])
            if (listener != null) {
                listener!!.onPlay(tracks, trackPosition)
            }
        }
    }

    fun pauseMusic() {
        synchronized(trackLock) {
            lastMusicPosition = currentPosition
            exoPlayer!!.pause()
            notification!!.showPause()
            synchronized(focusLock) {
                resumeOnFocusGain = false
            }
            if (listener != null && trackPosition >= 0) {
                listener!!.onPause(tracks, trackPosition)
            }
        }
    }

    fun seekTo(position: Long) {
        lastMusicPosition = position
        exoPlayer!!.seekTo(position)
    }

    fun getTracks(): List<YoutubeSearchResult> {
        synchronized(trackLock) {
            return Collections.unmodifiableList(tracks)
        }
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val ret: Int
        ret = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        synchronized(focusLock) {
            when (ret) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> playbackDelayed = false
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    playbackDelayed = false
                    playMusic()
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> playbackDelayed = true
            }
        }
    }

    override fun onCompletion(exoPlayer: ExoPlayerWrapper) {
        pauseMusic()
        synchronized(trackLock) {
            if (moveOn()) {
                playMusic(user, tracks, trackPosition + 1)
            } else {
                lastMusicPosition = 0
            }
        }
    }

    private fun moveOn(): Boolean {
        return trackPosition >= 0 && trackPosition + 1 < tracks.size && user != null
    }

    override fun onError(exoPlayer: ExoPlayerWrapper, error: ExoPlaybackException) {
        synchronized(trackLock) {
            if (trackPosition >= 0) {
                if (listener != null) {
                    listener!!.onFailure(Status.ServerOffline, tracks, trackPosition)
                }
                notification!!.showFailure(tracks[trackPosition])
                if (moveOn()) {
                    playMusic(user, tracks, trackPosition + 1)
                } else {
                    trackPosition = -1
                }
            }
        }
    }

    override fun onPrepared(exoPlayer: ExoPlayerWrapper) {
        synchronized(trackLock) {
            preparing = false
            requestAudioFocus()
        }
    }

    override fun onAudioSessionIdChanged(exoPlayer: ExoPlayerWrapper, id: Int) {
        equalizerManager!!.setAudioSessionId(id)
        if (listener != null) {
            listener!!.onAudioSessionIdChanged(id)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        try {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> if (playbackDelayed || resumeOnFocusGain) {
                    synchronized(focusLock) {
                        playbackDelayed = false
                        resumeOnFocusGain = false
                    }
                    exoPlayer!!.setVolume(1.0f)
                    playMusic()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    synchronized(focusLock) {
                        resumeOnFocusGain = false
                        playbackDelayed = false
                    }
                    pauseMusic()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    synchronized(focusLock) {
                        resumeOnFocusGain = isPlaying
                        playbackDelayed = false
                    }
                    pauseMusic()
                }
            }
        } catch (ignored: IllegalStateException) {
        }

    }

    override fun onCreate() {
        super.onCreate()

        youtubeServer = YoutubeServer(this)
        historyServer = HistoryServer(this)

        exoPlayer = ExoPlayerWrapper(this)
        exoPlayer!!.onPlayerListener = this
        val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA).build()
        exoPlayer!!.setAudioAttributes(audioAttributes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()
        }

        equalizerManager = EqualizerManager(this)

        notification = MusicPlayerNotification(this)

        val filter = IntentFilter()
        filter.addAction(ACTION_MUSIC_PLAYER_STOP)
        filter.addAction(ACTION_MUSIC_PLAY_PAUSE)
        filter.addAction(ACTION_MUSIC_PREVIOUS)
        filter.addAction(ACTION_MUSIC_NEXT)
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()

        notification!!.stop()

        equalizerManager!!.release()
        exoPlayer!!.release()
        youtubeServer!!.close()
        historyServer!!.close()
        unregisterReceiver(receiver)

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.abandonAudioFocus(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}
