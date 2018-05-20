package com.grarak.ytfetcher.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import android.support.annotation.StringRes
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import java.io.File
import java.util.regex.Pattern

object Utils {

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        var width = drawable.intrinsicWidth
        width = if (width > 0) width else 1
        var height = drawable.intrinsicHeight
        height = if (height > 0) height else 1

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    fun formatResultTitle(result: YoutubeSearchResult): Array<String?> {
        val matcher = Pattern.compile("(.+)[:| -] (.+)").matcher(result.title!!)
        if (matcher.matches()) {
            return arrayOf(matcher.group(1), matcher.group(2))
        }

        var title = result.title
        var contentText = result.id
        if (title!!.length > 20) {
            val tmp = title.substring(20)
            val whitespaceIndex = tmp.indexOf(' ')
            if (whitespaceIndex >= 0) {
                val firstWhitespace = 20 + tmp.indexOf(' ')
                contentText = title.substring(firstWhitespace + 1)
                title = title.substring(0, firstWhitespace)
            }
        }
        return arrayOf(title, contentText)
    }

    fun getDownloadFolder(context: Context): File {
        return File(Environment.getExternalStorageDirectory().toString()
                + "/Android/data/" + context.packageName + "/files")
    }

    fun formatSeconds(secs: Long): String {
        val seconds = secs % 60
        val minutes = secs / 60
        var format = ""
        if (minutes < 10) {
            format += "0"
        }
        format += minutes.toString() + ":"
        if (seconds < 10) {
            format += "0"
        }
        format += seconds
        return format
    }

    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun encode(text: String): String {
        return Base64.encodeToString(text.toByteArray(), Base64.DEFAULT)
    }

    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun toast(@StringRes message: Int, context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun toast(message: String, context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
