package com.autosign.cpdaily.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autosign.cpdaily.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "s1nger_alerts"
    private const val CHANNEL_NAME = "s1nger 打卡状态提醒"
    private const val NOTIFY_ID_FAILURE = 2001
    private const val NOTIFY_ID_SUCCESS = 2002

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "展示 s1nger 自动化打卡执行成功或失败结果通知"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * 发送签到打卡失败提醒通知 (提示用户导出日志发给开发者)
     */
    fun sendSignFailureNotification(context: Context, reason: String = "未检测到打卡成功凭据") {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_tab", "test") // 打开时自动定位至【测试/日志】页
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ s1nger 签到打卡可能未成功")
            .setContentText("$reason，请点击打开应用导出日志发给开发者排查")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "⚠️ 今日校园打卡异常提醒：\n$reason。\n\n请点击本通知打开 s1nger，在【测试】页面点击【📋 导出日志】发送给开发者进行诊断与适配。"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFY_ID_FAILURE, notification)
    }

    /**
     * 发送签到打卡成功提醒通知
     */
    fun sendSignSuccessNotification(context: Context, message: String = "已成功完成今日打卡") {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("🎉 s1nger 今日打卡成功")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFY_ID_SUCCESS, notification)
    }
}
