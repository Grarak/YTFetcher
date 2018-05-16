package com.grarak.ytfetcher.views;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.grarak.ytfetcher.R;

public class MusicVisualizerView extends View implements Visualizer.OnDataCaptureListener {

    private byte[] waveform;
    private final Paint paint;
    private final int gap;
    private int[] colors;

    private Visualizer visualizer;
    private boolean enabled;

    public MusicVisualizerView(Context context) {
        this(context, null);
    }

    public MusicVisualizerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        colors = new int[1];
        colors[0] = ContextCompat.getColor(context, R.color.colorPrimaryDark);

        gap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                5, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (this) {
            if (waveform == null) return;

            int chunkSize = 30;
            float height = getMeasuredHeight();
            float chunkWidth = getMeasuredWidth() / chunkSize;

            int color = 0;
            for (int i = 0; i < chunkSize; i++) {
                float left = chunkWidth * i;
                float top = height - getAverage(
                        i * chunkSize, chunkSize) / (1 << 8) * height;
                float right = left + chunkWidth;
                float bottom = height;

                paint.setColor(colors[color++]);
                canvas.drawRect(left + gap / 2f, top, right - gap / 2f, bottom, paint);

                if (color >= colors.length) {
                    color = 0;
                }
            }
        }
    }

    private float getAverage(int start, int size) {
        int sum = 0;
        for (int i = start; i < start + size; i++) {
            sum += waveform[i] & 0xff;
        }
        return sum / size;
    }

    public synchronized void setAudioSessionId(int id) {
        if (ContextCompat.checkSelfPermission(
                getContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (visualizer != null) {
                visualizer.release();
            }

            visualizer = new Visualizer(id);
            visualizer.setEnabled(false);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(this,
                    Visualizer.getMaxCaptureRate() / 2, true, false);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
        if (!enabled) return;
        synchronized (this) {
            this.waveform = waveform;
            invalidate();
        }
    }

    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
    }
}
