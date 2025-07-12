package org.ganquan.musictimer.tools

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

private const val CHANNEL_ID: String = "apk_down"
private const val SHARED_PREFER_KEY: String = "updateVersion"

class Apk {
    private var builder: NotificationCompat.Builder
    private var context: Context
    private var lastUpdateNoticeTime = 0L

    constructor(context1: Context) {
        context = context1
        initChannel()
        builder = getBuilder()
    }

    @SuppressLint("DefaultLocale")
    fun down(url:String,
             newVersion: String = "",
             appName: String = "",
             callback: (String) -> Unit
    ) {
        var apkUrl = "$url/raw/master/app/release/app-release.apk"
        var apkName = appName
        if(appName == "") apkName = context.packageName.split(".").last()
        if(newVersion != "") apkName += "_${newVersion.replace(Regex("\\."), "_")}.apk"
        val apkFile = Files.getPackageFile(context, apkName)
        if (apkFile.exists()) callback(apkFile.path)
        else {
            Http.down(apkUrl, apkFile.path, callback) { progressList ->
                val curBytes = progressList[0]
                val totalBytes = progressList[1]
                if (curBytes == totalBytes && totalBytes != 0L) {
                    updateNotification("下载完成", listOf(0, 0))
                } else {
                    var progress = curBytes * 100 / totalBytes
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateNoticeTime > 250) {
                        lastUpdateNoticeTime = currentTime
                        updateNotification("正在下载: ${String.format("%.2f",curBytes.toDouble() / 1024 /1024)} M", listOf(100, progress.toInt()))
                    }
                }
            }
            builder.setContentTitle("版本更新 $newVersion")
            updateNotification("开始下载", listOf(100,0))
        }
    }

    /**
     * 需要添加 <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
     * 需要配置 AndroidManifest.xml 的 provider
     * 需要添加 file_paths.xml
     */
    fun install(apkFile: File): Boolean {
        updateNotification("下载完成", listOf(0, 0))
        val intent = Intent(Intent.ACTION_VIEW)
        val apkUri = FileProvider.getUriForFile(context, context.packageName + ".provider", apkFile)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)

//            val resultPendingIntent = TaskStackBuilder.create(context)
//                    .addNextIntentWithParentStack(intent)
//                    .getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                    )
//            builder.setContentIntent(resultPendingIntent)
//            builder.setAutoCancel(true)
//            builder.setContentText("点击开始安装")
//            NotificationM.getManager(context).notify(1, builder.build())

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getBuilder(text: String = ""): NotificationCompat.Builder {
        return NotificationM.getBuild(context, CHANNEL_ID, "版本更新", text)    }

    private fun initChannel() {
        NotificationM.createChannel(
            context,
            CHANNEL_ID,
            "更新应用",
            NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    private fun updateNotification(text:String, progressList: List<Int>) {
        builder.setContentText(text)
        if( progressList[1] > 0) builder.setProgress(progressList[0], progressList[1], false)
        NotificationM.getManager(context).notify(1, builder.build())
    }

    companion object {
        fun checkVersion(context: Context, url: String, callback: (String) -> Unit = {}) {
            var jsonUrl = "$url/raw/master/app/release/output-metadata.json"
            var currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

            CoroutineScopeM.start({
                Http.get(jsonUrl)
            },{ outputJson ->
                val newVersion = (((outputJson as Map<*, *>)["elements"] as ArrayList<*>)[0] as Map<*,*>)["versionName"].toString()
                val ignoreVersion = Utils.sharedPrefer(context, SHARED_PREFER_KEY)
                if(newVersion == ignoreVersion || newVersion == currentVersion) callback("")
                else callback(newVersion)
            })
        }

        fun alertDialog(context: Context, version: String = "", style: Int, callback: () -> Unit = {}) {
            var currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            MaterialAlertDialogBuilder(context, style)
                .setTitle("版本更新 $version")
                .setMessage("当前版本：$currentVersion")
                .setPositiveButton("立即更新"){ dialog, which -> callback() }
                .setNegativeButton("暂不更新"){ dialog, which ->  }
                .setNeutralButton("忽略此版本"){ dialog, which ->
                    Utils.sharedPrefer(context, SHARED_PREFER_KEY, version)
                }.show()
        }
    }
}

