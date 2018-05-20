package com.grarak.ytfetcher.service

import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult

interface DownloadListener {
    fun onFinish(result: YoutubeSearchResult)

    fun onProgress(result: YoutubeSearchResult, progress: Int)
}
