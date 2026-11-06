package com.autosign.cpdaily.util

import android.content.Context
import android.os.Build
import android.os.Environment
import com.autosign.cpdaily.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale

object LogRepository {
    private const val MAX_LOG_SIZE = 200
    private val logQueue = LinkedList<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // 外部监听器（用于更新 UI 终端）
    var uiListener: ((String) -> Unit)? = null

    @Synchronized
    fun addLog(msg: String) {
        val timeStr = timeFormat.format(Date())
        val entry = "[$timeStr] $msg"
        
        if (logQueue.size >= MAX_LOG_SIZE) {
            logQueue.removeFirst()
        }
        logQueue.add(entry)

        // 通知 UI 回调
        uiListener?.invoke(entry)
    }

    @Synchronized
    fun getAllLogs(): List<String> {
        return ArrayList(logQueue)
    }

    /**
     * 生成完整的诊断诊断报告（供用户一键导出发送给开发者）
     */
    @Synchronized
    fun generateDiagnosticReport(context: Context): String {
        val sb = StringBuilder()
        sb.append("================ s1ngerAutoSign 诊断日志报告 ================\n")
        sb.append("生成时间: ${fullDateFormat.format(Date())}\n\n")

        sb.append("【设备信息】\n")
        sb.append("手机品牌: ${Build.MANUFACTURER}\n")
        sb.append("手机型号: ${Build.MODEL} (${Build.PRODUCT})\n")
        sb.append("系统架构: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")
        sb.append("Android 版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("系统构建版本: ${Build.DISPLAY}\n\n")

        sb.append("【应用状态】\n")
        sb.append("应用名称: s1ngerAutoSign\n")
        sb.append("应用版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("工作模式: ${PrefsUtil.getWorkMode(context).title}\n")
        sb.append("定时时间: 每日 ${String.format("%02d:%02d", PrefsUtil.getSignHour(context), PrefsUtil.getSignMinute(context))} (已开启: ${PrefsUtil.isAlarmEnabled(context)})\n")
        sb.append("无障碍服务就绪: ${com.autosign.cpdaily.service.SignAccessibilityService.isServiceConnected}\n")
        sb.append("失败重试上限: ${PrefsUtil.getMaxRetries(context)} 次\n")
        val latestVideo = ScreenRecorder.getLatestRecording()
        if (latestVideo != null && latestVideo.exists()) {
            sb.append("诊断录屏文件: ${latestVideo.name} (${latestVideo.length() / 1024} KB)\n")
        }
        sb.append("\n")

        sb.append("【最近运行日志 (共 ${logQueue.size} / $MAX_LOG_SIZE 条)】\n")
        if (logQueue.isEmpty()) {
            sb.append("(暂无运行日志)\n")
        } else {
            for (line in logQueue) {
                sb.append(line).append("\n")
            }
        }
        sb.append("============================================================\n")
        return sb.toString()
    }

    /**
     * 导出诊断报告为独立 TXT 文件，防止内容过长超出剪贴板上限
     */
    @Synchronized
    fun exportLogToTxtFile(context: Context): File {
        val report = generateDiagnosticReport(context)
        val filename = "s1ngerAutoSign_log_${fileTimestampFormat.format(Date())}.txt"

        // 1. 保存在应用外部私有日志目录（用于 FileProvider 安全分享）
        val logDir = File(context.getExternalFilesDir(null), "logs").apply {
            if (!exists()) mkdirs()
        }
        val file = File(logDir, filename)
        file.writeText(report, Charsets.UTF_8)

        // 2. 同时在系统公共 Download 目录存一份备用副本，方便用户在文件管理器直接找
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() || downloadDir.mkdirs()) {
                val publicFile = File(downloadDir, filename)
                publicFile.writeText(report, Charsets.UTF_8)
            }
        } catch (ignored: Exception) {}

        return file
    }
}
