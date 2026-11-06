package com.autosign.cpdaily.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.autosign.cpdaily.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AutoSign_Scheduler"
    private const val REQUEST_CODE = 1001

    fun scheduleDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val hour = PrefsUtil.getSignHour(context)
        val minute = PrefsUtil.getSignMinute(context)

        val mode = PrefsUtil.getWorkMode(context)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            if (mode.isSprint) {
                // 极速冲榜模式：依据【签到开始时间】提前 20 秒 (例如 06:29:40) 唤醒并硬件解锁预热
                val startHour = PrefsUtil.getSignStartHour(context)
                val startMinute = PrefsUtil.getSignStartMinute(context)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.SECOND, -20)
            } else {
                // 普通模式：依据设定的定时打卡时间准点唤醒
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 如果今天的时间已经过去，则安排到明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.autosign.cpdaily.ACTION_TRIGGER_SIGN"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "已成功设定定时打卡闹钟: 目标时间为 ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "设定定时打卡失败: ${e.message}", e)
        }
    }

    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.autosign.cpdaily.ACTION_TRIGGER_SIGN"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "已取消定时打卡闹钟")
    }
}
