package com.autosign.cpdaily.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.autosign.cpdaily.model.WorkMode
import com.autosign.cpdaily.service.SignAccessibilityService
import com.autosign.cpdaily.util.AlarmScheduler
import com.autosign.cpdaily.util.PrefsUtil
import com.autosign.cpdaily.util.RootUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "s1nger_AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "⏰ 定时打卡闹钟触发！开始唤醒与解锁流程...")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AutoSign:WakeLockTag"
        )
        wakeLock.acquire(10 * 60 * 1000L /* 10 分钟安全超时 */)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mode = PrefsUtil.getWorkMode(context)
                val pin = PrefsUtil.getPin(context)

                if (mode.isSprint) {
                    // 🚀 极速冲榜模式：执行毫秒级抢榜专用流水线
                    val sprintOk = com.autosign.cpdaily.util.SprintSignEngine.executeSprint(context, isSimulation = false)
                    if (!sprintOk) {
                        Log.w(TAG, "极速冲榜未完全成功，切换至普通模式进行保底打卡...")
                        RootUtil.launchCpdaily(context)
                        Thread.sleep(1000)
                        SignAccessibilityService.startTask()
                    }
                    // 安排明天的定时
                    AlarmScheduler.scheduleDailyAlarm(context)
                    return@launch
                }

                if (mode.isRoot) {
                    // 1. Root 硬件点亮屏幕并快速解锁
                    RootUtil.wakeAndUnlock(context, pin)
                    Thread.sleep(1000)

                    // 2. 启动今日校园
                    RootUtil.launchCpdaily(context)
                } else {
                    // 免 Root 智能无障碍 / SKZ 模式
                    val service = SignAccessibilityService.instance
                    if (service != null) {
                        com.autosign.cpdaily.util.NonRootUtil.wakeAndUnlockWithAccessibility(context, service, pin)
                    } else {
                        RootUtil.wakeAndUnlock(context, pin)
                    }
                    Thread.sleep(1000)
                    RootUtil.launchCpdaily(context)
                }

                Thread.sleep(500)

                // 3. 标记无障碍服务进入打卡时序流水线
                SignAccessibilityService.startTask()

                // 4. 安排明天的打卡定时
                AlarmScheduler.scheduleDailyAlarm(context)
            } catch (e: Exception) {
                Log.e(TAG, "执行定时打卡唤醒异常: ${e.message}", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
}
