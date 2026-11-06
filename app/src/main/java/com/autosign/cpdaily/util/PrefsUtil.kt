package com.autosign.cpdaily.util

import android.content.Context
import android.content.SharedPreferences

object PrefsUtil {
    private const val PREF_NAME = "autosign_prefs"
    private const val KEY_PIN = "key_pin"
    private const val KEY_SIGN_HOUR = "key_sign_hour"
    private const val KEY_SIGN_MINUTE = "key_sign_minute"
    private const val KEY_ALARM_ENABLED = "key_alarm_enabled"
    private const val KEY_TASK_RUNNING = "key_task_running"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(context: Context): String {
        return getPrefs(context).getString(KEY_PIN, "") ?: ""
    }

    fun saveSignTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_SIGN_HOUR, hour)
            .putInt(KEY_SIGN_MINUTE, minute)
            .apply()
    }

    fun getSignHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_SIGN_HOUR, 6) // 默认 6点
    }

    fun getSignMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_SIGN_MINUTE, 31) // 默认 31分
    }

    // 签到官方开放开始时间 (极速冲榜瞄准目标，默认 06:30)
    private const val KEY_SIGN_START_HOUR = "key_sign_start_hour"
    private const val KEY_SIGN_START_MINUTE = "key_sign_start_minute"

    fun saveSignStartTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_SIGN_START_HOUR, hour)
            .putInt(KEY_SIGN_START_MINUTE, minute)
            .apply()
    }

    fun getSignStartHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_SIGN_START_HOUR, 6)
    }

    fun getSignStartMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_SIGN_START_MINUTE, 30)
    }

    // 语言配置与首次启动标记 (zh / en / system)
    private const val KEY_APP_LANGUAGE = "key_app_language"
    private const val KEY_LANGUAGE_CHOSEN = "key_language_chosen"

    fun saveAppLanguage(context: Context, lang: String) {
        getPrefs(context).edit().putString(KEY_APP_LANGUAGE, lang).apply()
    }

    fun getAppLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_APP_LANGUAGE, "zh") ?: "zh"
    }

    fun isLanguageChosen(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LANGUAGE_CHOSEN, false)
    }

    fun setLanguageChosen(context: Context, chosen: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_LANGUAGE_CHOSEN, chosen).apply()
    }

    fun setAlarmEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALARM_ENABLED, enabled).apply()
    }

    fun isAlarmEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALARM_ENABLED, true)
    }

    private const val KEY_RECORD_SCREEN_ENABLED = "key_record_screen_enabled"

    fun isRecordScreenEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RECORD_SCREEN_ENABLED, true) // 默认开启自动录屏
    }

    fun setRecordScreenEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_RECORD_SCREEN_ENABLED, enabled).apply()
    }

    private const val KEY_WORK_MODE = "key_work_mode"

    fun saveWorkMode(context: Context, mode: com.autosign.cpdaily.model.WorkMode) {
        getPrefs(context).edit().putInt(KEY_WORK_MODE, mode.id).apply()
    }

    fun getWorkMode(context: Context): com.autosign.cpdaily.model.WorkMode {
        val id = getPrefs(context).getInt(KEY_WORK_MODE, com.autosign.cpdaily.model.WorkMode.NO_PWD_ACCESSIBILITY.id)
        return com.autosign.cpdaily.model.WorkMode.fromId(id)
    }

    private const val KEY_CUSTOM_BG_URI = "key_custom_bg_uri"

    fun saveCustomBgUri(context: Context, uriString: String?) {
        getPrefs(context).edit().putString(KEY_CUSTOM_BG_URI, uriString).apply()
    }

    fun getCustomBgUri(context: Context): String? {
        return getPrefs(context).getString(KEY_CUSTOM_BG_URI, null)
    }

    private const val KEY_SIGN_COUNT = "key_sign_count"
    private const val KEY_LAST_SIGN_DATE = "key_last_sign_date"

    fun getSignCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_SIGN_COUNT, 0)
    }

    fun recordSignSuccess(context: Context) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = getPrefs(context).getString(KEY_LAST_SIGN_DATE, "")
        if (today != lastDate) {
            val count = getSignCount(context) + 1
            getPrefs(context).edit()
                .putInt(KEY_SIGN_COUNT, count)
                .putString(KEY_LAST_SIGN_DATE, today)
                .apply()
        }
    }

    fun setTaskRunning(context: Context, running: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TASK_RUNNING, running).apply()
    }

    fun isTaskRunning(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TASK_RUNNING, false)
    }

    private const val KEY_MAX_RETRIES = "key_max_retries"

    fun getMaxRetries(context: Context): Int {
        return getPrefs(context).getInt(KEY_MAX_RETRIES, 3) // 默认 3 次
    }

    fun saveMaxRetries(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_MAX_RETRIES, count).apply()
    }

    private const val KEY_AGREEMENT_ACCEPTED = "key_agreement_accepted"

    fun isAgreementAccepted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AGREEMENT_ACCEPTED, false)
    }

    fun setAgreementAccepted(context: Context, accepted: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AGREEMENT_ACCEPTED, accepted).apply()
    }

    // ==========================================
    // 预览版限制：7 天有效期与 5 次签到上限
    // ==========================================
    private const val KEY_FIRST_USE_TIME = "key_first_use_time"
    const val PREVIEW_MAX_DAYS = 7
    const val PREVIEW_MAX_SIGN_DAYS = 5

    fun getFirstUseTimestamp(context: Context): Long {
        var timestamp = getPrefs(context).getLong(KEY_FIRST_USE_TIME, 0L)
        if (timestamp == 0L) {
            timestamp = System.currentTimeMillis()
            getPrefs(context).edit().putLong(KEY_FIRST_USE_TIME, timestamp).apply()
        }
        return timestamp
    }

    fun isPreviewExpired(context: Context): Boolean {
        // 正式版完全解除有效期与签到次数限制
        return false
    }

    fun getPreviewExpireReason(context: Context): String? {
        val signedDays = getSignCount(context)
        if (signedDays >= PREVIEW_MAX_SIGN_DAYS) {
            return "已达成 5 天签到体验上限"
        }
        val firstUse = getFirstUseTimestamp(context)
        val now = System.currentTimeMillis()
        val daysElapsed = ((now - firstUse) / (1000L * 3600 * 24)).toInt()
        if (daysElapsed >= PREVIEW_MAX_DAYS) {
            return "已超过 7 天预览版有效期"
        }
        return null
    }

    fun getRemainingPreviewDays(context: Context): Int {
        val firstUse = getFirstUseTimestamp(context)
        val now = System.currentTimeMillis()
        val daysElapsed = ((now - firstUse) / (1000L * 3600 * 24)).toInt()
        return (PREVIEW_MAX_DAYS - daysElapsed).coerceAtLeast(0)
    }

    fun getRemainingSignDays(context: Context): Int {
        return (PREVIEW_MAX_SIGN_DAYS - getSignCount(context)).coerceAtLeast(0)
    }

    // ==========================================
    // 极速模式直达 URL 缓存
    // ==========================================
    private const val KEY_LAST_SIGN_URL = "key_last_sign_url"
    const val DEFAULT_KNOWN_SIGN_URL = "https://fdm.lzjtu.edu.cn/wec-counselor-sign-apps/stu/mobile/index.html?signBody=true&signWid=200616065&signInstanceWid=64416&from=push#/detail/signBody"

    fun saveLastSignUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_LAST_SIGN_URL, url).apply()
    }

    fun getLastSignUrl(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_SIGN_URL, null) ?: DEFAULT_KNOWN_SIGN_URL
    }
}

