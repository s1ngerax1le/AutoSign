package com.autosign.cpdaily.util

import android.content.Context
import android.util.Log
import com.autosign.cpdaily.service.SignAccessibilityService
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * 今日校园个推推送雷达嗅探器
 * 监控 /data/data/com.wisedu.cpdaily/databases/pushsdk.db
 * 毫秒级提取 06:30:00 服务端下发给今日校园的专属签到 linkUrl (含 signWid 与 signInstanceWid)
 */
object PushSniffer {
    private const val TAG = "PushSniffer"
    const val DEFAULT_BASE_SIGN_URL = "https://fdm.lzjtu.edu.cn/wec-counselor-sign-apps/stu/mobile/index.html"

    /**
     * 单次极速提取最新推送中的签到 URL
     * @param minTimestampMs 最早有效时间戳。正式冲榜时设为当天发车前 60 秒，防止误取往期旧链接；演练模式下为 0L 允许提取最新可用链接。
     * @param isSimulation 是否为演练/测试模式
     */
    fun extractLatestSignUrl(
        context: Context? = null,
        minTimestampMs: Long = 0L,
        isSimulation: Boolean = false
    ): String? {
        val sqliteBin = if (java.io.File("/system/bin/sqlite3").exists()) "/system/bin/sqlite3" else "sqlite3"

        // 通道 1: 直查 pushsdk.db (个推长连接消息库，写入最早，比应用层 cache 快 700ms 左右)
        try {
            val pushCmd = "echo \"SELECT createtime, msgextra FROM message ORDER BY id DESC LIMIT 3;\" | $sqliteBin /data/data/com.wisedu.cpdaily/databases/pushsdk.db"
            val result = RootUtil.execCommandWithOutput(pushCmd)
            Log.d(TAG, "pushsdk query output length: ${result.length}, raw: [${result.trim()}]")
            val url = parsePushsdkOutput(result, minTimestampMs, isSimulation)
            if (!url.isNullOrBlank()) {
                context?.let { PrefsUtil.saveLastSignUrl(it, url) }
                return url
            }
        } catch (e: Exception) {
            Log.w(TAG, "直查 pushsdk.db 异常: ${e.message}")
        }

        // 通道 2: 直查 cache 库 (今日校园消息缓存库，key = 'app_message')
        try {
            val cacheCmd = "echo \"SELECT time, value FROM cache WHERE key = 'app_message' ORDER BY _id DESC LIMIT 3;\" | $sqliteBin /data/data/com.wisedu.cpdaily/databases/cache"
            val result = RootUtil.execCommandWithOutput(cacheCmd)
            Log.d(TAG, "cache query output length: ${result.length}, raw: [${result.trim()}]")
            val url = parseCacheOutput(result, minTimestampMs, isSimulation)
            if (!url.isNullOrBlank()) {
                context?.let { PrefsUtil.saveLastSignUrl(it, url) }
                return url
            }
        } catch (e: Exception) {
            Log.w(TAG, "直查 cache 库异常: ${e.message}")
        }

        // 通道 3: 若为演练模式且数据库暂时没读到，兜底使用上次持久化成功的链接
        if (isSimulation && context != null) {
            val cachedUrl = PrefsUtil.getLastSignUrl(context)
            if (!cachedUrl.isNullOrBlank()) {
                Log.d(TAG, "演练模式使用持久化缓存链接: $cachedUrl")
                SignAccessibilityService.log("⚡ [演练模式] 使用持久化历史有效 URL 直达: $cachedUrl")
                return cachedUrl
            }
        }

        return null
    }

    private val SIGN_URL_REGEX = Regex("""https?://[^\s"'\\}]+(?:sign|counselor)[^\s"'\\}]+""")

    private fun parsePushsdkOutput(output: String?, minTimestampMs: Long, isSimulation: Boolean): String? {
        if (output.isNullOrBlank()) return null
        val lines = output.trim().split("\n")
        for (line in lines) {
            val parts = line.trim().split("|", limit = 2)
            if (parts.size < 2) continue

            val createTime = parts[0].toLongOrNull() ?: 0L
            if (!isSimulation && minTimestampMs > 0L && createTime > 0L) {
                if (createTime < minTimestampMs) {
                    continue // 历史旧推送，跳过
                }
            }

            val payload = parts[1].trim()
            val match = SIGN_URL_REGEX.find(payload)
            if (match != null) {
                val url = match.value.trimEnd('"', '\'', '}', '\\', ';')
                Log.d(TAG, "🎯 [PushSDK] 正则秒级提取签到 URL: $url (时间戳: $createTime)")
                SignAccessibilityService.log("🎯 [推送雷达] 毫秒级捕获签到 URL: $url")
                return url
            }
        }
        return null
    }

    private fun parseCacheOutput(output: String?, minTimestampMs: Long, isSimulation: Boolean): String? {
        if (output.isNullOrBlank()) return null
        val lines = output.trim().split("\n")
        for (line in lines) {
            val parts = line.trim().split("|", limit = 2)
            if (parts.size < 2) continue

            val time = parts[0].toLongOrNull() ?: 0L
            if (!isSimulation && minTimestampMs > 0L && time > 0L) {
                if (time < minTimestampMs) {
                    continue
                }
            }

            val payload = parts[1].trim()
            val match = SIGN_URL_REGEX.find(payload)
            if (match != null) {
                val url = match.value.trimEnd('"', '\'', '}', '\\', ';')
                Log.d(TAG, "🎯 [Cache] 正则秒级提取签到 URL: $url (时间戳: $time)")
                SignAccessibilityService.log("🎯 [消息缓存] 毫秒级捕获签到 URL: $url")
                return url
            }
        }
        return null
    }

    /**
     * 高频毫秒级轮询监听器
     */
    suspend fun pollSignUrl(
        context: Context? = null,
        timeoutMs: Long = 12000L,
        intervalMs: Long = 60L,
        minTimestampMs: Long = 0L,
        isSimulation: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        SignAccessibilityService.log("📡 [推送雷达] 启动毫秒级嗅探 (轮询间隔 ${intervalMs}ms, 超时 ${timeoutMs}ms)...")

        while (isActive && System.currentTimeMillis() - startTime < timeoutMs) {
            val url = extractLatestSignUrl(context, minTimestampMs, isSimulation)
            if (!url.isNullOrBlank()) {
                val elapsed = System.currentTimeMillis() - startTime
                SignAccessibilityService.log("⚡ [推送雷达命中] 耗时 ${elapsed}ms 捕获签到直达链接！")
                return@withContext url
            }
            delay(intervalMs)
        }
        Log.w(TAG, "推送雷达嗅探超时 (${timeoutMs}ms)")
        null
    }
}
