package com.grarak.ytfetcher.utils.server.playlist

import android.content.Context

import com.google.gson.GsonBuilder
import com.grarak.ytfetcher.utils.Prefs
import com.grarak.ytfetcher.utils.server.Gson
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

class PlaylistResults : Gson() {

    var name: String? = null
    var songs: ArrayList<YoutubeSearchResult>? = null

    fun save(context: Context) {
        Prefs.saveString("playlist_" + name!!, toString(), context)
    }

    companion object {

        fun restore(name: String, context: Context): PlaylistResults? {
            return GsonBuilder().create().fromJson(
                    Prefs.getString("playlist_$name",
                            null, context), PlaylistResults::class.java)
        }
    }
}
