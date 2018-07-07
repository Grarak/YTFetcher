package com.grarak.ytfetcher.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.grarak.ytfetcher.R

class MusicVisualizerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr), Visualizer.OnDataCaptureListener {

    private var waveform: ByteArray? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gap: Int
    private val colors = IntArray(1)

    private var visualizer: Visualizer? = null
    private var enabled: Boolean = false

    init {

        colors[0] = ContextCompat.getColor(context, R.color.colorPrimaryDark)

        gap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                5f, context.resources.displayMetrics).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(this) {
            if (waveform == null) return

            val chunkSize = 30
            val height = measuredHeight.toFloat()
            val chunkWidth = (measuredWidth / chunkSize).toFloat()

            var color = 0
            for (i in 0 until chunkSize) {
                val left = chunkWidth * i
                val top = height - getAverage(
                        i * chunkSize, chunkSize) / (1 shl 8) * height
                val right = left + chunkWidth

                paint.color = colors[color++]
                canvas.drawRect(left + gap / 2f, top, right - gap / 2f, height, paint)

                if (color >= colors.size) {
                    color = 0
                }
            }
        }
    }

    private fun getAverage(start: Int, size: Int): Float {
        var sum = 0
        for (i in start until start + size) {
            sum += waveform!![i].toInt() and 0xff
        }
        return (sum / size).toFloat()
    }

    @Synchronized
    fun setAudioSessionId(id: Int) {
        if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            visualizer?.release()

            visualizer = Visualizer(id)
            visualizer!!.enabled = false
            visualizer!!.captureSize = Visualizer.getCaptureSizeRange()[1]
            visualizer!!.setDataCaptureListener(this,
                    Visualizer.getMaxCaptureRate() / 2, true, false)
            visualizer!!.enabled = true
        } catch (ignored: Exception) {
        }
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
        if (!enabled) return
        synchronized(this) {
            this.waveform = waveform
            invalidate()
        }
    }

    override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {}
}
