package com.grarak.ytfetcher.utils.server.youtube

import com.google.gson.GsonBuilder
import com.grarak.ytfetcher.utils.server.Gson

class Youtube : Gson() {

    var apikey: String? = null
    var searchquery: String? = null
    var id: String? = null
    var addhistory: Boolean = false

    companion object {

        fun fromString(json: String): Youtube? {
            return try {
                GsonBuilder().create().fromJson(json, Youtube::class.java)
            } catch (ignored: Exception) {
                null
            }
        }
    }
}
