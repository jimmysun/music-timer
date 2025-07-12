package org.ganquan.musictimer.tools

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoroutineScopeM {
    companion object {
        val customScope = CoroutineScope(
            SupervisorJob() + // 防止子协程异常传播
                    Dispatchers.IO +   // 指定默认调度器
                    CoroutineExceptionHandler { _, e ->
                        Log.e("MyScope", "协程异常: $e")
                    }
        )

        fun start(function: () -> Any?, callback: (Any?) -> Unit) {
            startMulti(listOf(function), callback)
        }

        fun startMulti(functions: List<() -> Any?>, callback: (Any?) -> Unit) {
            customScope.launch {
                val result = functions.map {
                    async { it() }.await()
                }
                withContext(Dispatchers.Main) {
                    callback(if (functions.size > 1) result else result[0])
                }
            }
        }

        fun cleanup() {
            customScope.cancel()
        }
    }
}