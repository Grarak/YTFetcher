package com.grarak.ytfetcher.utils.server.youtube

import android.content.Context

import com.google.gson.GsonBuilder
import com.grarak.ytfetcher.utils.Prefs
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.Gson

import java.io.File

class YoutubeSearchResult : Gson() {

    var title: String? = null
    var id: String? = null
    var thumbnail: String? = null
    var duration: String? = null

    fun getDownloadPath(context: Context): File {
        return File(Utils.getDownloadFolder(context).toString() + "/" + id + ".ogg")
    }

    fun save(context: Context) {
        Prefs.saveString("result_" + id!!, toString(), context)
    }

    fun delete(context: Context): Boolean {
        if (getDownloadPath(context).delete()) {
            Prefs.remove("result_" + id!!, context)
            return true
        }
        return false
    }

    companion object {

        fun fromString(json: String?): YoutubeSearchResult? {
            return try {
                GsonBuilder().create().fromJson(json, YoutubeSearchResult::class.java)
            } catch (ignored: Exception) {
                null
            }
        }

        fun restore(id: String, context: Context): YoutubeSearchResult? {
            return fromString(Prefs.getString("result_$id", null, context))
        }
    }
}
