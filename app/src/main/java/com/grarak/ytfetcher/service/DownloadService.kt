package com.grarak.ytfetcher.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.Youtube
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service(), DownloadListener {
    companion object {
        private val ACTION_CANCEL = DownloadService::class.java.name + ".ACTION.CANCEL"
        private val ACTION_CANCEL_ALL = DownloadService::class.java.name + ".ACTION.CANCEL_ALL"
        val ACTION_DOWNLOADED = DownloadService::class.java.name + ".ACTION.DOWNLOADED"
        private val INTENT_USER = DownloadService::class.java.name + ".INTENT.USER"
        val INTENT_DOWNLOAD = DownloadService::class.java.name + ".INTENT.DOWNLOAD"

        private const val NOTIFICATION_ID = 2
        private const val NOTIFICATION_CHANNEL = "downloading_channel"

        fun queueDownload(context: Context, user: User, result: YoutubeSearchResult) {
            val intent = Intent(context, DownloadService::class.java)
            intent.putExtra(INTENT_USER, user)
            intent.putExtra(INTENT_DOWNLOAD, result)
            context.startService(intent)
        }
    }

    private var user: User? = null
    private var server: YoutubeServer? = null

    private val downloadQueue = LinkedBlockingQueue<YoutubeSearchResult>()
    private val downloading = AtomicBoolean()
    private var handler: Handler? = null
    private var downloadTask: DownloadTask? = null

    private var isForeground: Boolean = false

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CANCEL_ALL) {
                downloadQueue.clear()
            }
            downloadTask?.cancel()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        server = YoutubeServer(this)
        handler = Handler()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL) == null) {
                notificationManager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL,
                        getString(R.string.downloading), NotificationManager.IMPORTANCE_LOW))
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_CANCEL)
        intentFilter.addAction(ACTION_CANCEL_ALL)
        registerReceiver(cancelReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()

        user = null
        downloadTask?.cancel()
        unregisterReceiver(cancelReceiver)

        stopForeground(true)
        isForeground = false
    }

    private fun showLoadingNotification(result: YoutubeSearchResult) {
        val manager = NotificationManagerCompat.from(this)

        val builder = getBaseNotification(result)
                .setProgress(0, 0, true)

        if (isForeground) {
            manager.notify(NOTIFICATION_ID, builder.build())
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
            isForeground = true
        }
    }

    private fun showProgressNotification(result: YoutubeSearchResult, progress: Int) {
        if (progress % 20 != 0) return

        val manager = NotificationManagerCompat.from(this)

        val pendingIntentCancel = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_CANCEL), 0)
        val pendingIntentCancelAll = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_CANCEL_ALL), 0)

        val builder = getBaseNotification(result)
                .addAction(0, getString(R.string.cancel), pendingIntentCancel)
                .addAction(0, getString(R.string.cancel_all), pendingIntentCancelAll)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, progress, false)
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun getBaseNotification(result: YoutubeSearchResult): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle(result.title)
                .setContentText(getString(R.string.downloading))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_download)
    }

    private fun startDownloading() {
        if (downloading.get()) return
        downloading.set(true)
        val result = downloadQueue.poll()

        fetchUrl(result, 0)
        showLoadingNotification(result)
    }

    private fun fetchUrl(result: YoutubeSearchResult, retries: Int) {
        if (retries > 10 || user == null) {
            onFinish(result)
        }

        val youtube = Youtube()
        youtube.apikey = user!!.apikey
        youtube.id = result.id

        server!!.fetchSong(youtube, object : YoutubeServer.YoutubeSongIdCallback {
            override fun onSuccess(url: String) {
                val uri = Uri.parse(url)
                val serverUri = Uri.parse(Settings.getServerUrl(applicationContext))
                if (uri.host == serverUri.host && !uri.queryParameterNames.contains("url")) {

                    downloadTask = DownloadTask(this@DownloadService, url, result)
                    downloadTask!!.execute()
                } else {
                    handler!!.postDelayed({ fetchUrl(result, retries + 1) }, (1000 * 10).toLong())
                }
            }

            override fun onFailure(code: Int) {
                onFinish(result)
            }
        })
    }

    override fun onFinish(result: YoutubeSearchResult) {
        if (result.getDownloadPath(this).exists()) {
            val intent = Intent(ACTION_DOWNLOADED)
            intent.putExtra(INTENT_DOWNLOAD, result)
            sendBroadcast(intent)
            result.save(this)
        }

        downloading.set(false)
        if (downloadQueue.size > 0) {
            startDownloading()
        } else {
            stopSelf()
        }
    }

    override fun onProgress(result: YoutubeSearchResult, progress: Int) {
        showProgressNotification(result, progress)
    }

    class DownloadTask constructor(service: DownloadService,
                                   private val url: String, private val result: YoutubeSearchResult)
        : AsyncTask<Void, Int, Void>() {

        private val downloadListenerRef: WeakReference<DownloadListener> = WeakReference(service)
        private val file: File = result.getDownloadPath(service)
        private val downloadingFile: File
        private var cancel: Boolean = false

        private var connection: HttpURLConnection? = null

        init {
            downloadingFile = File(file.toString() + ".downloading")
            downloadingFile.parentFile.mkdirs()
        }

        private fun getContentLength(connection: HttpURLConnection): Long {
            var value = connection.getHeaderField("Content-Range")
            if (value != null) {
                return java.lang.Long.parseLong(value.substring(value.lastIndexOf('/') + 1))
            } else {
                value = connection.getHeaderField("Content-Length")
                if (value != null) {
                    return java.lang.Long.parseLong(value)
                }
            }
            return 0
        }

        override fun doInBackground(vararg voids: Void): Void? {
            if (file.exists()) return null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection!!.requestMethod = "GET"

                var total: Long = 0
                if (downloadingFile.exists()) {
                    val size = downloadingFile.length()
                    connection!!.setRequestProperty("Range", "bytes=$size-")
                    total += size
                }
                connection!!.connect()

                val statusCode = connection!!.responseCode
                if (statusCode < 200 || statusCode >= 300) {
                    return null
                }

                val contentLength = getContentLength(connection!!)
                onProgressUpdate((total / contentLength * 100).toInt())

                val outputStream = FileOutputStream(downloadingFile,
                        statusCode == HttpURLConnection.HTTP_PARTIAL)
                val buf = ByteArray(8192)

                val inputStream = DataInputStream(connection!!.inputStream)
                var read = inputStream.read(buf)

                if (cancel) return null

                while (read > 0 && !cancel) {
                    outputStream.write(buf, 0, read)
                    outputStream.flush()
                    total += read.toLong()
                    onProgressUpdate((total * 100 / contentLength).toInt())

                    read = inputStream.read(buf)
                }

                if (total == contentLength && !cancel) {
                    downloadingFile.renameTo(file)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
            return null
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            val downloadListener = downloadListenerRef.get()
            downloadListener?.onProgress(result, values[0]!!)
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            val downloadListener = downloadListenerRef.get()
            downloadListener?.onFinish(result)
        }

        fun cancel() {
            cancel = true
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        user = intent.getSerializableExtra(INTENT_USER) as User
        val result = intent.getSerializableExtra(INTENT_DOWNLOAD) as YoutubeSearchResult

        if (!downloadQueue.contains(result)) {
            downloadQueue.offer(result)
            if (downloadQueue.size == 1) {
                startDownloading()
            }
        }

        return Service.START_STICKY
    }
}
