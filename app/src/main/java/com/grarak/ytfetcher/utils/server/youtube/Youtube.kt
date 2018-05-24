package com.grarak.ytfetcher.utils.server.youtube

import com.grarak.ytfetcher.utils.server.Gson

class Youtube : Gson() {

    var apikey: String? = null
    var searchquery: String? = null
    var id: String? = null
    var addhistory: Boolean = false
}
