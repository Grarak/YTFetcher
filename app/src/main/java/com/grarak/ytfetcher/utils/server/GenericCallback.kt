package com.grarak.ytfetcher.utils.server

interface GenericCallback {
    fun onSuccess()

    fun onFailure(code: Int)
}
