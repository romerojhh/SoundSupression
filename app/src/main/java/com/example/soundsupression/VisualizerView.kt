package com.example.soundsupression

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

/**
 * A simple class that draws waveform data received from a
 * [Visualizer.OnDataCaptureListener.onWaveFormDataCapture]
 */
internal class VisualizerView(context: Context?) : View(context) {
    private var mBytes: ByteArray? = null
    private var mPoints: FloatArray? = null
    private val mRect = Rect()
    private val mForePaint = Paint()

    init {
        init()
    }

    private fun init() {
        mBytes = null
        mForePaint.strokeWidth = 1f
        mForePaint.isAntiAlias = true
        mForePaint.color = Color.rgb(0, 128, 255)
    }

    fun updateVisualizer(bytes: ByteArray?) {
        mBytes = bytes
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBytes == null) {
            return
        }
        if (mPoints == null || mPoints!!.size < mBytes!!.size * 4) {
            mPoints = FloatArray(mBytes!!.size * 4)
        }
        mRect[0, 0, width] = height
        for (i in 0 until mBytes!!.size - 1) {
            mPoints!![i * 4] = mRect.width() * i / (mBytes!!.size - 1).toFloat()
            mPoints!![i * 4 + 1] = (mRect.height() / 2f
                    + (mBytes!![i] + 128).toByte() * (mRect.height() / 2f) / 128)
            mPoints!![i * 4 + 2] = mRect.width() * (i + 1) / (mBytes!!.size - 1).toFloat()
            mPoints!![i * 4 + 3] = (mRect.height() / 2f
                    + (mBytes!![i + 1] + 128).toByte() * (mRect.height() / 2f) / 128)
        }
        canvas.drawLines(mPoints!!, mForePaint)
    }
}