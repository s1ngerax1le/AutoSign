package com.autosign.cpdaily.util

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.autosign.cpdaily.service.SignAccessibilityService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 自动打卡高清录屏引擎 (ScreenRecorder)
 * 利用 Root 权限调用底层 /system/bin/screenrecord
 * 自动记录打卡全过程，结束后生成标准化 MP4 视频，方便诊断排查所有界面异常
 */
object ScreenRecorder {
    private const val TAG = "ScreenRecorder"
    private const val MAX_SAVED_VIDEOS = 10
    private val RECORD_DIR = File(Environment.getExternalStorageDirectory(), "Movies/AutoSign")
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private var currentRecordingFile: File? = null

    @Volatile
    var isRecording: Boolean = false
        private set

    /**
     * 启动后台高清录屏 (720x1560, 4Mbps, 限制最长 3 分钟防死锁)
     */
    @Synchronized
    fun startRecording(context: Context): Boolean {
        if (isRecording) {
            Log.w(TAG, "Screen recording is already running.")
            return true
        }

        try {
            if (!RECORD_DIR.exists()) {
                RECORD_DIR.mkdirs()
            }
            cleanOldRecordings()

            val fileName = "sign_${fileDateFormat.format(Date())}.mp4"
            val targetFile = File(RECORD_DIR, fileName)
            currentRecordingFile = targetFile

            SignAccessibilityService.log("🎥 [自动录屏] 启动后台高清录制 -> Movies/AutoSign/$fileName")

            // 使用 nohup 与后台 & 执行系统 screenrecord
            // 720x1560 兼顾极佳清晰度与低 CPU/内存占用；--time-limit 180 作为保底熔断
            val cmd = "nohup screenrecord --size 720x1560 --bit-rate 4000000 --time-limit 180 ${targetFile.absolutePath} >/dev/null 2>&1 &"
            RootUtil.execCommand(cmd, context)

            isRecording = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screenrecord: ${e.message}", e)
            SignAccessibilityService.log("⚠️ 自动录屏启动失败: ${e.message}")
            isRecording = false
            return false
        }
    }

    /**
     * 停止后台录屏并优雅封装 MP4 容器
     */
    @Synchronized
    fun stopRecording(context: Context): File? {
        if (!isRecording && currentRecordingFile == null) return null

        try {
            SignAccessibilityService.log("⏹️ [自动录屏] 正在停止录屏并封装 MP4 视频...")
            // 向 screenrecord 进程发送 SIGINT (信号 2)，通知编码器完成 MP4 文件头与索引写入
            RootUtil.execCommand("pkill -2 screenrecord || killall -2 screenrecord", context)

            // 给底层编码器写入磁盘预留 1000ms
            Thread.sleep(1000)

            val recordedFile = currentRecordingFile
            if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 1024L) {
                // 刷新系统媒体库，让相册与视频播放器立即索引
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(recordedFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
                SignAccessibilityService.log("🎬 [录屏已保存] 文件大小: ${recordedFile.length() / 1024} KB (${recordedFile.name})")
                return recordedFile
            } else {
                SignAccessibilityService.log("⚠️ 录屏文件过小或未成功生成")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop screenrecord: ${e.message}", e)
        } finally {
            isRecording = false
        }
        return currentRecordingFile
    }

    /**
     * 获取最新的一份录屏文件
     */
    fun getLatestRecording(): File? {
        if (!RECORD_DIR.exists()) return null
        return RECORD_DIR.listFiles { file -> file.isFile && file.name.endsWith(".mp4") }
            ?.maxByOrNull { it.lastModified() }
    }

    /**
     * 获取所有录屏列表（按修改时间倒序排列）
     */
    fun getAllRecordings(): List<File> {
        if (!RECORD_DIR.exists()) return emptyList()
        return RECORD_DIR.listFiles { file -> file.isFile && file.name.endsWith(".mp4") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 调用系统播放器直接播放最新打卡录屏
     */
    fun openLatestRecording(context: Context) {
        val latest = getLatestRecording()
        if (latest == null || !latest.exists() || latest.length() == 0L) {
            Toast.makeText(context, "暂无录屏文件，打卡任务启动后将自动生成", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                latest
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open video: ${e.message}", e)
            Toast.makeText(context, "无法启动播放器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 调用系统分享面板分享录屏文件
     */
    fun shareLatestRecording(context: Context) {
        val latest = getLatestRecording()
        if (latest == null || !latest.exists() || latest.length() == 0L) {
            Toast.makeText(context, "暂无录屏文件", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                latest
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "分享打卡录屏视频").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot share video: ${e.message}", e)
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 自动清理多余的历史录屏，仅保留最新的 10 份
     */
    private fun cleanOldRecordings() {
        try {
            val files = RECORD_DIR.listFiles { file -> file.isFile && file.name.endsWith(".mp4") }
                ?.sortedByDescending { it.lastModified() } ?: return

            if (files.size >= MAX_SAVED_VIDEOS) {
                for (i in MAX_SAVED_VIDEOS - 1 until files.size) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "cleanOldRecordings error: ${e.message}")
        }
    }
}
