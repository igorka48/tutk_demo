package com.example.tutkdemo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.*
import com.example.tutkdemo.AVProvider.Companion.audioPort
import com.example.tutkdemo.AVProvider.Companion.outputPort
import com.example.tutkdemo.AVProvider.Companion.videoPort
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream


class ExoActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private lateinit var avProvider: AVProvider

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ijk)

        FFmpegKitConfig.enableLogCallback {
            //Log.d("FFmpegKitLog",  it.message)
        }
        FFmpegKitConfig.enableStatisticsCallback {
           // Log.d("FFmpegKit", "Stats: $it")
        }

        playerView = findViewById(R.id.playerView)
        player = ExoPlayer.Builder(this).build()
        player.addAnalyticsListener( PlaybackStatsListener(false, null))
        playerView.player = player


        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                FFmpegKit.executeAsync("-i tcp://127.0.0.1:${videoPort} -i tcp://127.0.0.1:${audioPort} -c:v copy -c:a aac -f mp4 -movflags frag_keyframe+empty_moov tcp://127.0.0.1:${outputPort}"
                ) { session ->
                    when {
                        ReturnCode.isSuccess(session.returnCode) -> {
                            Log.d("FFmpegKit", "SUCCESS")
                            // SUCCESS
                        }
                        ReturnCode.isCancel(session.returnCode) -> {
                            Log.d("FFmpegKit", "CANCEL")
                            // CANCEL
                        }
                        else -> {
                            // FAILURE
                            Log.d(
                                "FFmpegKit",
                                String.format(
                                    "Command failed with state %s and rc %s.%s",
                                    session.state,
                                    session.returnCode,
                                    session.failStackTrace
                                )
                            )
                        }
                    }
                }
            }
        }



//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                val client = avProvider.audioSocketServer.accept()
//                withContext(Dispatchers.Main) {
//                    prepareExoPlayerFromInputStream(client.inputStream)
//                }
//            }
//        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val client = avProvider.outSocketServer.accept()
                withContext(Dispatchers.Main) {
                    prepareExoPlayerFromInputStream(client.inputStream)
                }
            }
        }


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