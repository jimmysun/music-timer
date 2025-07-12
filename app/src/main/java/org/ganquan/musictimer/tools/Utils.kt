package org.ganquan.musictimer.tools

import android.content.Context
import androidx.core.content.edit

class Utils {
    companion object {
        fun int2String(i: Int, len: Int = 2): String {
            var s: String = i.toString()
            if(s.length >= len) return s
            return s.padStart(len, '0')
        }

        fun sharedPrefer(context: Context, key: String = "", value: Any? = null): Any? {
            val sharedPref = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            if(key == "") return sharedPref.edit { clear() }
            return if(value == null) Json.parseJson(sharedPref.getString(key, "").toString())
            else sharedPref.edit(commit = true) {
                putString(key, Json.toJson(value))
            }
        }
    }
}