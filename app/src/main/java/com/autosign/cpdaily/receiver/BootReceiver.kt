package com.autosign.cpdaily.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autosign.cpdaily.util.AlarmScheduler
import com.autosign.cpdaily.util.PrefsUtil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("AutoSign_Boot", "系统已重启，正在检查并重新注册每日签到定时器...")
            if (PrefsUtil.isAlarmEnabled(context)) {
                AlarmScheduler.scheduleDailyAlarm(context)
            }
        }
    }
}
