package com.grarak.ytfetcher.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.bumptech.glide.Glide
import com.grarak.ytfetcher.LoginActivity
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import java.util.concurrent.atomic.AtomicBoolean

class MusicPlayerNotification internal constructor(private val service: MusicPlayerService) {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL = "music_channel"
    }

    private val manager: NotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val fetching = AtomicBoolean()
    private val playing = AtomicBoolean()
    private var result: YoutubeSearchResult? = null
    private var playingBitmap: Bitmap? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    private fun getBitmap(url: String): Bitmap {
        return try {
            Glide.with(service).asBitmap().load(url).submit().get()
        } catch (ignored: Exception) {
            Utils.drawableToBitmap(ContextCompat.getDrawable(service, R.mipmap.ic_launcher)!!)
        }
    }

    private fun getBroadcast(action: String): PendingIntent {
        return PendingIntent.getBroadcast(service, 0, Intent(action), 0)
    }

    internal fun showProgress(result: YoutubeSearchResult) {
        fetching.set(true)
        playing.set(false)
        this.result = result

        val builder = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                .setContentTitle(service.getString(R.string.loading))
                .setContentText(result.title)
                .setSmallIcon(R.drawable.ic_music_box)
                .setProgress(0, 0, true)

        service.startForeground(NOTIFICATION_ID, builder.build())
    }

    internal fun showFailure(result: YoutubeSearchResult) {
        fetching.set(false)
        playing.set(false)
        this.result = result

        val intent = Intent(service, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val contentIntent = PendingIntent.getActivity(service, 0, intent, 0)

        val builder = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                .setContentTitle(service.getString(R.string.failed))
                .setContentText(result.title)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_music_box)

        manager.notify(NOTIFICATION_ID, builder.build())
        service.stopForeground(false)
    }

    internal fun showPlay(result: YoutubeSearchResult) {
        fetching.set(false)
        playing.set(true)
        this.result = result
        Thread {
            playingBitmap = getBitmap(result.thumbnail!!)
            val builder = baseBuilder(result, playingBitmap!!, true)

            service.startForeground(NOTIFICATION_ID, builder.build())
        }.start()
    }

    internal fun showPause() {
        fetching.set(false)
        playing.set(false)
        if (result == null) return
        Thread {
            if (playingBitmap == null) {
                playingBitmap = getBitmap(result!!.thumbnail!!)
            }
            val builder = baseBuilder(result!!, playingBitmap!!, false)
                    .setAutoCancel(true)

            service.startForeground(NOTIFICATION_ID, builder.build())
        }.start()
    }

    internal fun stop() {
        manager.cancel(NOTIFICATION_ID)
    }

    private fun baseBuilder(
            result: YoutubeSearchResult, bitmap: Bitmap, play: Boolean): NotificationCompat.Builder {
        val intent = Intent(service, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val contentIntent = PendingIntent.getActivity(service, 0, intent, 0)

        val titleFormatted = Utils.formatResultTitle(result)

        val mediaStyle = android.support.v4.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
        mediaStyle.setShowActionsInCompactView(2)

        return NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                .setContentTitle(titleFormatted[0])
                .setContentText(titleFormatted[1])
                .setSubText(result.duration)
                .setSmallIcon(R.drawable.ic_music_box)
                .setLargeIcon(bitmap)
                .setContentIntent(contentIntent)
                .addAction(NotificationCompat.Action(
                        if (play) R.drawable.ic_pause else R.drawable.ic_play,
                        service.getString(if (play) R.string.pause else R.string.play),
                        getBroadcast(MusicPlayerService.ACTION_MUSIC_PLAY_PAUSE)))
                .addAction(NotificationCompat.Action(
                        R.drawable.ic_skip_next,
                        service.getString(R.string.next),
                        getBroadcast(MusicPlayerService.ACTION_MUSIC_NEXT)))
                .addAction(NotificationCompat.Action(
                        R.drawable.ic_stop,
                        service.getString(R.string.stop),
                        getBroadcast(MusicPlayerService.ACTION_MUSIC_PLAYER_STOP)))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(mediaStyle)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL) != null) {
            return
        }
        val channel = NotificationChannel(
                NOTIFICATION_CHANNEL, service.getString(R.string.music_player),
                NotificationManager.IMPORTANCE_LOW)
        channel.setSound(null, null)

        manager.createNotificationChannel(channel)
    }
}
