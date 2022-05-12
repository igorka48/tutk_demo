package com.example.tutkdemo

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.ijkplayerview.widget.IjkPrettyVideoView
import tv.danmaku.ijk.media.ijkplayerview.widget.media.IjkVideoView
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.ByteArrayOutputStream
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var avProvider: AVProvider
    private lateinit var mVideoView: IjkVideoView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        mVideoView = findViewById(R.id.playerView)

        avProvider = AVProvider(
            getString(R.string.deviceUID),
            getString(R.string.licenseKey)
        )

       avProvider.initAV()




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
                //val client = avProvider.outSocketServer.accept()
                withContext(Dispatchers.Main) {
                   // prepareExoPlayerFromInputStream(client.inputStream)
                    playIjk(Uri.parse("tcp://127.0.0.1:6667"))
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


    fun playIjk(uri: Uri){
        // handle arguments

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin(IjkMediaPlayer.IJK_LIB_NAME_FFMPEG)
        //最大帧率 20
        mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW)
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
        mVideoView.isH265 = true
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
        Log.d("URI", "URI:$uri")
        mVideoView.setVideoURI(uri, IjkVideoView.IJK_TYPE_HTTP_PLAY)
        //mVideoView.set

        //准备就绪，做一些配置操作，比如音视频同步方式.

        //准备就绪，做一些配置操作，比如音视频同步方式.
        mVideoView.setOnPreparedListener(IMediaPlayer.OnPreparedListener {
            Log.e("TAG", "onPrepared#done! ")
            mVideoView.openZeroVideoDelay(true)
        })
        mVideoView.setOnInfoListener(IMediaPlayer.OnInfoListener { mp, what, extra ->
            Log.e(
                "TAG",
                "onInfo#position: " + mp.currentPosition + " what: " + what + " extra: " + extra
            )
//            if (IjkMediaPlayer.MP_STATE_PREPARED == what) {
//                val takeTime: Long = SystemClock.currentThreadTimeMillis() - mLastStartTime
//                Log.i("poe", "加载视频prepare耗时#=====================> $takeTime ms")
//                // DO: 2020/3/31 真正的准备完成了，准备播放 ，回调到外面通知状态改变！。
//            }
            false
        })
    }
}