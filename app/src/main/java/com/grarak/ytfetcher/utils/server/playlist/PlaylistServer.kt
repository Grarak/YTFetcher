package com.grarak.ytfetcher.utils.server.playlist

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Request
import com.grarak.ytfetcher.utils.server.Server
import com.grarak.ytfetcher.utils.server.Status
import java.net.HttpURLConnection

class PlaylistServer(context: Context) : Server(Settings.getServerUrl(context)) {

    interface PlaylistListCallback {
        fun onSuccess(playlists: Playlists)

        fun onFailure(code: Int)
    }

    interface PlayListIdsCallback {
        fun onSuccess(ids: List<String>)

        fun onFailure(code: Int)
    }

    fun list(apiKey: String, playlistListCallback: PlaylistListCallback) {
        post(getUrl("users/playlist/list"),
                String.format("{\"apikey\":\"%s\"}", apiKey), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    playlistListCallback.onSuccess(Playlists(response))
                } else {
                    playlistListCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                playlistListCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun listPublic(playlist: Playlist, playlistListCallback: PlaylistListCallback) {
        post(getUrl("users/playlist/listpublic"), playlist.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    playlistListCallback.onSuccess(Playlists(response))
                } else {
                    playlistListCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                playlistListCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun create(playlist: Playlist, genericCallback: GenericCallback) {
        post(getUrl("users/playlist/create"), playlist.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun setPublic(playlist: Playlist, genericCallback: GenericCallback) {
        post(getUrl("users/playlist/setpublic"), playlist.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun delete(playlist: Playlist, genericCallback: GenericCallback) {
        post(getUrl("users/playlist/delete"), playlist.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun addToPlaylist(playlistId: PlaylistId, genericCallback: GenericCallback) {
        post(getUrl("users/playlist/addid"), playlistId.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun deleteFromPlaylist(playlistId: PlaylistId, genericCallback: GenericCallback) {
        post(getUrl("users/playlist/deleteid"), playlistId.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun listPlaylistIds(playlist: Playlist, playListIdsCallback: PlayListIdsCallback) {
        post(getUrl("users/playlist/listids"), playlist.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val listType = object : TypeToken<List<String>>() {

                    }.type
                    val results = GsonBuilder().create().fromJson<List<String>>(response, listType)
                    playListIdsCallback.onSuccess(results)
                } else {
                    playListIdsCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                playListIdsCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun listPlaylistIdsPublic(playlistPublic: PlaylistPublic, playListIdsCallback: PlayListIdsCallback) {
        post(getUrl("users/playlist/listidspublic"), playlistPublic.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val listType = object : TypeToken<List<String>>() {

                    }.type
                    val results = GsonBuilder().create().fromJson<List<String>>(response, listType)
                    playListIdsCallback.onSuccess(results)
                } else {
                    playListIdsCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                playListIdsCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun setPlaylistIds(playlistIds: PlaylistIds, genericCallback: GenericCallback) {
        post(getUrl("users/playlist/setids"), playlistIds.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
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
}
