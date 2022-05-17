package com.example.tutkdemo

import android.util.Log
import com.tutk.IOTC.*
import kotlinx.coroutines.*
import java.net.*


class AVProvider {

    private val defaultErrorHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("", "${this.javaClass.name} got $exception")
    }
    private val defaultScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO) + defaultErrorHandler
    private val videoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) + defaultErrorHandler
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) + defaultErrorHandler


    var localhostAddr = InetAddress.getByAddress(localhost)
    var audioAddr: SocketAddress = InetSocketAddress(localhostAddr, audioPort)
    var videoAddr: SocketAddress = InetSocketAddress(localhostAddr, videoPort)


    private val audioSocketServer = ServerSocket(audioPort)
    private val videoSocketServer = ServerSocket(videoPort)
    val outSocketServer = ServerSocket(outputPort)

    private var videoSocket: Socket? = null
    private var audioSocket: Socket? = null

    fun initAV(uid: String, licenceKay: String) = defaultScope.launch {
        println("StreamClient start...")

        var ret = TUTKGlobalAPIs.TUTK_SDK_Set_License_Key(licenceKay)
        System.out.printf("TUTK_SDK_Set_License_Key() ret = %d\n", ret)
        if (ret != TUTKGlobalAPIs.TUTK_ER_NoERROR) {
            System.out.printf("TUTK_SDK_Set_License_Key exit...!!\n")
            return@launch
        }

        ret = IOTCAPIs.IOTC_Initialize2(0)
        System.out.printf("IOTC_Initialize2() ret = %d\n", ret)
        if (ret != IOTCAPIs.IOTC_ER_NoERROR) {
            System.out.printf("IOTCAPIs_Device exit...!!\n")
            return@launch
        }

        // alloc 3 sessions for video and two-way audio

        // alloc 3 sessions for video and two-way audio
        AVAPIs.avInitialize(3)

        val sid = IOTCAPIs.IOTC_Get_SessionID()
        if (sid < 0) {
            System.out.printf("IOTC_Get_SessionID error code [%d]\n", sid)
            return@launch
        }
        ret = IOTCAPIs.IOTC_Connect_ByUID_Parallel(uid, sid)
        System.out.printf("Step 2: call IOTC_Connect_ByUID_Parallel(%s).......\n", uid)

        var srvType = 0
        var bResend = 0
        var avIndex = 0
        val av_client_in_config = St_AVClientStartInConfig()
        val av_client_out_config = St_AVClientStartOutConfig()

        av_client_in_config.iotc_session_id = sid
        av_client_in_config.iotc_channel_id = 0
        av_client_in_config.timeout_sec = 20
        av_client_in_config.account_or_identity = "admin"
        av_client_in_config.password_or_token = "888888"
        av_client_in_config.resend = 1
        av_client_in_config.security_mode = 0 //enable DTLS

        av_client_in_config.auth_type = 0

        avIndex = AVAPIs.avClientStartEx(av_client_in_config, av_client_out_config)
        bResend = av_client_out_config.resend
        srvType = av_client_out_config.server_type
        System.out.printf("Step 2: call avClientStartEx(%d).......\n", avIndex)

        if (avIndex < 0) {
            System.out.printf("avClientStartEx failed[%d]\n", avIndex)
            return@launch
        }

        if (startIpcamStream(avIndex)) {
            Log.d("Tag", "index + $avIndex")
            val deferred = async {
                readAudio(avIndex)
            }
            val deferred2 = async {
                readVideo(avIndex)
            }
            awaitAll(deferred, deferred2)
        }

        AVAPIs.avClientStop(avIndex)
        System.out.printf("avClientStop OK\n")
        IOTCAPIs.IOTC_Session_Close(sid)
        System.out.printf("IOTC_Session_Close OK\n")
        AVAPIs.avDeInitialize()
        IOTCAPIs.IOTC_DeInitialize()
        System.out.printf("StreamClient exit...\n")
    }

    private fun startIpcamStream(avIndex: Int): Boolean {
        // This IOTYPE constant and its corrsponsing data structure is defined in
        // Sample/Linux/Sample_AVAPIs/AVIOCTRLDEFs.h
        //
        val av = AVAPIs()
        val IOTYPE_USER_IPCAM_START = 0x1FF
        var ret = AVAPIs.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START, ByteArray(8), 8)
        if (ret < 0) {
            System.out.printf("start_ipcam_stream failed[%d]\n", ret)
            return false
        }
        val IOTYPE_USER_IPCAM_AUDIOSTART = 0x300
        ret = AVAPIs.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTART, ByteArray(8), 8)
        if (ret < 0) {
            System.out.printf("start_ipcam_stream failed[%d]\n", ret)
            return false
        }
        return true
    }

    private suspend fun readAudio(avIndex: Int) {
        audioSocket = audioSocketServer.accept()
        System.out.printf(
            "[%s] Start\n",
            Thread.currentThread().name
        )

        val av = AVAPIs()
        val frameInfo = ByteArray(FRAME_INFO_SIZE)
        val audioBuffer = ByteArray(AUDIO_BUF_SIZE)
        while (true) {
            var ret = AVAPIs.avCheckAudioBuf(avIndex)
            if (ret < 0) {
                // Same error codes as below
                System.out.printf(
                    "[%s] avCheckAudioBuf() failed: %d\n",
                    Thread.currentThread().name, ret
                )
                break
            } else if (ret < 3) {
                try {
                    Thread.sleep(120)
                    continue
                } catch (e: InterruptedException) {
                    println(e.message)
                    break
                }
            }
            val frameNumber = IntArray(1)
            ret = AVAPIs.avRecvAudioData(
                avIndex, audioBuffer,
                AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
                frameNumber
            )
            if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                System.out.printf(
                    "[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                    Thread.currentThread().name
                )
                break
            } else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                System.out.printf(
                    "[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
                    Thread.currentThread().name
                )
                break
            } else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                System.out.printf(
                    "[%s] Session cant be used anymore\n",
                    Thread.currentThread().name
                )
                break
            } else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                //System.out.printf("[%s] Audio frame losed\n",
                //        Thread.currentThread().getName());
                continue
            }

            audioSocket?.outputStream?.write(audioBuffer, 0, ret)
            // Now the data is ready in audioBuffer[0 ... ret - 1]
            // Do something here
        }

        System.out.printf(
            "[%s] Exit\n",
            Thread.currentThread().name
        )
    }

    private suspend fun readVideo(avIndex: Int) {
        System.out.printf(
            "[%s] Start\n",
            Thread.currentThread().name
        )
        videoSocket = videoSocketServer.accept()
        val av = AVAPIs()
        val frameInfo = ByteArray(FRAME_INFO_SIZE)
        val videoBuffer = ByteArray(VIDEO_BUF_SIZE)
        val outBufSize = IntArray(1)
        val outFrameSize = IntArray(1)
        val outFrmInfoBufSize = IntArray(1)
        while (true) {
            val frameNumber = IntArray(1)
            val ret = AVAPIs.avRecvFrameData2(
                avIndex, videoBuffer,
                VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                frameInfo, FRAME_INFO_SIZE,
                outFrmInfoBufSize, frameNumber
            )
            if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
                try {
                    Thread.sleep(30)
                    continue
                } catch (e: InterruptedException) {
                    println(e.message)
                    break
                }
            } else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                System.out.printf(
                    "[%s] Lost video frame number[%d]\n",
                    Thread.currentThread().name, frameNumber[0]
                )
                continue
            } else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                System.out.printf(
                    "[%s] Incomplete video frame number[%d]\n",
                    Thread.currentThread().name, frameNumber[0]
                )
                continue
            } else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                System.out.printf(
                    "[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                    Thread.currentThread().name
                )
                break
            } else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                System.out.printf(
                    "[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
                    Thread.currentThread().name
                )
                break
            } else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                System.out.printf(
                    "[%s] Session cant be used anymore\n",
                    Thread.currentThread().name
                )
                break
            }
            // Now the data is ready in videoBuffer[0 ... ret - 1]
            // Do something here
            videoSocket?.outputStream?.write(videoBuffer, 0, ret)
           // udpVideoSocket.send(DatagramPacket(videoBuffer, 0, ret, videoAddr))

        }

        System.out.printf(
            "[%s] Exit\n",
            Thread.currentThread().name
        )
    }


    companion object {
        const val audioPort = 6666
        const val videoPort = 6667
        const val outputPort = 6668
        const val AUDIO_BUF_SIZE = 1024
        const val FRAME_INFO_SIZE = 16
        const val VIDEO_BUF_SIZE = 100000
        val localhost = byteArrayOf(127, 0, 0, 1)
    }


}