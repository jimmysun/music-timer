package org.ganquan.musictimer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.net.toUri
import androidx.media.app.NotificationCompat
import org.ganquan.musictimer.tools.Broadcast
import org.ganquan.musictimer.tools.Files
import org.ganquan.musictimer.tools.NotificationM
import java.io.File
import kotlin.math.floor

// 通知通道与通知 ID
private const val CHANNEL_ID = "music_play_channel"
private const val NOTIFICATION_ID  = 1001
// 通知动作字符串，用于区分点击事件
private const val ACTION_PLAY  = "ACTION_PLAY"
private const val ACTION_PAUSE = "ACTION_PAUSE"
private const val ACTION_STOP  = "ACTION_STOP"

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var isPlaying = false
    private var currentMusicName = ""

    override fun onCreate() {
        super.onCreate()
        NotificationM.createChannel(this, CHANNEL_ID, description = "音乐播放通知")
        initMediaPlayer()
    }

    /**
     * 在 startService/startForegroundService 后回调
     */
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent:Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            if(intent.action != null) {
                // 区分点击通知的 Action
                when (intent.action) {
                    ACTION_PLAY -> resume()
                    ACTION_PAUSE -> pause()
                    ACTION_STOP -> {
                        stopSelf() // 停止 Service
                        isPlaying = false
                        Broadcast.sendLocal(this, "end worker")
                        return START_NOT_STICKY
                    }
                }
            } else {
                // 首次启动，直接开始播放
                val folderPath: String = intent.getStringExtra("folder_path").toString()
                val musicName: String = intent.getStringExtra("music_name").toString()
                val list = Files.getList(folderPath)
                start(list, musicName)
                pause()
//                Broadcast.receiveLocal (this) { msg -> broadcastReceiveHandler(msg)}
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        // 如果被系统杀掉，不再自动重启
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Broadcast.destroyLocal(this) { msg, _ -> broadcastReceiveHandler(msg)}
        mediaPlayer.pause()
        mediaPlayer.stop()
        mediaPlayer.release()
        isPlaying = false
        // 取消前台状态与通知
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * 初始化 MediaPlayer，并设置音频属性
     */
    private fun initMediaPlayer() {
        mediaPlayer.isLooping = true
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
    }

    /** 开始播放并更新状态 */
    private fun start(fileList: List<File>, name: String = "") {
        try {
            var file: File? = fileList.find { it.name == name }
            if (file == null) file = fileList[floor(Math.random() * fileList.size).toInt()]
            currentMusicName = file.nameWithoutExtension
            Broadcast.sendLocal(this, "new music", currentMusicName)
            play(file.path)
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                start(fileList, name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    private fun play(path: String) {
        mediaPlayer = MediaPlayer.create(this, path.toUri())
        mediaPlayer.start()
        isPlaying = true
    }

    /** 暂停播放并更新状态 */
    private fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    /** 恢复播放并更新状态 */
    fun resume() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    private fun broadcastReceiveHandler(msg: String) {
        when (msg) {
            "start worker" -> resume()
//            "end worker" -> stopSelf()
        }
    }

    /** 构建前台服务通知，包含播放/暂停/停止按钮 */
    private fun buildNotification(): Notification {
        val playPauseIntent:Intent = Intent(this, MusicService::class.java)
            .setAction(if(isPlaying) ACTION_PAUSE else ACTION_PLAY)
        val ppPending: PendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent:Intent = Intent(this, MusicService::class.java)
            .setAction(ACTION_STOP)
        val stopPending:PendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationM.getBuild(this,
            CHANNEL_ID,
            currentMusicName,
            actions = listOf(Triple(
                if(isPlaying) R.drawable.button_play else R.drawable.button_pause,
                if(isPlaying) "暂停" else "播放",
                ppPending
            ), Triple(
                R.drawable.button_stop,
                "停止",
                stopPending
            )),
            style = NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1)
        ).build()
    }
}