package com.grarak.ytfetcher.utils

import android.content.Context
import android.preference.PreferenceManager

object Prefs {

    fun clear(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()
    }

    fun remove(name: String, context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(name).apply()
    }

    fun getInt(name: String, defaults: Int, context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(name, defaults)
    }

    fun saveInt(name: String, value: Int, context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(name, value).apply()
    }

    fun getBoolean(name: String, defaults: Boolean, context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(name, defaults)
    }

    fun saveBoolean(name: String, value: Boolean, context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(name, value).apply()
    }

    fun getString(name: String, defaults: String?, context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(name, defaults)
    }

    fun saveString(name: String, value: String, context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(name, value).apply()
    }
}
