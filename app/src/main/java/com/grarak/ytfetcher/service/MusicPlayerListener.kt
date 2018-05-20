package com.grarak.ytfetcher.service

import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

interface MusicPlayerListener {
    fun onConnect()

    fun onPreparing(results: List<YoutubeSearchResult>, position: Int)

    fun onFailure(code: Int, results: List<YoutubeSearchResult>, position: Int)

    fun onPlay(results: List<YoutubeSearchResult>, position: Int)

    fun onPause(results: List<YoutubeSearchResult>, position: Int)

    fun onAudioSessionIdChanged(id: Int)

    fun onDisconnect()
}
