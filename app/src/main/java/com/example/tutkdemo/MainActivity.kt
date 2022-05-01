package com.example.tutkdemo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: VideoView
    private lateinit var player: ExoPlayer
    private lateinit var videoProvider: VideoProvider

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        playerView = findViewById(R.id.playerView)

        player = ExoPlayer.Builder(this).build()

        videoProvider = VideoProvider(
            getString(R.string.deviceUID),
            getString(R.string.licenseKey)
        )
        videoProvider.init()

        lifecycleScope.launch {
            videoProvider.audio.collect {
                Log.d("Audio", byteArrayToHexStr(it))
            }
        }
        lifecycleScope.launch {
            videoProvider.video.collect {
                Log.d("Video", byteArrayToHexStr(it))
            }
        }
    }

    fun prepareExoPlayerAudioFromByteArray(outputStream: ByteArrayOutputStream) {
        val byteArrayDataSource = ByteBufferDataSource(outputStream)
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val factory: DataSource.Factory = DataSource.Factory { byteArrayDataSource }
        val audioSource = ProgressiveMediaSource.Factory(factory, extractorsFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse("bytes:///" + "audio")))
        player.setMediaSource(audioSource)
        player.prepare()
        player.play()
    }

    fun byteArrayToHexStr(byteArray: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(byteArray.size * 2)
        for (j in byteArray.indices) {
            val v: Int = byteArray[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}