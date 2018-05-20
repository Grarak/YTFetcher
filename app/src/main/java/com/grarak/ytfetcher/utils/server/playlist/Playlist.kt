package com.grarak.ytfetcher.utils.server.playlist

import com.google.gson.annotations.SerializedName
import com.grarak.ytfetcher.utils.server.Gson

class Playlist : Gson() {

    var apikey: String? = null
    var name: String? = null
    @SerializedName("public")
    var isPublic: Boolean = false
}
