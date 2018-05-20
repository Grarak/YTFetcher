package com.grarak.ytfetcher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.grarak.ytfetcher.service.MusicPlayerListener
import com.grarak.ytfetcher.service.MusicPlayerService
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import java.util.*

class MusicManager(private val context: Context, private val user: User, private val listener: MusicPlayerListener) {
    private var service: MusicPlayerService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            synchronized(this@MusicManager) {
                val binder = service as MusicPlayerService.MusicPlayerBinder
                this@MusicManager.service = binder.service
                this@MusicManager.service!!.listener = listener
                listener.onConnect()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            synchronized(this@MusicManager) {
                this@MusicManager.service = null
            }
        }
    }

    val isPlaying: Boolean
        get() {
            synchronized(this) {
                service?.run {
                    return isPlaying
                }
            }
            return false
        }

    val trackPosition: Int
        get() {
            synchronized(this) {
                service?.run {
                    return trackPosition
                }
            }
            return -1
        }

    val tracks: List<YoutubeSearchResult>
        get() {
            synchronized(this) {
                service?.run {
                    return getTracks()
                }
            }
            return ArrayList()
        }

    val currentPosition: Long
        get() {
            synchronized(this) {
                service?.run {
                    return currentPosition
                }
            }
            return 0
        }

    val duration: Long
        get() {
            synchronized(this) {
                service?.run {
                    return duration
                }
            }
            return 0
        }

    val isPreparing: Boolean
        get() {
            synchronized(this) {
                service?.run {
                    return isPreparing
                }
            }
            return false
        }

    val audioSessionId: Int
        get() {
            synchronized(this) {
                service?.run {
                    return audioSessionId
                }
            }
            return 0
        }

    val equalizerManager: EqualizerManager?
        get() {
            synchronized(this) {
                service?.run {
                    return equalizerManager
                }
            }
            return null
        }

    fun onResume() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun onPause() {
        synchronized(this) {
            service?.listener = null
            try {
                context.unbindService(serviceConnection)
            } catch (ignored: IllegalArgumentException) {
            }

        }
    }

    fun destroy() {
        onPause()
        context.stopService(Intent(context, MusicPlayerService::class.java))
    }

    fun restart() {
        destroy()
        onResume()
    }

    fun play(result: YoutubeSearchResult) {
        play(listOf(result), 0)
    }

    fun play(results: List<YoutubeSearchResult>, position: Int) {
        synchronized(this) {
            service?.playMusic(user, results, position)
        }
    }

    fun resume() {
        synchronized(this) {
            service?.resumeMusic()
        }
    }

    fun pause() {
        synchronized(this) {
            service?.pauseMusic()
        }
    }

    fun seekTo(position: Int) {
        synchronized(this) {
            service?.seekTo(position.toLong())
        }
    }
}
