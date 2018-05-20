package com.grarak.ytfetcher.utils.server.youtube

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.server.Request
import com.grarak.ytfetcher.utils.server.Server
import com.grarak.ytfetcher.utils.server.Status
import java.net.HttpURLConnection
import java.net.URLEncoder

class YoutubeServer(private val context: Context) : Server(Settings.getServerUrl(context)) {

    interface YoutubeSongIdCallback {
        fun onSuccess(url: String)

        fun onFailure(code: Int)
    }

    interface YoutubeChartsCallback {
        fun onSuccess(youtubeCharts: YoutubeCharts)

        fun onFailure(code: Int)
    }

    interface YoutubeResultsCallback {
        fun onSuccess(youtubeSearchResults: List<YoutubeSearchResult>?)

        fun onFailure(code: Int)
    }

    interface YoutubeResultCallback {
        fun onSuccess(result: YoutubeSearchResult)

        fun onFailure(code: Int)
    }

    fun fetchSong(youtube: Youtube, youtubeSongIdCallback: YoutubeSongIdCallback) {
        post(getUrl("youtube/fetch"), youtube.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    if (headers.containsKey("ytfetcher-id")) {
                        verifyFetchedSong(response.trim { it <= ' ' },
                                headers["ytfetcher-id"]!![0], youtubeSongIdCallback)
                    } else {
                        youtubeSongIdCallback.onSuccess(response)
                    }
                } else {
                    youtubeSongIdCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                youtubeSongIdCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    private fun verifyFetchedSong(url: String, id: String, youtubeSongIdCallback: YoutubeSongIdCallback) {
        get(url, object : Request.RequestCallback {

            private val newUrl = (getUrl("youtube/get?id=")
                    + URLEncoder.encode(id)
                    + "&url=" + URLEncoder.encode(url))

            override fun onConnect(request: Request, status: Int, url: String) {
                request.close()
                if (status in 200..299) {
                    youtubeSongIdCallback.onSuccess(url)
                } else {
                    verifyForwardedSong()
                }
            }

            override fun onSuccess(request: Request, status: Int, headers: Map<String, List<String>>, response: String) {}

            override fun onFailure(request: Request, e: Exception?) {
                verifyForwardedSong()
            }

            private fun verifyForwardedSong() {
                get(newUrl, object : Request.RequestCallback {
                    override fun onConnect(request: Request, status: Int, url: String) {
                        request.close()
                        youtubeSongIdCallback.onSuccess(url)
                    }

                    override fun onSuccess(request: Request, status: Int, headers: Map<String, List<String>>, response: String) {}

                    override fun onFailure(request: Request, e: Exception?) {
                        youtubeSongIdCallback.onFailure(Status.ServerOffline)
                    }
                })
            }
        })
    }

    fun search(youtube: Youtube, youtubeResultsCallback: YoutubeResultsCallback) {
        post(getUrl("youtube/search"), youtube.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val listType = object : TypeToken<List<YoutubeSearchResult>>() {

                    }.type
                    val results = GsonBuilder().create().fromJson<List<YoutubeSearchResult>>(response, listType)
                    if (results != null) {
                        youtubeResultsCallback.onSuccess(results)
                    } else {
                        youtubeResultsCallback.onFailure(Status.ServerOffline)
                    }
                } else {
                    youtubeResultsCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                youtubeResultsCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun getCharts(youtube: Youtube, youtubeChartsCallback: YoutubeChartsCallback) {
        post(getUrl("youtube/getcharts"), youtube.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val youtubeCharts = YoutubeCharts(response)
                    if (youtubeCharts.items.size > 0) {
                        youtubeChartsCallback.onSuccess(youtubeCharts)
                    } else {
                        youtubeChartsCallback.onFailure(Status.ServerOffline)
                    }
                } else {
                    youtubeChartsCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                youtubeChartsCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun getInfo(youtube: Youtube, youtubeResultCallback: YoutubeResultCallback) {
        val savedResult = YoutubeSearchResult.restore(youtube.id!!, context)
        if (savedResult != null) {
            youtubeResultCallback.onSuccess(savedResult)
            return
        }

        post(getUrl("youtube/getinfo"), youtube.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val result = YoutubeSearchResult.fromString(response)
                    if (result != null) {
                        youtubeResultCallback.onSuccess(result)
                    } else {
                        youtubeResultCallback.onFailure(Status.ServerOffline)
                    }
                } else {
                    youtubeResultCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                youtubeResultCallback.onFailure(Status.ServerOffline)
            }
        })
    }
}
