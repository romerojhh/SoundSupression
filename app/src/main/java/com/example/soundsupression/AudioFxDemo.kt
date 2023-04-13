package com.example.soundsupression

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class AudioFxDemo : Activity() {
    private var mLinearLayout: LinearLayout? = null
    private var mVisualizerView: VisualizerView? = null
    private var mStatusTextView: TextView? = null
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        volumeControlStream = AudioManager.STREAM_MUSIC
        mStatusTextView = TextView(this)
        mLinearLayout = LinearLayout(this)
        mLinearLayout!!.orientation = LinearLayout.VERTICAL
        mLinearLayout!!.addView(mStatusTextView)
        setContentView(mLinearLayout)
        // Create the MediaPlayer
        checkRecordAudioPermission()
        setupVisualizerFxAndUI()
        mStatusTextView!!.text = "Say something.."
    }

    private var audioRecord: AudioRecord? = null
    private var bufferSize = 0
    private var audioDataQueue: BlockingQueue<ByteArray>? = null
    @SuppressLint("MissingPermission")
    fun startRecording() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_8BIT
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioDataQueue = LinkedBlockingQueue()
        Thread {
            val buffer = ByteArray(bufferSize)
            audioRecord!!.startRecording()
            while (!Thread.currentThread().isInterrupted) {
                val numBytes = audioRecord!!.read(buffer, 0, bufferSize)
                val data = ByteArray(numBytes)
                System.arraycopy(buffer, 0, data, 0, numBytes)
                try {
                    (audioDataQueue as LinkedBlockingQueue<ByteArray>).put(data)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            audioRecord!!.stop()
            audioRecord!!.release()
        }.start()
    }

    private fun checkRecordAudioPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission has not been granted yet, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_RECORD_AUDIO
            )
        } else {
            // Permission has already been granted, proceed with audio capture
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted, proceed with audio capture
                startRecording()
            } else {
                // Permission has been denied, show a message to the user or handle the error
                Toast.makeText(this, "Audio capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopRecording() {
        audioRecord!!.stop()
        audioRecord!!.release()
    }

    @get:Throws(InterruptedException::class)
    val audioData: ByteArray
        get() = audioDataQueue!!.take()

    private fun setupVisualizerFxAndUI() {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.
        mVisualizerView = VisualizerView(this)
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            (VISUALIZER_HEIGHT_DIP * resources.displayMetrics.density).toInt()
        )
        mLinearLayout!!.addView(mVisualizerView)
        val timer = Timer()
        val interval: Long = 15 // 1/20 second in milliseconds
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // This code will be executed every 1/20 second
                try {
                    mVisualizerView!!.updateVisualizer(audioData)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
        }, 0, interval)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && audioRecord != null) {
            stopRecording()
        }
    }

    companion object {
        private const val TAG = "AudioFxDemo"
        private const val VISUALIZER_HEIGHT_DIP = 100f
        private const val MY_PERMISSIONS_RECORD_AUDIO = 1
    }
}