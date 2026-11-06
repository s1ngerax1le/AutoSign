package com.autosign.cpdaily.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import com.autosign.cpdaily.service.SignAccessibilityService
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * 极速冲榜核心引擎 (SprintSignEngine)
 * 专为争夺今日校园排行榜第 1 名打造！
 *
 * 核心架构：
 * 1. 提前预热驻留 (T - 20s): 硬件级亮屏与秒级 PIN 解锁 -> GPS 预热锁定 -> 提前直达【辅导员通知】列表页驻留
 * 2. 06:30:00 准点秒杀发车: 06:30:00.000 毫秒级直接点击最新签到条目 (10ms)，100% 携带原生登录态秒进真实详情页
 * 3. 详情页极速击发流水线: 拍照签到 (50ms) -> 去拍照 (50ms) -> 相机快门 (100ms) -> 对勾确认 (50ms) -> 提交 (50ms)
 * 4. 彻底杜绝 URL 鉴权失效与“加载中...”死循环卡死；遇阻 3 秒即刻自愈降级至普通模式，绝无假成功！
 */
object SprintSignEngine {
    private const val TAG = "SprintSignEngine"

    private fun getRealDisplaySize(context: Context): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val bounds = wm?.maximumWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                Pair(bounds.width(), bounds.height())
            } else {
                val dm = Resources.getSystem().displayMetrics
                Pair(dm.widthPixels, dm.heightPixels)
            }
        } else {
            val dm = DisplayMetrics()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    /**
     * 执行极速冲榜全流程
     * @param isSimulation 是否为平时测试演练（演练模式不等待 06:30，测试预热直达与准星校准）
     */
    suspend fun executeSprint(context: Context, isSimulation: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val (width, height) = getRealDisplaySize(context)
        val tagPrefix = if (isSimulation) "⚡ [极速演练]" else "👑 [极速冲榜]"
        SignAccessibilityService.log("$tagPrefix 引擎启动！屏幕物理标定: ${width}x${height}")

        val pin = PrefsUtil.getPin(context)
        val sprintStartTime = System.currentTimeMillis()
        var needFallback = false

        try {
            SignAccessibilityService.isTaskRunning = true

            // 启动打卡全流程自动高清录屏
            if (PrefsUtil.isRecordScreenEnabled(context)) {
                ScreenRecorder.startRecording(context)
            }

            // ==========================================
            // 阶段 1: 提前唤醒与环境预热 (GPS 常驻 + PIN 解锁)
            // ==========================================
            if (!isSimulation) {
                SignAccessibilityService.log("🔓 [阶段 1/4] 硬件开屏与 PIN 秒级解锁...")
                RootUtil.wakeAndUnlock(context, pin)

                SignAccessibilityService.log("📡 [阶段 1/4] 硬件级高精度 GPS 芯片预热常驻...")
                RootUtil.execCommand("pm grant com.autosign.cpdaily android.permission.ACCESS_FINE_LOCATION", context)
                RootUtil.execCommand("pm grant com.autosign.cpdaily android.permission.ACCESS_COARSE_LOCATION", context)
                GpsPrewarmer.startPrewarm(context)

                SignAccessibilityService.log("🔥 [阶段 1/4] 启动今日校园前台暖机...")
                RootUtil.launchCpdaily(context)
                delay(1200)
            } else {
                SignAccessibilityService.log("📡 启动高精度 GPS 芯片预热常驻...")
                RootUtil.execCommand("pm grant com.autosign.cpdaily android.permission.ACCESS_FINE_LOCATION", context)
                RootUtil.execCommand("pm grant com.autosign.cpdaily android.permission.ACCESS_COARSE_LOCATION", context)
                GpsPrewarmer.startPrewarm(context)
                SignAccessibilityService.log("🔥 启动今日校园前台运行...")
                RootUtil.launchCpdaily(context)
                delay(800)
            }

            // 激活无障碍服务
            if (SignAccessibilityService.instance == null) {
                SignAccessibilityService.log("⚠️ 激活无障碍辅助驱动...")
                RootUtil.enableAccessibilityService()
                delay(300)
            }

            // ==========================================
            // 阶段 2: 前台暖机就位 (保持今日校园【讯息】活跃，推送服务随时就绪)
            // ==========================================
            SignAccessibilityService.log("🚀 [阶段 2/4] 极速切入【讯息】前台活跃位，推送雷达就绪...")
            stationAtMessageTab(width, height)

            // ==========================================
            // 阶段 3: 演练直达 vs 准点秒杀发车 (06:30 准点 URL 雷达高频嗅探)
            // ==========================================
            var signUrl: String? = null
            var enteredDetail = false
            val zeroHourActionStart: Long

            if (isSimulation) {
                zeroHourActionStart = System.currentTimeMillis()
                SignAccessibilityService.log("⚡ [演练模式] 启动雷达嗅探最新有效签到 URL 直达...")
                signUrl = PushSniffer.extractLatestSignUrl(context, minTimestampMs = 0L, isSimulation = true)
            } else {
                // 正式冲榜模式：静候准点时间归零 (默认 06:30:00)
                val targetCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, PrefsUtil.getSignStartHour(context))
                    set(Calendar.MINUTE, PrefsUtil.getSignStartMinute(context))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val waitMs = targetCalendar.timeInMillis - System.currentTimeMillis()
                if (waitMs > 1000L) {
                    val waitSecStr = String.format("%.1f", waitMs / 1000.0)
                    SignAccessibilityService.log("⏳ 前台暖机就绪！距离 ${String.format("%02d:%02d:00", PrefsUtil.getSignStartHour(context), PrefsUtil.getSignStartMinute(context))} 准点发车还有 ${waitSecStr} 秒...")
                    delay(waitMs - 200L) // 提前 200ms 唤醒雷达准备击发
                }

                zeroHourActionStart = System.currentTimeMillis()
                val minTimestampMs = targetCalendar.timeInMillis - 60_000L // 仅嗅探当日有效推送
                SignAccessibilityService.log("🚀 [准点秒杀发车] 启动高频推送雷达，毫秒级捕捉今日校园签到 URL...")

                // 核心通道 1: 毫秒级嗅探 pushsdk.db 与 cache.db (预计服务端在 06:30:06 下发)
                signUrl = PushSniffer.pollSignUrl(
                    context = context,
                    timeoutMs = 12000L,
                    intervalMs = 50L,
                    minTimestampMs = minTimestampMs,
                    isSimulation = false
                )
            }

            // ==========================================
            // 阶段 4: 直达 URL 击发 或 备用通道兜底
            // ==========================================
            if (!signUrl.isNullOrBlank()) {
                SignAccessibilityService.log("⚡ [URL 直达启动] 原生容器秒开详情页: $signUrl")
                RootUtil.launchBrowsePage(signUrl)
                enteredDetail = waitForSignDetailPage(timeoutMs = 4000L)
            }

            // 备用通道 1: 若推送嗅探超时或直达未入详情页，切入【辅导员通知】下拉刷新备用通道
            if (!enteredDetail) {
                SignAccessibilityService.log("⚠️ [雷达未命中/直达遇阻] 启用【辅导员通知】下拉刷新备用通道...")
                val stationSuccess = stationAtCounselorNoticePage(width, height)
                if (stationSuccess) {
                    SignAccessibilityService.log("🔄 下拉刷新通知列表以强制向服务端拉取最新签到条目...")
                    RootUtil.execCommand("input swipe ${(width * 0.5f).toInt()} ${(height * 0.35f).toInt()} ${(width * 0.5f).toInt()} ${(height * 0.75f).toInt()} 300")
                    delay(800)

                    // 下拉刷新后，今日校园会将新通知写入 cache.db，再次查库
                    val retryUrl = PushSniffer.extractLatestSignUrl(context, minTimestampMs = if (isSimulation) 0L else System.currentTimeMillis() - 120_000L, isSimulation = isSimulation)
                    if (!retryUrl.isNullOrBlank()) {
                        SignAccessibilityService.log("⚡ 刷新后成功嗅探到 URL: $retryUrl，直达击发！")
                        RootUtil.launchBrowsePage(retryUrl)
                        enteredDetail = waitForSignDetailPage(timeoutMs = 3500L)
                    }

                    if (!enteredDetail) {
                        SignAccessibilityService.log("👉 模拟点击通知列表最新签到卡片...")
                        enteredDetail = clickLatestSignItemAndEnterDetail(width, height, isSimulation)
                    }
                }
            }

            if (!enteredDetail) {
                SignAccessibilityService.log("⚠️ 未能顺利进入真实签到详情页，启动普通模式保底降级...")
                needFallback = true
                fallbackToRegularSign(context)
                return@withContext false
            }

            // ==========================================
            // 阶段 5: 真实签到详情页击发流水线 (图 a -> 图 b -> 图 c -> 图 d -> 图 e)
            // ==========================================
            SignAccessibilityService.log("🎯 [阶段 4/4] 已身处真实签到详情页，开始执行完整打卡流水线...")

            // 5.1 检查是否已经处于【已签到】状态
            val service = SignAccessibilityService.instance
            val initialRoot = service?.rootInActiveWindow
            val alreadySigned = initialRoot?.findAccessibilityNodeInfosByText("已签到")?.any { it.isVisibleToUser } == true ||
                    initialRoot?.findAccessibilityNodeInfosByText("签到信息")?.any { it.isVisibleToUser } == true

            if (alreadySigned) {
                SignAccessibilityService.log("🎉 详情页核验成功：当前已处于【已签到】状态，无需重复打卡！")
                finishSprintSuccess(context, sprintStartTime, zeroHourActionStart, isSimulation = isSimulation)
                delay(1000)
                if (!isSimulation) RootUtil.lockScreen()
                return@withContext true
            }

            // 5.2 自适应检测并关闭【切换签到范围】底部弹出框 (图三)：优先点击【取消】
            dismissRangePickerIfPresent(width, height)

            // 5.3 击发【拍照签到】按钮 (主卡片正中心巨大蓝色圆形按钮: 50%, 48.5%)
            // 严禁点击屏幕底部 (94.7%)，底部为【签到/有问题/排行榜】Tab，误触会导致跳入反馈页或排行榜！
            val centerPhotoX = (width * 0.50f).toInt()
            val centerPhotoY = (height * 0.485f).toInt()
            var photoSignClicked = false
            val waitSignButtonStart = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - waitSignButtonStart < 4500L)) {
                dismissRangePickerIfPresent(width, height)

                // 检查是否已经弹出【拍照主题】或相机已经调起
                val curRoot = service?.rootInActiveWindow
                val hasGoCamera = curRoot?.findAccessibilityNodeInfosByText("去拍照")?.isNotEmpty() == true
                val isCameraPkg = curRoot?.packageName?.contains("camera") == true
                if (hasGoCamera || isCameraPkg) {
                    photoSignClicked = true
                    SignAccessibilityService.log("⚡ 探测到拍照弹窗或相机已就绪，立即切入拍照流程！")
                    break
                }

                val signButtons = curRoot?.let { r ->
                    r.findAccessibilityNodeInfosByText("拍照签到")
                        .ifEmpty { r.findAccessibilityNodeInfosByText("去签到") }
                        .ifEmpty { r.findAccessibilityNodeInfosByText("立即签到") }
                }?.filter { it.isVisibleToUser }

                val validButton = signButtons?.firstOrNull()
                if (validButton != null) {
                    val r = Rect()
                    validButton.getBoundsInScreen(r)
                    validButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    val tapX = if (r.centerX() > 0) r.centerX() else centerPhotoX
                    val tapY = if (r.centerY() > 0 && r.centerY() < height * 0.85f) r.centerY() else centerPhotoY
                    RootUtil.execCommand("input tap $tapX $tapY")
                    photoSignClicked = true
                    SignAccessibilityService.log("👉 [拍照签到已触发] 命中节点坐标: ($tapX, $tapY)")
                    break
                } else {
                    // 保底高频脉冲轻触卡片正中央蓝色大圆圈 (50%, 48.5%)
                    // GPS 定位完成后该圆圈瞬间激活，脉冲轻触可实现 0 延迟秒发车！
                    RootUtil.execCommand("input tap $centerPhotoX $centerPhotoY")
                }
                delay(200)
            }

            if (!photoSignClicked) {
                RootUtil.execCommand("input tap $centerPhotoX $centerPhotoY")
                val costSign = System.currentTimeMillis() - zeroHourActionStart
                SignAccessibilityService.log("👉 [拍照签到保底触控] 模拟点击卡片中央大圆圈 ($centerPhotoX, $centerPhotoY)，耗时: ${costSign}ms")
            }

            // 5.4 击发【拍照主题】弹窗 -> 点击【去拍照】(图二右侧，70.5%, 60.3%)
            val goCameraX = (width * 0.705f).toInt()
            val goCameraY = (height * 0.603f).toInt()
            var goCameraClicked = false
            val waitDialogStart = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - waitDialogStart < 4000L)) {
                val dialogRoot = service?.rootInActiveWindow
                val isCameraPkg = dialogRoot?.packageName?.contains("camera") == true
                if (isCameraPkg) {
                    goCameraClicked = true
                    SignAccessibilityService.log("⚡ 探测到相机已成功拉起！")
                    break
                }

                val goCameraNodes = dialogRoot?.findAccessibilityNodeInfosByText("去拍照")
                    ?.filter { it.isVisibleToUser }
                val goBtn = goCameraNodes?.firstOrNull()
                if (goBtn != null) {
                    val r = Rect()
                    goBtn.getBoundsInScreen(r)
                    goBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    val tapX = if (r.centerX() > 0) r.centerX() else goCameraX
                    val tapY = if (r.centerY() > 0) r.centerY() else goCameraY
                    RootUtil.execCommand("input tap $tapX $tapY")
                    goCameraClicked = true
                    SignAccessibilityService.log("👉 [去拍照已点击] 命中节点坐标: ($tapX, $tapY)")
                    break
                } else if (System.currentTimeMillis() - waitDialogStart > 400L) {
                    RootUtil.execCommand("input tap $goCameraX $goCameraY")
                }
                delay(80)
            }

            if (!goCameraClicked) {
                RootUtil.execCommand("input tap $goCameraX $goCameraY")
                val costDialog = System.currentTimeMillis() - zeroHourActionStart
                SignAccessibilityService.log("👉 [去拍照保底触控] 模拟点击弹窗右侧按钮 ($goCameraX, $goCameraY)，耗时: ${costDialog}ms")
            }

            // 5.5 捕捉相机取景器 -> 瞬间击发快门
            var shutterClicked = false
            val waitCameraStart = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - waitCameraStart < 4500L)) {
                val cameraRoot = service?.rootInActiveWindow
                val isCameraPkg = cameraRoot?.packageName?.contains("camera") == true
                val shutterNodes = cameraRoot?.let { r ->
                    r.findAccessibilityNodeInfosByText("快门")
                        .ifEmpty { r.findAccessibilityNodeInfosByText("拍摄") }
                        .ifEmpty { r.findAccessibilityNodeInfosByText("拍照") }
                }?.filter { it.isVisibleToUser }

                if (isCameraPkg || !shutterNodes.isNullOrEmpty()) {
                    delay(300) // 曝光与对焦预留
                    shutterNodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    val shutterX = (width * 0.4956f).toInt()
                    val shutterY = (height * 0.8566f).toInt()
                    RootUtil.execCommand("input tap $shutterX $shutterY")
                    RootUtil.execCommand("input keyevent 25") // KEYCODE_VOLUME_DOWN
                    val cost = System.currentTimeMillis() - zeroHourActionStart
                    SignAccessibilityService.log("📸 [快门瞬间击发！] 耗时: ${cost}ms")
                    shutterClicked = true
                    break
                }
                delay(60)
            }

            if (!shutterClicked) {
                val shutterX = (width * 0.4956f).toInt()
                val shutterY = (height * 0.8566f).toInt()
                RootUtil.execCommand("input tap $shutterX $shutterY")
                RootUtil.execCommand("input keyevent 25")
            }

            // 5.6 捕捉快门确认对勾 [✓] / [确定]
            val waitConfirmStart = System.currentTimeMillis()
            var confirmClicked = false

            while (isActive && (System.currentTimeMillis() - waitConfirmStart < 4000L)) {
                val confirmRoot = service?.rootInActiveWindow
                val confirmNodes = confirmRoot?.let { r ->
                    r.findAccessibilityNodeInfosByText("确定")
                        .ifEmpty { r.findAccessibilityNodeInfosByText("完成") }
                        .ifEmpty { r.findAccessibilityNodeInfosByText("✓") }
                        .ifEmpty { r.findAccessibilityNodeInfosByText("√") }
                        .ifEmpty { r.findAccessibilityNodeInfosByText("使用") }
                }?.filter { it.isVisibleToUser }

                val cBtn = confirmNodes?.firstOrNull()
                if (cBtn != null) {
                    val r = Rect()
                    cBtn.getBoundsInScreen(r)
                    cBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    val tapX = if (r.centerX() > 0) r.centerX() else (width * 0.6906f).toInt()
                    val tapY = if (r.centerY() > 0) r.centerY() else (height * 0.9116f).toInt()
                    RootUtil.execCommand("input tap $tapX $tapY")
                    confirmClicked = true
                    SignAccessibilityService.log("👉 [照片已确认勾选] 命中节点坐标: ($tapX, $tapY)")
                    break
                } else if (System.currentTimeMillis() - waitConfirmStart > 600L) {
                    val confirmX = (width * 0.6906f).toInt()
                    val confirmY = (height * 0.9116f).toInt()
                    RootUtil.execCommand("input tap $confirmX $confirmY")
                }
                delay(80)
            }

            if (!confirmClicked) {
                val confirmX = (width * 0.6906f).toInt()
                val confirmY = (height * 0.9116f).toInt()
                RootUtil.execCommand("input tap $confirmX $confirmY")
                val costConfirm = System.currentTimeMillis() - zeroHourActionStart
                SignAccessibilityService.log("👉 [照片确认保底触控] 模拟点击对勾 ($confirmX, $confirmY)，耗时: ${costConfirm}ms")
            }

            // 5.7 捕捉签到成功凭据 ("签到成功 动态防伪条")
            val waitSuccessStart = System.currentTimeMillis()
            var successDetected = false

            while (isActive && (System.currentTimeMillis() - waitSuccessStart < 5000L)) {
                val resultRoot = service?.rootInActiveWindow
                val hasSuccessPopup = resultRoot?.findAccessibilityNodeInfosByText("签到成功")?.isNotEmpty() == true ||
                        resultRoot?.findAccessibilityNodeInfosByText("动态防伪条")?.isNotEmpty() == true ||
                        resultRoot?.findAccessibilityNodeInfosByText("已签到")?.isNotEmpty() == true ||
                        resultRoot?.findAccessibilityNodeInfosByText("返回APP首页")?.isNotEmpty() == true

                if (hasSuccessPopup) {
                    successDetected = true
                    break
                }
                delay(120)
            }

            if (successDetected) {
                SignAccessibilityService.log("🎉 [检测到签到成功凭据！]")
                val backHomeNodes = service?.rootInActiveWindow?.findAccessibilityNodeInfosByText("返回APP首页")
                backHomeNodes?.firstOrNull { it.isVisibleToUser }?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                RootUtil.execCommand("input tap ${(width * 0.50f).toInt()} ${(height * 0.77f).toInt()}")

                finishSprintSuccess(context, sprintStartTime, zeroHourActionStart, isSimulation = isSimulation)
                delay(1200)
                if (!isSimulation) {
                    RootUtil.lockScreen() // 正式打卡后自动熄屏
                } else {
                    RootUtil.launchAutoSign(context) // 演练后返回本应用
                }
                return@withContext true
            } else {
                SignAccessibilityService.log("⚠️ [核验失败] 未能检测到签到成功凭据！")
                if (!isSimulation) {
                    SignAccessibilityService.log("🔄 极速流程未成功捕获凭证，启动普通模式降级兜底...")
                    needFallback = true
                    fallbackToRegularSign(context)
                } else {
                    RootUtil.launchAutoSign(context)
                }
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "极速冲榜引擎异常: ${e.message}", e)
            SignAccessibilityService.log("❌ 冲榜引擎异常: ${e.message}，启动普通模式降级兜底...")
            needFallback = true
            fallbackToRegularSign(context)
            return@withContext false
        } finally {
            if (PrefsUtil.isRecordScreenEnabled(context)) {
                ScreenRecorder.stopRecording(context)
            }
            GpsPrewarmer.stopPrewarm()
            if (!needFallback) {
                SignAccessibilityService.isTaskRunning = false
            }
        }
    }

    /**
     * 切入今日校园【讯息】前台活跃位并维持通道活跃
     */
    private suspend fun stationAtMessageTab(width: Int, height: Int) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 4000L) {
            val root = SignAccessibilityService.instance?.rootInActiveWindow
            if (root != null && root.packageName == "com.wisedu.cpdaily") {
                val inMessageList = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/rv_con_list").isNotEmpty()
                if (inMessageList) {
                    SignAccessibilityService.log("📍 今日校园前台就绪，推送监听通道已全速激活")
                    return
                }
                val msgTabs = root.findAccessibilityNodeInfosByText("讯息")
                    .ifEmpty { root.findAccessibilityNodeInfosByText("消息") }
                val msgTab = msgTabs.firstOrNull { it.isVisibleToUser }
                if (msgTab != null) {
                    val rect = android.graphics.Rect()
                    msgTab.getBoundsInScreen(rect)
                    msgTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    RootUtil.execCommand("input tap ${rect.centerX()} ${rect.centerY()}")
                    delay(500)
                    return
                }
            }
            delay(100)
        }
    }

    /**
     * 自适应检测并关闭【切换签到范围】底部选择框 (图三)
     * 避免遮挡底部的【拍照签到】按钮
     */
    suspend fun dismissRangePickerIfPresent(width: Int, height: Int): Boolean {
        val service = SignAccessibilityService.instance ?: return false
        val root = service.rootInActiveWindow ?: return false

        // 排除【拍照主题】弹窗（图二包含“去拍照”或“拍照主题”）
        val isThemeDialog = root.findAccessibilityNodeInfosByText("去拍照").isNotEmpty() ||
                root.findAccessibilityNodeInfosByText("拍照主题").isNotEmpty()
        if (isThemeDialog) return false

        // 检查是否存在【切换签到范围】选择器（图三）：
        // 包含“取消”且伴随“确认”/“东方公寓”/“兰州交通大学”/“切换签到范围”，或“拍照签到”未出现但存在“取消”
        val cancelNodes = root.findAccessibilityNodeInfosByText("取消")
        val hasRangeKeywords = root.findAccessibilityNodeInfosByText("确认").isNotEmpty() ||
                root.findAccessibilityNodeInfosByText("东方公寓").isNotEmpty() ||
                root.findAccessibilityNodeInfosByText("兰州交通大学").isNotEmpty() ||
                root.findAccessibilityNodeInfosByText("切换签到范围").isNotEmpty()

        val cancelNode = cancelNodes.firstOrNull { it.isVisibleToUser }
        if (cancelNode != null && (hasRangeKeywords || root.findAccessibilityNodeInfosByText("拍照签到").isEmpty())) {
            val r = Rect()
            cancelNode.getBoundsInScreen(r)
            cancelNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val tapX = if (r.centerX() > 0) r.centerX() else (width * 0.087f).toInt()
            val tapY = if (r.centerY() > 0) r.centerY() else (height * 0.690f).toInt()
            RootUtil.execCommand("input tap $tapX $tapY")
            SignAccessibilityService.log("👉 [自适应关闭] 探测到【切换签到范围】弹窗，已点击【取消】($tapX, $tapY)")
            delay(500)
            return true
        } else if (hasRangeKeywords && root.findAccessibilityNodeInfosByText("拍照签到").isEmpty()) {
            // 无障碍树未扫描到取消节点时，向图三左上角取消坐标 (8.7%, 69.0%) 派发物理触控
            val cancelX = (width * 0.087f).toInt()
            val cancelY = (height * 0.690f).toInt()
            RootUtil.execCommand("input tap $cancelX $cancelY")
            SignAccessibilityService.log("👉 [保底坐标关闭] 派发【切换签到范围】取消物理触控 ($cancelX, $cancelY)")
            delay(500)
            return true
        }
        return false
    }

    /**
     * 等待并确认是否成功切入【真实签到详情页】(BrowsePageActivity)
     */
    private suspend fun waitForSignDetailPage(timeoutMs: Long = 4000L): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val root = SignAccessibilityService.instance?.rootInActiveWindow
            if (root != null && root.packageName == "com.wisedu.cpdaily") {
                val hasError = root.findAccessibilityNodeInfosByText("For input string").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("undefined ~ undefined").isNotEmpty()
                if (hasError) {
                    SignAccessibilityService.log("⚠️ 检测到详情页异常 (参数失效或鉴权错误)，准备转备用通道...")
                    return false
                }

                val hasBrowseBar = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/browse_title_bar").isNotEmpty()
                val hasSpecificSignTitle = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/tv_title")
                    .any {
                        val t = it.text?.toString() ?: ""
                        t.contains("学生") || t.contains("每日签到") || t.contains("打卡")
                    }
                val hasSignContent = root.findAccessibilityNodeInfosByText("本专科学生每日签到打卡").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("拍照签到").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("已签到").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("去签到").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("签到范围").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("切换签到范围").isNotEmpty()

                if (hasSignContent || (hasBrowseBar && hasSpecificSignTitle)) {
                    val arrivedMs = System.currentTimeMillis() - start
                    SignAccessibilityService.log("⚡ 成功切入【真实签到详情页】！耗时: ${arrivedMs}ms")
                    return true
                }
            }
            delay(50)
        }
        return false
    }

    /**
     * 毫秒级进入【讯息 -> 辅导员通知】列表页并驻留
     */
    private suspend fun stationAtCounselorNoticePage(width: Int, height: Int): Boolean {
        val startTime = System.currentTimeMillis()
        var lastActionTime = 0L

        while (System.currentTimeMillis() - startTime < 10000L) {
            val service = SignAccessibilityService.instance
            val root = service?.rootInActiveWindow

            if (root != null && root.packageName == "com.wisedu.cpdaily") {
                // 1. 如果已经身处【辅导员通知】列表页 (ContainerActivity)，驻留成功！
                val inCounselorPage = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/rv_app_msg").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/title_name").any { it.text?.toString()?.contains("辅导员通知") == true }

                if (inCounselorPage) {
                    val cost = System.currentTimeMillis() - startTime
                    SignAccessibilityService.log("📍 已成功驻留【辅导员通知】页面！耗时: ${cost}ms")
                    return true
                }

                val now = System.currentTimeMillis()

                // 2. 如果停留在【讯息】列表页 (HomeActivity 中的 rv_con_list)
                val inMessageList = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/rv_con_list").isNotEmpty()
                if (inMessageList) {
                    if (now - lastActionTime > 600L) {
                        val counselorTitleNodes = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/rc_title")
                            .filter { it.text?.toString()?.contains("辅导员通知") == true }

                        val targetItem = counselorTitleNodes.firstOrNull { it.isVisibleToUser }
                        if (targetItem != null) {
                            val rect = Rect()
                            targetItem.getBoundsInScreen(rect)
                            targetItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            RootUtil.execCommand("input tap ${rect.centerX()} ${rect.centerY()}")
                            SignAccessibilityService.log("👉 [预热] 点击【辅导员通知】(${rect.centerX()}, ${rect.centerY()})，进入列表中...")
                            lastActionTime = now
                            delay(600)
                            continue
                        } else {
                            RootUtil.execCommand("input tap ${width * 0.50f} ${height * 0.209f}")
                            SignAccessibilityService.log("👉 [预热保底] 模拟点击讯息列表首项...")
                            lastActionTime = now
                            delay(600)
                            continue
                        }
                    }
                    delay(50)
                    continue
                }

                // 3. 如果在今日校园其他主 Tab (首页、服务、我的)，点击底栏【讯息】Tab
                val inBottomNav = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/common_tab_layout").isNotEmpty()
                if (inBottomNav) {
                    if (now - lastActionTime > 600L) {
                        val msgTabs = root.findAccessibilityNodeInfosByText("讯息")
                            .ifEmpty { root.findAccessibilityNodeInfosByText("消息") }
                        val msgTab = msgTabs.firstOrNull { it.isVisibleToUser }
                        if (msgTab != null) {
                            val rect = Rect()
                            msgTab.getBoundsInScreen(rect)
                            msgTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            RootUtil.execCommand("input tap ${rect.centerX()} ${rect.centerY()}")
                        } else {
                            RootUtil.execCommand("input tap ${width * 0.38f} ${height * 0.92f}")
                        }
                        SignAccessibilityService.log("👉 [预热] 点击底部【讯息】Tab...")
                        lastActionTime = now
                        delay(500)
                        continue
                    }
                    delay(50)
                    continue
                }

                // 4. 如果身处其他无关子页面，等待至少 1500ms 排除界面切换过渡期后，再派发 Back 键自愈
                if (now - lastActionTime > 1500L) {
                    SignAccessibilityService.log("↩️ [预热自愈] 当前身处非目标页面，派发返回键...")
                    RootUtil.execCommand("input keyevent 4")
                    lastActionTime = now
                    delay(600)
                    continue
                }
            } else {
                // 如果今日校园不在前台，尝试拉起
                RootUtil.launchCpdaily(SignAccessibilityService.instance?.applicationContext ?: return false)
                delay(600)
            }
            delay(40)
        }
        return false
    }

    /**
     * 在【辅导员通知】列表中毫秒级捕捉并点击最新签到条目，进入真实详情页
     */
    private suspend fun clickLatestSignItemAndEnterDetail(width: Int, height: Int, isSimulation: Boolean): Boolean {
        val startTime = System.currentTimeMillis()
        var clicked = false
        var clickTimestamp = 0L

        while (System.currentTimeMillis() - startTime < 8000L) {
            val service = SignAccessibilityService.instance
            val root = service?.rootInActiveWindow

            if (root != null && root.packageName == "com.wisedu.cpdaily") {
                // 1. 严格检查是否已经真正进入【真实签到详情页】(BrowsePageActivity)
                val hasBrowseBar = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/browse_title_bar").isNotEmpty()
                val hasSpecificSignTitle = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/tv_title")
                    .any {
                        val t = it.text?.toString() ?: ""
                        t.contains("学生") || t.contains("每日签到") || t.contains("打卡")
                    }
                val hasSignContent = root.findAccessibilityNodeInfosByText("本专科学生每日签到打卡").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("拍照签到").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("已签到").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("去签到").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("签到范围").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByText("切换签到范围").isNotEmpty()

                if (hasSignContent || (hasBrowseBar && hasSpecificSignTitle)) {
                    val arrivedMs = System.currentTimeMillis() - startTime
                    SignAccessibilityService.log("⚡ 成功切入【真实签到详情页】！直达耗时: ${arrivedMs}ms")
                    return true
                }

                // 2. 检查是否在辅导员通知列表中，点击最新签到条目
                val inCounselorPage = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/rv_app_msg").isNotEmpty() ||
                        root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/title_name").any { it.text?.toString()?.contains("辅导员通知") == true }

                if (inCounselorPage) {
                    if (!clicked || (System.currentTimeMillis() - clickTimestamp > 1200L)) {
                        val messageItems = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/message_item")
                            .filter { it.isVisibleToUser }
                        val tvContents = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/tv_content")
                            .filter { it.text?.toString()?.contains("签到") == true && it.isVisibleToUser }

                        val targetNode = tvContents.maxByOrNull {
                            val r = Rect()
                            it.getBoundsInScreen(r)
                            r.bottom
                        } ?: messageItems.maxByOrNull {
                            val r = Rect()
                            it.getBoundsInScreen(r)
                            r.bottom
                        }

                        if (targetNode != null) {
                            val rect = Rect()
                            targetNode.getBoundsInScreen(rect)
                            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            targetNode.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                            val tapX = rect.centerX().coerceIn(100, width - 100)
                            val tapY = rect.centerY().coerceIn(350, height - 150)
                            RootUtil.execCommand("input tap $tapX $tapY")
                            clicked = true
                            clickTimestamp = System.currentTimeMillis()
                            SignAccessibilityService.log("👉 [10ms 命中] 已点击最新签到条目 ($tapX, $tapY)，等待详情页呈现...")
                            delay(500)
                            continue
                        } else {
                            // 辅导员通知列表保底点击最新项 (约 640, 2447)
                            val fallbackX = (width * 0.50f).toInt()
                            val fallbackY = (height * 0.88f).toInt()
                            RootUtil.execCommand("input tap $fallbackX $fallbackY")
                            clicked = true
                            clickTimestamp = System.currentTimeMillis()
                            SignAccessibilityService.log("👉 [保底击发] 点击通知列表底部卡片 ($fallbackX, $fallbackY)...")
                            delay(500)
                            continue
                        }
                    }
                } else {
                    // 如果万一身处讯息列表，说明提前返回了，立即点击辅导员通知
                    val inMessageList = root.findAccessibilityNodeInfosByViewId("com.wisedu.cpdaily:id/rv_con_list").isNotEmpty()
                    if (inMessageList) {
                        RootUtil.execCommand("input tap ${(width * 0.50f).toInt()} ${(height * 0.209f).toInt()}")
                        delay(600)
                        continue
                    }
                }
            } else {
                RootUtil.launchCpdaily(SignAccessibilityService.instance?.applicationContext ?: return false)
                delay(600)
            }
            delay(30)
        }
        return false
    }

    private fun fallbackToRegularSign(context: Context) {
        try {
            SignAccessibilityService.log("🛡️ [自动降级兜底] 极速模式已切换至普通模式，正在杀后台重新冷启动保底完成打卡...")
            RootUtil.forceStopCpdaily()
            Thread.sleep(800)
            RootUtil.launchCpdaily(context)
            Thread.sleep(1500)
            SignAccessibilityService.startTask()
        } catch (e: Exception) {
            Log.e(TAG, "fallbackToRegularSign error: ${e.message}", e)
        }
    }

    private fun finishSprintSuccess(context: Context, startTime: Long, actionStartTime: Long, isSimulation: Boolean) {
        val totalMs = System.currentTimeMillis() - startTime
        val sprintCostMs = System.currentTimeMillis() - actionStartTime
        val secondsStr = String.format("%.2f", sprintCostMs / 1000.0)

        if (isSimulation) {
            SignAccessibilityService.log("🏁 [演练完成] 极速直达详情页与准星校准耗时: $secondsStr 秒 (${sprintCostMs}ms)！")
            NotificationHelper.sendSignSuccessNotification(context, "极速演练成功！直达详情页耗时: $secondsStr 秒")
        } else {
            SignAccessibilityService.log("🏆 [冲榜成功！] 06:30 准点签到彻底完成！准点耗时: $secondsStr 秒 (${sprintCostMs}ms)，锁定第 1 名！")
            PrefsUtil.recordSignSuccess(context)
            NotificationHelper.sendSignSuccessNotification(context, "🏆 极速冲榜成功！准点打卡耗时 $secondsStr 秒")
        }
    }
}

