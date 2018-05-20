package com.grarak.ytfetcher.utils.server.user

import android.content.Context

import com.google.gson.GsonBuilder
import com.grarak.ytfetcher.utils.Prefs
import com.grarak.ytfetcher.utils.server.Gson

class User : Gson() {

    var apikey: String? = null
    var name: String? = null
    var password: String? = null
    var admin: Boolean = false
    var verified: Boolean = false

    fun save(context: Context) {
        Prefs.saveString("user", toString(), context)
    }

    companion object {

        fun restore(context: Context): User? {
            return fromString(Prefs.getString("user", null, context))
        }

        fun delete(context: Context) {
            Prefs.remove("user", context)
        }

        fun fromString(json: String?): User? {
            try {
                return GsonBuilder().create().fromJson(json, User::class.java)
            } catch (ignored: Exception) {
                return null
            }

        }
    }
}
