package org.ganquan.musictimer.tools

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject

//测试数据
//var d1 = mapOf("id" to 1, "name" to "张三")
//var d2 = mapOf("id" to 2, "name" to "李四")
//{"root": [{"id":1, "name":"张三"}, {"id":2, "name":"李四"}] }
//val d3 = mapOf("root" to listOf<Any>(d1, d2))
//[[{"id":1, "name":"张三"}, {"id":2, "name":"李四"}]]
//var d4 = listOf(listOf(d1, d2))

class Json {
    companion object {

        fun toJson(params: Any?): String {
            return when(params) {
                is Map<*,*> -> {
                    val obj = JSONObject()
                    for ((k, v) in params) {
                        obj.put(k.toString(), toJson(v))
                    }
                    obj
                }
                is Array<*>, is List<*> -> {
                    val array = JSONArray()
                    (params as List<*>).forEach { array.put(toJson(it)) }
                    JSONObject().put(params::class.simpleName.toString(), array)
                }
                is Int -> JSONObject().put("Int", params)
                is Long -> JSONObject().put("Long", params)
                is Char -> JSONObject().put("Char", params)
                is String -> JSONObject().put("String", params)
                null -> JSONObject().put("Null", "")
                else -> ""
            }.toString()
        }

        fun parseJson(jsonStr: String): Any? {
            if(jsonStr == "") return ""

            val obj = JSONObject(jsonStr.replace("SingletonList", "ArrayList"))
            val map = mutableMapOf<String, Any?>()
            var value:Any? = null
            obj.keys().asSequence().forEach { key ->
                value = when(key) {
                    "ArrayList","Array" -> {
                        val jsonArray = obj.getJSONArray(key)
                        val list = if(key=="Array") arrayListOf<Any?>() else mutableListOf<Any?>()
                        for (i in 0 until jsonArray.length()) {
                            list.add(parseJson(jsonArray[i].toString()))
                        }
                        list
                    }
                    "Int" -> obj.getInt(key)
                    "Long" -> obj.getLong(key)
                    "String" -> obj.getString(key)
                    "Char" -> obj.getString(key)[0]
                    "Null" -> null
                    else -> {
                        map.put(key, parseJson(obj.getString(key)))
                        null
                    }
                }
            }
            return if(map.isNotEmpty()) map else value
        }

        fun toGJson(params: Any): String {
            return Gson().toJson(params)
        }

        fun parseGJson(jsonStr: String ): Any {
            val obj = when(jsonStr[0]) {
                '{' -> object : TypeToken<Map<*, *>>() {}.type
                '[' -> object : TypeToken<Map<*,*>>() {}.type
                else -> object : TypeToken<String>() {}.type
            }
            return Gson().fromJson(jsonStr, obj)
        }
    }
}