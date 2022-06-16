package com.example.tutkdemo

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutkdemo.MainActivity.Companion.FULLSCREEN_KEY
import com.example.tutkdemo.databinding.ActivityIjkBinding
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.danmaku.ijk.media.ijkplayerview.widget.media.IjkVideoView
import tv.danmaku.ijk.media.player.IjkMediaPlayer


class IjkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIjkBinding
    private lateinit var players: List<IjkVideoView>
    private var isInFullscreen = false
    private lateinit var avProvider: AVProvider

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        isInFullscreen = savedInstanceState?.getBoolean(FULLSCREEN_KEY) ?: false
        avProvider = (applicationContext as TUTKDemoApplication).avProvider

        binding = ActivityIjkBinding.inflate(layoutInflater)
        setContentView(binding.root)


        with(binding) {
            players = listOf(audioPlayerView, videoPlayerView)
            modeButton.setOnClickListener { changeMode() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("fullscreen", isInFullscreen)
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


    private fun playAudio(uri: Uri) {
        with(binding) {
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer.native_profileBegin(IjkMediaPlayer.IJK_LIB_NAME_FFMPEG)
            Timber.d("Audio URI:$uri")
            audioPlayerView.setVideoURI(uri, IjkVideoView.IJK_TYPE_LIVING_LOW_DELAY)
            audioPlayerView.setOnPreparedListener {
                Timber.e("onPrepared#done! ")
                audioPlayerView.openZeroVideoDelay(true)
            }
            audioPlayerView.setOnInfoListener { mp, what, extra ->
                Timber.e("onInfo#position: " + mp.currentPosition + " what: " + what + " extra: " + extra)
                false
            }
        }
    }

    private fun playVideo(uri: Uri) {
        with(binding) {
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer.native_profileBegin(IjkMediaPlayer.IJK_LIB_NAME_FFMPEG)
            videoPlayerView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW)
            videoPlayerView.isH265 = true
            Timber.d("URI:$uri")
            videoPlayerView.setVideoURI(uri, IjkVideoView.IJK_TYPE_LIVING_LOW_DELAY)
            videoPlayerView.setOnPreparedListener {
                Timber.e("onPrepared#done! ")
                videoPlayerView.openZeroVideoDelay(true)
            }
            videoPlayerView.setOnInfoListener { mp, what, extra ->
                Timber.e("onInfo#position: " + mp.currentPosition + " what: " + what + " extra: " + extra)
                false
            }
        }
    }

    override fun onStop() {
        Timber.d("ActivityLifecycle onStop")
        avProvider.stopVideo()
        try {
            players.forEach {
                it.stopPlayback()
                it.release(true)
                it.stopBackgroundPlay()
            }
            IjkMediaPlayer.native_profileEnd()
        } catch(e: Exception){
            Timber.e(e)
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("ActivityLifecycle onResume")
        lifecycleScope.launch {
            val result = avProvider.startVideo()
            withContext(Main) {
                playVideo(result.videoUri)
                playAudio(result.audioUri)
            }
        }
    }
}