package com.autosign.cpdaily.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object AdbUtil {
    private const val TAG = "s1nger_AdbUtil"

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received!")
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.i(TAG, "Shizuku binder dead!")
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            Log.w(TAG, "Registering Shizuku listeners: ${e.message}")
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isShizukuPermissionGranted(): Boolean {
        return try {
            if (isShizukuAvailable()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int = 1001) {
        try {
            if (isShizukuAvailable() && !isShizukuPermissionGranted()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "requestShizukuPermission error: ${e.message}")
        }
    }

    /**
     * 在非 Root 设备上通过 Shizuku/无线调试桥执行 ADB Shell 指令 (UID 2000)
     */
    fun execCommand(command: String): String {
        if (!isShizukuPermissionGranted()) {
            Log.w(TAG, "Shizuku 未授权或未运行，无法执行 ADB 命令: $command")
            return ""
        }
        val output = StringBuilder()
        var process: Process? = null
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
            if (process != null) {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                process.waitFor()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "AdbUtil.execCommand error: ${e.message}", e)
        } finally {
            try {
                process?.destroy()
            } catch (ignored: Exception) {}
        }
        return output.toString()
    }

    fun tap(x: Int, y: Int) {
        execCommand("input tap $x $y")
    }

    fun keyevent(keyCode: Int) {
        execCommand("input keyevent $keyCode")
    }

    fun lockScreen() {
        execCommand("input keyevent 26")
    }

    fun wakeScreen() {
        execCommand("input keyevent 224")
    }

    fun wakeAndUnlock(context: Context, pin: String) {
        wakeScreen()
        try { Thread.sleep(500) } catch (ignored: Exception) {}
        execCommand("input swipe 600 2000 600 600 250")
        try { Thread.sleep(500) } catch (ignored: Exception) {}
        if (pin.isNotBlank()) {
            execCommand("input text $pin")
            try { Thread.sleep(200) } catch (ignored: Exception) {}
            execCommand("input keyevent 66")
        }
    }

    fun forceStopCpdaily() {
        execCommand("am force-stop com.wisedu.cpdaily")
    }

    fun launchCpdaily(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.wisedu.cpdaily")
            val component = intent?.component?.flattenToString()
            if (!component.isNullOrBlank()) {
                execCommand("am start -n $component -a android.intent.action.MAIN -c android.intent.category.LAUNCHER")
                return
            }
        } catch (ignored: Exception) {}
        execCommand("am start -n com.wisedu.cpdaily/com.wisorg.wisedu.home.ui.HomeActivity")
    }

    fun enableAccessibilityService() {
        execCommand("settings put secure enabled_accessibility_services com.autosign.cpdaily/com.autosign.cpdaily.service.SignAccessibilityService && settings put secure accessibility_enabled 1")
    }
}
