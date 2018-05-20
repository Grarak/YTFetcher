package com.grarak.ytfetcher.utils

import android.content.Context

object Settings {

    fun setDarkTheme(enabled: Boolean, context: Context) {
        Prefs.saveBoolean("dark_theme", enabled, context)
    }

    fun isDarkTheme(context: Context): Boolean {
        return Prefs.getBoolean("dark_theme", true, context)
    }

    fun getLastSearch(context: Context): String {
        return Prefs.getString("last_search", "", context) ?: ""
    }

    fun setLastSearch(context: Context, search: String) {
        Prefs.saveString("last_search", search, context)
    }

    fun setPage(context: Context, page: Int) {
        Prefs.saveInt("page", page, context)
    }

    fun getPage(context: Context): Int {
        return Prefs.getInt("page", 0, context)
    }

    fun setServerUrl(url: String, context: Context) {
        Prefs.saveString("server_url", url, context)
    }

    fun getServerUrl(context: Context): String {
        return Prefs.getString("server_url", "", context) ?: ""
    }
}
