package com.grarak.ytfetcher.utils.server.youtube

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grarak.ytfetcher.utils.Prefs
import java.util.*

class YoutubeCharts internal constructor(json: String?) : Iterable<YoutubeSearchResult> {

    var items: MutableList<YoutubeSearchResult> = ArrayList()

    init {
        try {
            val listType = object : TypeToken<List<YoutubeSearchResult>>() {

            }.type
            items.addAll(GsonBuilder().create().fromJson(json, listType))
        } catch (ignored: Exception) {
        }

    }

    override fun toString(): String {
        return GsonBuilder().create().toJson(items)
    }

    fun save(context: Context) {
        Prefs.saveString("youtube_charts", toString(), context)
    }

    override fun iterator(): Iterator<YoutubeSearchResult> {
        return items.iterator()
    }

    companion object {

        fun restore(context: Context): YoutubeCharts {
            return YoutubeCharts(Prefs.getString("youtube_charts", null, context))
        }
    }
}
