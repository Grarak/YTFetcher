package com.grarak.ytfetcher.utils.server

import android.os.Handler
import android.os.Looper
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class Request internal constructor() : Closeable {

    private var connection: HttpURLConnection? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean()

    interface RequestCallback {
        fun onConnect(request: Request, status: Int, url: String)

        fun onSuccess(request: Request, status: Int,
                      headers: Map<String, List<String>>, response: String)

        fun onFailure(request: Request, e: Exception?)
    }

    internal fun doRequest(url: String, contentType: String?,
                           data: String?, requestCallback: RequestCallback) {
        closed.set(false)
        var reader: BufferedReader? = null
        var outputStream: DataOutputStream? = null

        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection!!.connectTimeout = 5000
            if (contentType != null) {
                connection!!.setRequestProperty("Content-Type", contentType)
            }
            if (data != null) {
                connection!!.requestMethod = "POST"
                connection!!.doOutput = true
            } else {
                connection!!.requestMethod = "GET"
            }
            connection!!.connect()

            if (data != null) {
                outputStream = DataOutputStream(connection!!.outputStream)
                outputStream.writeBytes(data)
                outputStream.flush()
            }

            val statusCode = connection!!.responseCode
            when (statusCode) {
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER -> {
                    val newUrl = connection!!.getHeaderField("Location")
                    if (newUrl == null) {
                        handler.post { requestCallback.onFailure(this, null) }
                    } else {
                        doRequest(newUrl, contentType, data, requestCallback)
                    }
                    return
                }
            }

            handler.post { requestCallback.onConnect(this, statusCode, url) }
            val inputStream: InputStream
            if (statusCode < 200 || statusCode >= 300) {
                inputStream = connection!!.errorStream
            } else {
                inputStream = connection!!.inputStream
            }
            reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line = reader.readLine()
            while (line != null) {
                response.append(line).append("\n")
                line = reader.readLine()
            }
            handler.post {
                requestCallback.onSuccess(this, statusCode,
                        connection!!.headerFields, response.toString())
            }
        } catch (e: IOException) {
            if (!closed.get()) {
                handler.post { requestCallback.onFailure(this, e) }
            }
        } finally {
            if (connection != null) {
                connection!!.disconnect()
            }

            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (ignored: IOException) {
                }

            }
            if (reader != null) {
                try {
                    reader.close()
                } catch (ignored: IOException) {
                }

            }
        }
    }

    override fun close() {
        closed.set(true)
        Thread {
            if (connection != null) {
                connection!!.disconnect()
            }
        }.start()
    }
}
