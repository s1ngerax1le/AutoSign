package com.autosign.cpdaily.util

import android.os.Build
import android.util.Log
import com.autosign.cpdaily.service.SignAccessibilityService
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootUtil {
    private const val TAG = "AutoSign_Root"

    /**
     * 检查设备是否具备 Root 权限并已授权
     */
    fun isRootAvailable(): Boolean {
        return execCommand("id").contains("uid=0")
    }

    /**
     * 利用 Root 权限静默激活无障碍服务（解决每次更新覆盖 APK 后无障碍被系统重置断开的问题）
     */
    fun enableAccessibilityService() {
        Log.d(TAG, "Enabling accessibility service via root...")
        execCommand("settings put secure enabled_accessibility_services com.autosign.cpdaily/com.autosign.cpdaily.service.SignAccessibilityService")
        execCommand("settings put secure accessibility_enabled 1")
    }

    /**
     * 执行 Shell 命令 (Root 模式走 su)
     */
    fun execCommand(command: String, context: android.content.Context? = null): String {
        return execSuCommand(command)
    }

    fun execCommandWithOutput(command: String): String {
        return execSuCommand(command)
    }

    /**
     * 极速直达：通过 Root 权限直接调起今日校园内置 WebView 容器打开目标 URL
     */
    fun launchBrowsePage(url: String) {
        val escapedUrl = url.replace("'", "'\\''")
        execCommand("am start --activity-clear-top -n com.wisedu.cpdaily/com.wisorg.wisedu.common.BrowsePageActivity --es 'url' '$escapedUrl'")
    }

    /**
     * 将本应用 s1ngerAutoSign 切回前台
     */
    fun launchAutoSign(context: android.content.Context? = null) {
        execCommand("am start -n com.autosign.cpdaily/.MainActivity")
    }

    private fun execSuCommand(command: String): String {
        return try {
            // 使用 su -mm 跨越 Android 应用独立挂载空间，直达目标数据目录
            val process = ProcessBuilder("su", "-mm", "-c", command)
                .redirectErrorStream(true)
                .start()
            val text = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(6, java.util.concurrent.TimeUnit.SECONDS)
            } else {
                process.waitFor()
            }
            if (text.contains("invalid option") || text.contains("unrecognized option")) {
                // 若不支持 -mm 参数，降级至标准 su -c
                val fallbackProcess = ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start()
                val fallbackText = fallbackProcess.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    fallbackProcess.waitFor(6, java.util.concurrent.TimeUnit.SECONDS)
                } else {
                    fallbackProcess.waitFor()
                }
                fallbackText
            } else {
                text
            }
        } catch (e: Exception) {
            Log.e(TAG, "execSuCommand error: ${e.message}")
            ""
        }
    }

    private var cachedWidth = 0
    private var cachedHeight = 0

    /**
     * 获取设备物理屏幕真实分辨率
     */
    fun getScreenResolution(context: android.content.Context? = null): Pair<Int, Int> {
        if (cachedWidth > 0 && cachedHeight > 0) {
            return Pair(cachedWidth, cachedHeight)
        }
        try {
            val out = execCommand("wm size")
            val match = Regex("(?i)(?:Physical|Override) size:\\s*(\\d+)x(\\d+)").findAll(out).lastOrNull()
            if (match != null) {
                cachedWidth = match.groupValues[1].toInt()
                cachedHeight = match.groupValues[2].toInt()
                return Pair(cachedWidth, cachedHeight)
            }
        } catch (e: Exception) {
            Log.w(TAG, "wm size error: ${e.message}")
        }
        if (context != null) {
            val dm = context.resources.displayMetrics
            cachedWidth = dm.widthPixels
            cachedHeight = dm.heightPixels
            return Pair(cachedWidth, cachedHeight)
        }
        return Pair(1280, 2772)
    }

    /**
     * 自动点亮屏幕并通过精准物理触控网格输入 PIN 码解锁 (完美突破 Android 14 / PixelOS / MIUI 锁屏隔离安全机制)
     */
    fun wakeAndUnlock(context: android.content.Context?, pin: String) {
        SignAccessibilityService.log("💡 [解锁步骤 1/3] 硬件级点亮屏幕...")
        // 1. 硬件级强制点亮屏幕 (SCREEN_BRIGHT_WAKE_LOCK)
        try {
            context?.let { ctx ->
                val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                val brightLock = pm.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or android.os.PowerManager.ON_AFTER_RELEASE,
                    "s1nger:BrightWakeLock"
                )
                brightLock.acquire(10000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquire warning: ${e.message}")
        }

        // 2. 发送物理唤醒与解散锁屏指令
        execCommand("cmd power wakeup; input keyevent 224; wm dismiss-keyguard", context)
        Thread.sleep(300)

        // 3. 执行平滑上滑手势 (从屏幕 75% 滑至 15%，完全呼出 PIN 键盘)
        val (width, height) = getScreenResolution(context)
        val startX = width / 2
        val startY = (height * 0.75).toInt()
        val endY = (height * 0.15).toInt()

        SignAccessibilityService.log("👆 [解锁步骤 2/3] 上滑呼出 PIN 键盘...")
        execCommand("input swipe $startX $startY $startX $endY 200", context)
        Thread.sleep(600)

        // 4. 输入 PIN 码
        if (pin.isNotBlank()) {
            SignAccessibilityService.log("🔢 [解锁步骤 3/3] 物理触控网格秒级击发 PIN 码 (${pin.length}位)...")

            // 基于真机 SystemUI 实测标定的标准 PIN 键盘相对屏幕比例 (精准适配全面屏)
            // 列: 1/4/7/del (24.4%), 2/5/8/0 (50.0%), 3/6/9/enter (75.6%)
            // 行: 1/2/3 (45.8%), 4/5/6 (56.8%), 7/8/9 (67.7%), del/0/enter (78.6%)
            val col1X = (width * 0.244).toInt()
            val col2X = (width * 0.500).toInt()
            val col3X = (width * 0.756).toInt()

            val row1Y = (height * 0.458).toInt()
            val row2Y = (height * 0.568).toInt()
            val row3Y = (height * 0.677).toInt()
            val row4Y = (height * 0.786).toInt()

            fun getCoords(char: Char): Pair<Int, Int>? {
                return when (char) {
                    '1' -> Pair(col1X, row1Y)
                    '2' -> Pair(col2X, row1Y)
                    '3' -> Pair(col3X, row1Y)
                    '4' -> Pair(col1X, row2Y)
                    '5' -> Pair(col2X, row2Y)
                    '6' -> Pair(col3X, row2Y)
                    '7' -> Pair(col1X, row3Y)
                    '8' -> Pair(col2X, row3Y)
                    '9' -> Pair(col3X, row3Y)
                    '0' -> Pair(col2X, row4Y)
                    else -> null
                }
            }

            val cmdBuilder = StringBuilder()
            // 依次击发各数字键 (100ms 间隔确保触控驱动稳定接收)
            for (c in pin) {
                val coords = getCoords(c)
                if (coords != null) {
                    cmdBuilder.append("input tap ${coords.first} ${coords.second}; sleep 0.1; ")
                }
            }
            // 提交：点击确认 Enter 键 + 发送 KEYCODE_ENTER 双通道确保立刻提交解锁
            cmdBuilder.append("input tap $col3X $row4Y; sleep 0.05; input keyevent 66")

            execCommand(cmdBuilder.toString(), context)
            Thread.sleep(600)
        } else {
            // 免密模式：解散锁屏与发送 MENU 键
            SignAccessibilityService.log("🔓 [解锁步骤 3/3] 免密模式：解散锁屏与发送 MENU 键 (未配置 PIN)")
            execCommand("input keyevent 82; wm dismiss-keyguard", context)
            Thread.sleep(300)
        }
    }

    /**
     * 启动今日校园 App (多通道保障拉起)
     */
    fun launchCpdaily(context: android.content.Context? = null) {
        Log.d(TAG, "Launching 今日校园...")
        // 通道 1: 通过 resolved ComponentName 执行 am start
        try {
            context?.let { ctx ->
                val intent = ctx.packageManager.getLaunchIntentForPackage("com.wisedu.cpdaily")
                val component = intent?.component?.flattenToString()
                if (!component.isNullOrBlank()) {
                    execCommand("am start -n $component -a android.intent.action.MAIN -c android.intent.category.LAUNCHER")
                }
                // 在应用上下文也派发一次
                intent?.apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }?.let { ctx.startActivity(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "PackageManager launch warning: ${e.message}")
        }

        // 通道 2: monkey 通用包名拉起兜底
        execCommand("monkey -p com.wisedu.cpdaily -c android.intent.category.LAUNCHER 1")
    }

    /**
     * 强制停止今日校园 App
     */
    fun forceStopCpdaily() {
        execCommand("am force-stop com.wisedu.cpdaily")
    }

    /**
     * 模拟点击指定坐标 (Root 兜底)
     */
    fun tap(x: Int, y: Int) {
        execCommand("input tap $x $y")
    }

    /**
     * 模拟相机拍照快门 (KEYCODE_CAMERA = 27)
     */
    fun triggerCameraShutter() {
        execCommand("input keyevent 27")
    }

    /**
     * 截取屏幕并保存到本地
     */
    fun takeScreenshot(savePath: String) {
        execCommand("mkdir -p /sdcard/AutoSign/")
        execCommand("screencap -p $savePath")
    }

    /**
     * 自动息屏 (KEYCODE_POWER = 26)
     */
    fun lockScreen() {
        execCommand("input keyevent 26")
    }
}
