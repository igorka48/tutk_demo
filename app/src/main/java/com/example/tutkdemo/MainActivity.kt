package com.example.tutkdemo

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: VideoView
    private lateinit var player: ExoPlayer
    private lateinit var AVProvider: AVProvider

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        playerView = findViewById(R.id.playerView)

        player = ExoPlayer.Builder(this).build()

        AVProvider = AVProvider(
            getString(R.string.deviceUID),
            getString(R.string.licenseKey)
        )
        AVProvider.init()


        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val client = AVProvider.audioSocketServer.accept()
                withContext(Dispatchers.Main) {
                    prepareExoPlayerAudioFromByteArray(client.inputStream)
                }
            }
        }


//        lifecycleScope.launch{
//            withContext(Dispatchers.IO){
//                val client = videoProvider.videoSocketServer.accept()
//                val buffer = ByteArray(VIDEO_BUF_SIZE)
//                while (true){
//                    client.inputStream.read(buffer, 0, buffer.size)
//                    Log.d("Video", byteArrayToHexStr(buffer))
//                }
//            }
//        }


    }

    fun prepareExoPlayerAudioFromByteArray(inputStream: InputStream) {
        val byteArrayDataSource = InputStreamDataSource(inputStream)
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val factory: DataSource.Factory = DataSource.Factory { byteArrayDataSource }
        val audioSource =
            ProgressiveMediaSource.Factory(factory, extractorsFactory).createMediaSource(
                MediaItem.fromUri(Uri.parse("bytes:///" + "audio"))
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