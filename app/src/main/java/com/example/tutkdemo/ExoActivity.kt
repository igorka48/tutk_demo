package com.example.tutkdemo

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.example.tutkdemo.AVProvider.Companion.audioPort
import com.example.tutkdemo.AVProvider.Companion.outputPort
import com.example.tutkdemo.AVProvider.Companion.videoPort
import com.example.tutkdemo.databinding.ActivityExoBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream


class ExoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExoBinding
    private var player: ExoPlayer? = null
    private lateinit var avProvider: AVProvider
    private var isInFullscreen = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isInFullscreen = savedInstanceState?.getBoolean(MainActivity.FULLSCREEN_KEY) ?: false

        binding = ActivityExoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        avProvider = (applicationContext as TUTKDemoApplication).avProvider
        FFmpegKitConfig.enableLogCallback {
            //Log.d("FFmpegKitLog",  it.message)
        }
        FFmpegKitConfig.enableStatisticsCallback {
            // Log.d("FFmpegKit", "Stats: $it")
        }
        player = ExoPlayer.Builder(this).build()
        player?.addAnalyticsListener(PlaybackStatsListener(false, null))

        with(binding) {
            modeButton.setOnClickListener { changeMode() }
            playerView.player = player
        }


//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                FFmpegKit.executeAsync("-i tcp://127.0.0.1:${videoPort} -i tcp://127.0.0.1:${audioPort} -c:v copy -c:a aac -f mp4 -movflags frag_keyframe+empty_moov tcp://127.0.0.1:${outputPort}"
//                ) { session ->
//                    when {
//                        ReturnCode.isSuccess(session.returnCode) -> {
//                            Log.d("FFmpegKit", "SUCCESS")
//                            // SUCCESS
//                        }
//                        ReturnCode.isCancel(session.returnCode) -> {
//                            Log.d("FFmpegKit", "CANCEL")
//                            // CANCEL
//                        }
//                        else -> {
//                            // FAILURE
//                            Log.d(
//                                "FFmpegKit",
//                                String.format(
//                                    "Command failed with state %s and rc %s.%s",
//                                    session.state,
//                                    session.returnCode,
//                                    session.failStackTrace
//                                )
//                            )
//                        }
//                    }
//                }
//            }
//        }


//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                val client = avProvider.audioSocketServer.accept()
//                withContext(Dispatchers.Main) {
//                    prepareExoPlayerFromInputStream(client.inputStream)
//                }
//            }
//        }

//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                val client = avProvider.outSocketServer.accept()
//                withContext(Dispatchers.Main) {
//                    prepareExoPlayerFromInputStream(client.inputStream)
//                }
//
//            }
//        }


//        lifecycleScope.launch{
//            withContext(Dispatchers.IO){
//                val client = avProvider.outSocketServer.accept()
//                val buffer = ByteArray(VIDEO_BUF_SIZE)
//                while (true){
//                    client.inputStream.read(buffer, 0, buffer.size)
//                    Log.d("Video", byteArrayToHexStr(buffer))
//                }
//            }
//        }


    }

    private fun prepareExoPlayerFromInputStream(inputStream: InputStream) {
        val byteArrayDataSource = InputStreamDataSource(inputStream)
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val factory: DataSource.Factory = DataSource.Factory { byteArrayDataSource }
        val audioSource =
            ProgressiveMediaSource.Factory(factory, extractorsFactory).createMediaSource(
                MediaItem.fromUri(Uri.parse("bytes:///" + "video"))
            )
        player?.setMediaSource(audioSource)
        player?.prepare()
        player?.play()
    }


    private fun changeMode() {
        if (isInFullscreen) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        isInFullscreen = !isInFullscreen
    }

    override fun onDestroy() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        releasePlayer()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
        player?.stop()
        player?.clearMediaItems()
        avProvider.stopVideo()
    }

    override fun onResume() {
        super.onResume()
        Log.d("Tag", "VideoPlayer. onResume")
        player?.playWhenReady = true
        lifecycleScope.launch {
            avProvider.startVideo()
        }

    }

    private fun releasePlayer() {
        Log.d("Tag", "VideoPlayer. releasePlayer")
        player?.playWhenReady = false
        player?.release()
        player = null
    }

}