package org.ganquan.musictimer.tools

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class Permission(val context: Context, val code: Int) {
    private var permissionInfo: PermissionInfo

    init {
        this.permissionInfo = getInfo()
    }

    @SuppressLint("BatteryLife")
    fun check(): Boolean {
        var types = mutableListOf<String>()
        val type = permissionInfo.type
        if(type != "") types.add(type)

        when (code) {
            PermissionCode.READ_MEDIA_AUDIO.data -> {
                types.add(READ_EXTERNAL_STORAGE)
            }

            PermissionCode.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.data -> {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val flag = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                if(!flag) openSetting(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

                return flag
            }
        }

        val results = types.map{ ContextCompat.checkSelfPermission(context, it) }
                        .filter { it == PackageManager.PERMISSION_GRANTED }
        if (results.isNotEmpty()) return true
        ActivityCompat.requestPermissions(context as Activity, types.toTypedArray(), code)
        return false
    }

    fun result(
        permissions: Array<String?>,
        grantResults: IntArray,
        isAlertDialog: Boolean = true
    ): Boolean {
        if (grantResults.isEmpty()) return false
        if (permissions.size == 1
            && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            return true
        } else if (permissions.size > 1
            && (grantResults.copyOfRange(0, 2).contains(PackageManager.PERMISSION_GRANTED))) {
            return true
        } else if(isAlertDialog){
            AlertDialog.Builder(context)
                .setMessage(permissionInfo.toastMsg)
                .setPositiveButton("去设置") { _, _ ->
                    openSetting()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        return false
    }

    fun openSetting(action: String = ACTION_APPLICATION_DETAILS_SETTINGS) {
        try {
            val intent = Intent(action)
            intent.setData(("package:${context.packageName}").toUri())
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getInfo(): PermissionInfo {
        return when (code) {
            PermissionCode.READ_MEDIA_AUDIO.data ->
                PermissionInfo(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) READ_MEDIA_AUDIO else "",
                    "请开启本地数据和音乐权限")

            PermissionCode.WRITE_EXTERNAL_STORAGE.data ->
                PermissionInfo(
                    WRITE_EXTERNAL_STORAGE,
                    "请开启本地数据读写权限")

            PermissionCode.FOREGROUND_SERVICE.data ->
                PermissionInfo(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) FOREGROUND_SERVICE else "",
                    "请开启后台活动权限")

            PermissionCode.ACCESS_BACKGROUND_LOCATION.data ->
                PermissionInfo(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ACCESS_BACKGROUND_LOCATION else "",
                    "请开启允许后台活动权限")

            PermissionCode.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.data ->
                PermissionInfo(
                    REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    "请开启允许后台活动权限")

            else -> PermissionInfo()
        }
    }
}

data class PermissionInfo(val type: String="", val toastMsg: String="")
enum class PermissionCode(val data: Int) {
    READ_MEDIA_AUDIO(1),
    WRITE_EXTERNAL_STORAGE(2),
    FOREGROUND_SERVICE(3),
    ACCESS_BACKGROUND_LOCATION(4),
    REQUEST_IGNORE_BATTERY_OPTIMIZATIONS(5)
}