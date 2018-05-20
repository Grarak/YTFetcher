package com.grarak.ytfetcher.utils.server.history

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Request
import com.grarak.ytfetcher.utils.server.Server
import com.grarak.ytfetcher.utils.server.Status
import java.net.HttpURLConnection

class HistoryServer(context: Context) : Server(Settings.getServerUrl(context)) {

    interface HistoryCallback {
        fun onSuccess(history: List<String>)

        fun onFailure(code: Int)
    }

    fun add(history: History, genericCallback: GenericCallback) {
        post(getUrl("users/history/add"), history.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int, headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(Status.ServerOffline)
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun httpGet(apiKey: String, historyCallback: HistoryCallback) {
        post(getUrl("users/history/list"), String.format("{\"apikey\":\"%s\"}", apiKey), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int, headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val listType = object : TypeToken<List<String>>() {

                    }.type
                    val results = GsonBuilder().create().fromJson<List<String>>(response, listType)
                    historyCallback.onSuccess(results)
                } else {
                    historyCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                historyCallback.onFailure(Status.ServerOffline)
            }
        })
    }


}
