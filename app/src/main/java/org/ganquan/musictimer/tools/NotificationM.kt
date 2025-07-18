package org.ganquan.musictimer.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.ganquan.musictimer.MainActivity

class NotificationM {
    companion object {

        fun getBuild (context: Context,
                   channelID: String,
                   title: String = "",
                   contentText: String = "",
                   contentIntent: PendingIntent? = null,
                   progress: Triple<Int,Int,Boolean>? = null,
                   actions: List<Triple<Int,String,PendingIntent>>? = null,
                   style: NotificationCompat.Style? = null
        ): NotificationCompat.Builder {
            val builder:NotificationCompat.Builder = NotificationCompat.Builder(context, channelID)
            if(title != "") builder.setContentTitle(title)
            if(contentText != "") builder.setContentText(contentText)
            if(progress != null) builder.setProgress(progress.first, progress.second, progress.third)
            if(style != null) builder.setStyle(style)

            if(contentIntent != null) {
                // 需要在 AndroidManifest.xml 的 ".MainActivity" 中添加 android:launchMode="singleTop"
                val contentIntent:PendingIntent = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(contentIntent)
            }

            var iconId = 0
            try {
                iconId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager
                        .getApplicationInfo(context.packageName, PackageManager.ApplicationInfoFlags.of(0)).icon
                } else {
                    context.packageManager
                        .getApplicationInfo(context.packageName, 0).icon
                }
                builder.setSmallIcon(iconId)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if(actions != null && actions.isNotEmpty()) {
                actions.forEach { it ->
                    builder.addAction(it.first, it.second, it.third)
                }
            }

            return builder
        }

        fun createChannel(context: Context,
                          channelID: String,
                          name: String = "",
                          importance: Int = NotificationManager.IMPORTANCE_LOW,
                          description: String = "") {

            val chan = NotificationChannel(
                channelID,
                if(name != "") name else context.packageName,
                importance
            )
            if(description != "") chan.description = description
            val mgr:NotificationManager = getManager(context)
            mgr.createNotificationChannel(chan)
        }

        fun getManager(context: Context): NotificationManager {
            return context.getSystemService(NotificationManager::class.java)
        }
    }
}