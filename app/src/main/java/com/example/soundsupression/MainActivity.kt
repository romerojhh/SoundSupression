package com.example.soundsupression

import ai.picovoice.koala.*
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.soundsupression.ui.theme.SoundSupressionTheme
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "AudioFxDemo"
        const val VISUALIZER_HEIGHT_DIP = 100f
        const val MY_PERMISSIONS_RECORD_AUDIO = 1
        const val accessKey = "hf56TCW0QeQMiZMGEFhPVuWiXHjaLCfwtT6tKv3K+pmkRBg/fU3tzg=="
    }

    fun enhance() {
        val koala: Koala
        try {
             koala = Koala.Builder()
                .setAccessKey(accessKey)
                .build(this)
        } catch (ex: KoalaException) {
            throw RuntimeException(ex)
        }

        // koala.process()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        volumeControlStream = AudioManager.STREAM_MUSIC

        checkRecordAudioPermission()

        setContent {
            SoundSupressionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainLayout()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && audioRecord != null) {
            stopRecording()
        }
    }

    @get:Throws(InterruptedException::class)
    val audioData: ByteArray?
        get() = audioDataQueue?.take()

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

    fun stopRecording() {
        audioRecord!!.stop()
        audioRecord!!.release()
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted, proceed with audio capture
                startRecording()
            } else {
                // Permission has been denied, show a message to the user or handle the error
                Toast.makeText(this, "Audio capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun MainLayout() {
        val data by remember {
            mutableStateOf(audioData)
        }
        var border = Modifier.border(1.dp, Color.Red)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = border.fillMaxSize()
        ) {

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                factory = {
                    VisualizerView(it)
                },
                update = {
                    val timer = Timer()
                    val interval: Long = 15 // 1/20 second in milliseconds
                    timer.scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            // This code will be executed every 1/20 second
                            try {
                                it.updateVisualizer(audioData)
                            } catch (e: InterruptedException) {
                                throw RuntimeException(e)
                            }
                        }
                    }, 0, interval)
                }
            )

            Row(modifier = border
                .align(Alignment.Start)
                .padding(start = 5.dp)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { /*TODO Record*/ }
                ) {
                    Text(text = "Record")
                }

                Button(
                    onClick = { /*TODO Stop*/ }
                ) {
                    Text(text = "Stop")
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    SoundSupressionTheme {
//        var border = Modifier.border(1.dp, Color.Red)
//        MainLayout()
//    }
//}