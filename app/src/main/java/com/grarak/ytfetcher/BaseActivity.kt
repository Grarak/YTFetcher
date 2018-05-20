package com.grarak.ytfetcher

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate

import com.crashlytics.android.Crashlytics
import com.grarak.ytfetcher.utils.Settings

import io.fabric.sdk.android.Fabric

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
        }
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setTheme(if (Settings.isDarkTheme(this))
            R.style.AppThemeDark
        else
            R.style.AppThemeLight)
        super.onCreate(savedInstanceState)
    }
}
