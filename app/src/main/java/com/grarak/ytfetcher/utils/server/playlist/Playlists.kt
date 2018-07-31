package com.grarak.ytfetcher.utils.server.playlist

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grarak.ytfetcher.utils.Prefs
import com.grarak.ytfetcher.utils.server.Gson
import java.util.*

class Playlists internal constructor(json: String?) : Gson(), Iterable<Playlist> {

    var items: MutableList<Playlist> = ArrayList()

    init {
        try {
            val listType = object : TypeToken<List<Playlist>>() {

            }.type
            items.addAll(GsonBuilder().create().fromJson(json, listType))
        } catch (ignored: Exception) {
        }

    }

    override fun toString(): String {
        return GsonBuilder().create().toJson(items)
    }

    fun save(context: Context) {
        Prefs.saveString("playlists", toString(), context)
    }

    override fun iterator(): Iterator<Playlist> {
        return items.iterator()
    }

    companion object {
        fun restore(context: Context): Playlists {
            return Playlists(Prefs.getString("playlists", null, context))
        }
    }
}
