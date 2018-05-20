package com.grarak.ytfetcher.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewTreeObserver
import com.grarak.ytfetcher.MainActivity
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.service.DownloadService
import com.grarak.ytfetcher.utils.EqualizerManager
import com.grarak.ytfetcher.utils.MusicManager
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Status
import com.grarak.ytfetcher.utils.server.playlist.PlaylistId
import com.grarak.ytfetcher.utils.server.playlist.PlaylistServer
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

open class BaseFragment : Fragment() {

    var user: User? = null
        get() {
            if (field == null) {
                field = arguments!!.getSerializable(MainActivity.USER_INTENT) as User
            }
            return field
        }

    private var playlistServer: PlaylistServer? = null
    private var resultToAddPlaylist: YoutubeSearchResult? = null
    private val resultsToQueue = LinkedBlockingQueue<YoutubeSearchResult>()

    val bottomNavigationView: BottomNavigationView?
        get() = if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView
        } else null

    val musicManager: MusicManager?
        get() = (activity as MainActivity).musicManager

    val equalizerManager: EqualizerManager?
        get() = musicManager!!.equalizerManager

    var availablePlaylists: ArrayList<String>
        get() = (activity as MainActivity).availablePlaylists
        set(value) {
            (activity as MainActivity).availablePlaylists = value
        }

    private val downloadedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val result = intent.getSerializableExtra(DownloadService.INTENT_DOWNLOAD) as YoutubeSearchResult
            onDownloaded(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlistServer = PlaylistServer(activity!!)
        savedInstanceState?.run {
            resultToAddPlaylist = getSerializable("resultToAddPlaylist")as? YoutubeSearchResult
        }
    }

    override fun onResume() {
        super.onResume()
        resultToAddPlaylist?.run {
            showPlaylistDialog(this)
        }

        activity!!.registerReceiver(downloadedReceiver,
                IntentFilter(DownloadService.ACTION_DOWNLOADED))
    }

    override fun onPause() {
        super.onPause()

        activity!!.unregisterReceiver(downloadedReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeGlobalOnLayoutListener(this)
                if (isAdded) {
                    onViewFinished()
                }
            }
        })
    }

    open fun onViewFinished() {}

    open fun onViewPagerResume() {}

    open fun onViewPagerPause() {}

    open fun onBackPressed(): Boolean {
        return false
    }

    fun showForegroundFragment(fragment: Fragment) {
        (activity as MainActivity).showForegroundFragment(fragment)
    }

    fun removeForegroundFragment(fragment: Fragment) {
        (activity as MainActivity).removeForegroundFragment(fragment)
    }

    fun showPlaylistDialog(result: YoutubeSearchResult) {
        if (availablePlaylists.size == 0) {
            Utils.toast(R.string.no_playlists, activity!!)
            return
        }

        resultToAddPlaylist = result
        val playlists = availablePlaylists.toTypedArray()
        AlertDialog.Builder(activity!!).setItems(playlists) { _, which ->
            val playlistId = PlaylistId()
            playlistId.apikey = user!!.apikey
            playlistId.name = playlists[which]
            playlistId.id = result.id
            playlistServer!!.addToPlaylist(playlistId, object : GenericCallback {
                override fun onSuccess() {}

                override fun onFailure(code: Int) {
                    if (code == Status.PlaylistIdAlreadyExists) {
                        Utils.toast(R.string.already_in_playlist, activity!!)
                    } else {
                        Utils.toast(R.string.failed_add_playlist, activity!!)
                    }
                }
            })
        }.setOnDismissListener { resultToAddPlaylist = null }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("resultToAddPlaylist", resultToAddPlaylist)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted(requestCode)
        } else {
            onPermissionDenied(requestCode)
        }
    }

    fun requestPermissions(request: Int, vararg permissions: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val needrequest = ArrayList<String>()
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(activity!!, permission) != PackageManager.PERMISSION_GRANTED) {
                    needrequest.add(permission)
                }
            }
            if (!needrequest.isEmpty()) {
                requestPermissions(needrequest.toTypedArray(), request)
                return
            }
        }
        onPermissionGranted(request)
    }

    fun onPermissionGranted(request: Int) {
        if (request == 0) {
            while (resultsToQueue.size != 0) {
                DownloadService.queueDownload(activity!!, user!!, resultsToQueue.poll())
            }
        }
    }

    fun onPermissionDenied(request: Int) {
        Utils.toast(R.string.no_permissions, activity!!)
    }

    fun queueDownload(result: YoutubeSearchResult) {
        if (Integer.parseInt(result.duration!!.substring(0, result.duration!!.indexOf(':'))) > 20) {
            Utils.toast(getString(R.string.too_long, result.title), activity!!)
            return
        }
        if (result.getDownloadPath(activity!!).exists()) {
            return
        }
        resultsToQueue.offer(result)
        requestPermissions(0, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    protected open fun onDownloaded(result: YoutubeSearchResult) {}

    fun deleteResult(result: YoutubeSearchResult): Boolean {
        val currentTrack = musicManager!!.trackPosition
        if (currentTrack >= 0) {
            if (musicManager!!.tracks[currentTrack] == result) {
                Utils.toast(R.string.delete_not_possible, activity!!)
                return false
            }
        }
        return result.delete(activity!!)
    }

    open fun onRemoveForeground() {}
}
