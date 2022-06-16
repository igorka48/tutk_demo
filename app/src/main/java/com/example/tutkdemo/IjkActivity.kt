package com.example.tutkdemo

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutkdemo.AVProvider.Companion.audioPort
import com.example.tutkdemo.AVProvider.Companion.localhost
import com.example.tutkdemo.AVProvider.Companion.videoPort
import com.example.tutkdemo.MainActivity.Companion.FULLSCREEN_KEY
import com.example.tutkdemo.databinding.ActivityIjkBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.danmaku.ijk.media.ijkplayerview.widget.media.IjkVideoView
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import kotlin.coroutines.coroutineContext


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
            Timber.d("Audio URI:" + uri)
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
            // handle arguments

            // init player
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer.native_profileBegin(IjkMediaPlayer.IJK_LIB_NAME_FFMPEG)
            //最大帧率 20
            videoPlayerView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW)
            //打开opense,h264下有效.
            //打开opense,h264下有效.
            // mVideoView.isAudioHardWare = true
//        mVideoView.setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
            //set the headers properties in user-agent.
            //        mVideoView.setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
            //set the headers properties in user-agent.
            //   mVideoView.userAgentStr = "Android_Station_V1.1.1"
            //设置h265
            //  if(mVideoPath.startsWith("rtsp")){
            //设置h265
            //  if(mVideoPath.startsWith("rtsp")){
            videoPlayerView.isH265 = true
            //  mVideoView.openZeroVideoDelay(true)
//        }else{
//            //打开视频0延迟.
//            mVideoView.openZeroVideoDelay(true);
//        }
            // prefer mVideoPath
            //        }else{
//            //打开视频0延迟.
//            mVideoView.openZeroVideoDelay(true);
//        }
            // prefer mVideoPath
            Timber.d("URI:$uri")
            videoPlayerView.setVideoURI(uri, IjkVideoView.IJK_TYPE_LIVING_LOW_DELAY)
            //mVideoView.set

            //准备就绪，做一些配置操作，比如音视频同步方式.

            //准备就绪，做一些配置操作，比如音视频同步方式.
            videoPlayerView.setOnPreparedListener(IMediaPlayer.OnPreparedListener {
                Timber.e("onPrepared#done! ")
                videoPlayerView.openZeroVideoDelay(true)
            })
            videoPlayerView.setOnInfoListener(IMediaPlayer.OnInfoListener { mp, what, extra ->
                Timber.e("onInfo#position: " + mp.currentPosition + " what: " + what + " extra: " + extra)
//            if (IjkMediaPlayer.MP_STATE_PREPARED == what) {
//                val takeTime: Long = SystemClock.currentThreadTimeMillis() - mLastStartTime
//                Log.i("poe", "加载视频prepare耗时#=====================> $takeTime ms")
//                // DO: 2020/3/31 真正的准备完成了，准备播放 ，回调到外面通知状态改变！。
//            }
                false
            })
        }
    }

    override fun onStop() {
        Timber.d("ActivityLifecycle onStop")
        avProvider.stopVideo()
        players.forEach {
            it.stopPlayback()
            it.release(true)
            it.stopBackgroundPlay()
        }
        IjkMediaPlayer.native_profileEnd()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("ActivityLifecycle onResume")
        avProvider.startVideo()
        lifecycleScope.launch {
            delay(300)
            playVideo(Uri.parse("tcp://$localhost:$videoPort"))
            playAudio(Uri.parse("tcp://$localhost:$audioPort"))
        }
    }
}