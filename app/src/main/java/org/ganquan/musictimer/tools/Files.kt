package org.ganquan.musictimer.tools

import android.content.Context
import android.os.Environment
import java.io.File

class Files {
    companion object {
        fun getMusicFile(path: String = ""): File {
            return getRootFile("${Environment.DIRECTORY_MUSIC}${File.separator}$path")
        }

        fun getDownFile(path: String = ""): File {
            return getRootFile("${Environment.DIRECTORY_DOWNLOADS}${File.separator}$path")
        }

        fun getPackageDownFile(context: Context, path: String = ""): File {
            return getPackageFile(context, "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$path")
        }

        fun getPackageFile(context: Context, path: String = ""): File {
            return File("${(context.getExternalFilesDir("") as File).path}${File.separator}$path")
        }

        fun getRootFile(folderName: String = ""): File {
            return File(
                Environment.getExternalStorageDirectory(),
                folderName
            )
        }

        fun getList(folderPath: String, isChildren: Boolean = false): List<File> {
            val list = mutableListOf<File>()
            val dir = File(folderPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    list.add(file)
                }

            } else {
                println("$folderPath 文件夹不存在")
            }
            list.sortBy { it.name }
            return list
        }

        fun createFolder(folder: File): String {
            if (folder.exists()) return folder.path
            try {
                if(folder.mkdirs()) return folder.path
                return ""
            } catch (e: Exception) {
                println(e)
                return ""
            }
        }
    }
}