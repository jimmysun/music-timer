package org.ganquan.musictimer.tools

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.floor

class Time {
    companion object {
        fun get(): ZonedDateTime {
            val zoneId = ZoneId.of("Asia/Shanghai")
            val now = ZonedDateTime.now(zoneId)
            return now
        }

        fun isPass(hour: Int, munit: Int): Boolean {
            val now = get()
            val startMinuteCount = hour * 60 + munit
            val nowMinuteCount = now.hour * 60 + now.minute

            return nowMinuteCount > startMinuteCount
        }

        fun toHourMinuteSecond(millisecond: Long): Triple<Int,Int,Int> {
            val second1 = floor((millisecond / 1000).toDouble())
            val second = (second1 % 60).toInt()
            val minute = ((second1-second) / 60 % 60).toInt()
            val hour = floor(((second1-minute-second) / 60 / 60).toDouble()).toInt()
            return Triple(hour, minute, second)
        }
    }
}