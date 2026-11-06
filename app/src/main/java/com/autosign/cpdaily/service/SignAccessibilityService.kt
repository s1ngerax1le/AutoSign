package com.autosign.cpdaily.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autosign.cpdaily.model.WorkMode
import com.autosign.cpdaily.util.AdbUtil
import com.autosign.cpdaily.util.LogRepository
import com.autosign.cpdaily.util.NonRootUtil
import com.autosign.cpdaily.util.NotificationHelper
import com.autosign.cpdaily.util.PrefsUtil
import com.autosign.cpdaily.util.RootUtil
import com.autosign.cpdaily.util.ScreenRecorder
import com.autosign.cpdaily.util.SprintSignEngine
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "s1nger_AccService"
        var instance: SignAccessibilityService? = null
            private set

        var isServiceConnected = false
            private set
        var isTaskRunning = false

        private var taskJob: Job? = null
        var logListener: ((String) -> Unit)? = null

        fun log(msg: String) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val logStr = "[$time] $msg"
            Log.d(TAG, logStr)
            LogRepository.addLog(msg)
            logListener?.invoke(logStr)
        }

        fun startTask() {
            if (isTaskRunning) {
                log("⚠️ 任务已在运行中，请勿重复触发")
                return
            }
            if (instance != null) {
                instance?.launchSignPipeline()
            } else {
                log("🔄 检测到无障碍服务未绑定，正在通过 Root 静默激活...")
                CoroutineScope(Dispatchers.IO).launch {
                    RootUtil.enableAccessibilityService()
                    delay(1200)
                    if (instance != null) {
                        instance?.launchSignPipeline()
                    } else {
                        log("❌ 无障碍服务仍未连接，请在设置中重新开启服务")
                    }
                }
            }
        }

        fun resetTask() {
            isTaskRunning = false
            taskJob?.cancel()
            taskJob = null
            instance?.serviceScope?.coroutineContext?.cancelChildren()
            log("⏹ 所有任务流水线与子协程已强力清空与重置")
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private fun showToast(msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(applicationContext, "[s1ngerAutoSign] $msg", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceConnected = true
        try {
            val info = serviceInfo ?: android.accessibilityservice.AccessibilityServiceInfo()
            info.flags = info.flags or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Configuring a11y service info: ${e.message}")
        }
        log("✅ s1ngerAutoSign 无障碍辅助服务已就绪 (已启用 WebView DOM 增强识别)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 无障碍事件监听
    }

    /**
     * 智能弹窗拦截器：自动检测并关闭版本更新/公告/隐私弹窗（严格规避拍照与签到界面）
     */
    private suspend fun checkAndDismissPopups(): Boolean {
        var dismissed = false
        withContext(Dispatchers.Main) {
            rootInActiveWindow?.let { root ->
                // 1. 绝不拦截属于我们自己应用的窗口
                if (root.packageName == packageName) return@withContext

                // 2. 核心白名单：拍照主题弹窗、相机取景或签到页面，绝对不可拦截！
                if (findNodeByKeywords(root, listOf("拍照主题", "去拍照", "以校园建筑物", "拍照签到", "动态防伪条", "签到成功", "本专科学生每日签到打卡")) != null) {
                    return@withContext
                }

                // 3. 针对版本更新弹窗进行精确识别
                val isUpdateDialog = findNodeByKeywords(root, listOf("版本更新", "发现新版本", "更新提示", "升级提示", "新版本", "立即更新", "马上升级")) != null

                if (isUpdateDialog) {
                    val dismissKeywords = listOf("暂不更新", "以后再说", "取消", "稍后", "忽略", "下次再说", "我知道了", "跳过", "关闭")
                    for (kw in dismissKeywords) {
                        val node = findNodeByTextOrDesc(root, kw)
                        if (node != null && node.isVisibleToUser) {
                            clickNodeOrParent(node)
                            log("🛡️ 检测到版本更新弹窗，已自动关闭：【$kw】")
                            dismissed = true
                            break
                        }
                    }
                } else {
                    // 非明确更新弹窗时，仅匹配明确的放弃/跳过词，绝不匹配单纯的“取消”（防止误杀正常业务表单）
                    val safeDismissKeywords = listOf("暂不更新", "以后再说", "下次再说", "我知道了", "跳过")
                    for (kw in safeDismissKeywords) {
                        val node = findNodeByTextOrDesc(root, kw)
                        if (node != null && node.isVisibleToUser) {
                            clickNodeOrParent(node)
                            log("🛡️ 检测并自动关闭通知弹窗：【$kw】")
                            dismissed = true
                            break
                        }
                    }
                }
            }
        }
        if (dismissed) delay(800)
        return dismissed
    }

    /**
     * 启动完整的打卡时序流水线（带全局强校验与自定义重试机制）
     */
    fun launchSignPipeline() {
        taskJob?.cancel()
        taskJob = serviceScope.launch {
            isTaskRunning = true
            try {
                if (PrefsUtil.isRecordScreenEnabled(applicationContext)) {
                    ScreenRecorder.startRecording(applicationContext)
                }

                if (PrefsUtil.isPreviewExpired(applicationContext)) {
                    log("❌ [已达使用上限] 已签到 5 天，预览版已停用。请更新正式版！")
                    showToast("已签到 5 天，预览版已停用，请更新正式版")
                    NotificationHelper.sendSignFailureNotification(applicationContext, "已签到 5 天，预览版已停用，请更新正式版以继续自动打卡！")
                    return@launch
                }

                // 清理旧通知，防止历史成功通知内容干扰成功凭证探测
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancelAll()

                val maxRetries = PrefsUtil.getMaxRetries(applicationContext)
                log("🚀 [s1ngerAutoSign] 自动化打卡流水线启动！(失败重试上限: $maxRetries 次)")
                showToast("打卡流水线启动，等待开屏...")

                var succeeded = false
                for (attempt in 1..maxRetries) {
                    if (!isActive) break
                    log("📍 [第 $attempt/$maxRetries 次尝试] 开始执行今日校园打卡...")
                    succeeded = executeSingleAttempt(attempt, maxRetries)
                    if (succeeded) {
                        log("🎉 [打卡成功] 第 $attempt 次尝试打卡彻底完成！")
                        PrefsUtil.recordSignSuccess(applicationContext)
                        NotificationHelper.sendSignSuccessNotification(applicationContext, "今日校园自动签到成功！")
                        break
                    }

                    if (attempt < maxRetries) {
                        log("⚠️ [第 $attempt 次尝试失败] 某个步骤未如期达成！正在强制杀掉今日校园后台并在 2.5 秒后重启重试...")
                        showToast("步骤异常，杀后台重启重试 ($attempt/$maxRetries)...")
                        RootUtil.forceStopCpdaily()
                        delay(2500)
                        if (!isActive) break
                        RootUtil.launchCpdaily(applicationContext)
                        delay(4500)
                    }
                }

                if (!succeeded) {
                    log("❌ [流程终止] 已连续重试 $maxRetries 次均未能确认签到成功，发送系统通知报警！")
                    showToast("⚠️ 打卡连续 $maxRetries 次失败，请导出日志发给开发者")
                    NotificationHelper.sendSignFailureNotification(applicationContext, "已重试 $maxRetries 次仍未检测到签到成功凭据")
                }

                delay(1500)
                finishTaskAndCleanUp(succeeded)

            } catch (e: CancellationException) {
                log("ℹ️ 打卡任务已取消")
            } catch (e: Exception) {
                log("❌ 打卡流水线异常: ${e.message}")
                NotificationHelper.sendSignFailureNotification(applicationContext, "执行异常: ${e.message}")
            } finally {
                if (PrefsUtil.isRecordScreenEnabled(applicationContext)) {
                    ScreenRecorder.stopRecording(applicationContext)
                }
                isTaskRunning = false
            }
        }
    }

    private fun getRealDisplaySize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val bounds = wm?.maximumWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                Pair(bounds.width(), bounds.height())
            } else {
                val dm = Resources.getSystem().displayMetrics
                Pair(dm.widthPixels, dm.heightPixels)
            }
        } else {
            val dm = DisplayMetrics()
            val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    /**
     * 单次完整签到时序尝试，严格按照 图 a -> b -> c -> d -> e 闭环校验
     * 若任意步骤超时或校验未达成，返回 false 触发外层杀后台重跑
     */
    private suspend fun executeSingleAttempt(attemptIndex: Int, maxRetries: Int): Boolean = coroutineScope {
        val (width, height) = getRealDisplaySize()
        log("📱 [屏幕物理标定] 真实物理显示分辨率: ${width}x${height}")

        // ==========================================
        // 阶段 0: 冷启动冗余等待与更新弹窗拦截
        // ==========================================
        log("⏳ [冷启动防护] 今日校园加载中，等待 4.5 秒...")
        delay(4500)
        checkAndDismissPopups()
        if (!isActive) return@coroutineScope false

        // ==========================================
        // 阶段 1: 等待并点击底部【讯息】Tab
        // ==========================================
        log("🔍 [步骤 1] 正在检索首页底部【讯息】Tab...")
        checkAndDismissPopups()
        val msgTabNode = waitForNode(timeoutMs = 10000) { root ->
            findNodeByTextOrDesc(root, "讯息")
        }
        if (!isActive) return@coroutineScope false

        if (msgTabNode != null) {
            clickNodeOrParent(msgTabNode)
            log("👉 已点击【讯息】Tab，等待 2.0 秒...")
        } else {
            log("⚠️ 未检索到【讯息】Tab，自适应模拟点击第 2 个 Tab 区域...")
            adaptiveClick(width * 0.38f, height * 0.95f)
        }
        delay(2000)
        checkAndDismissPopups()
        if (!isActive) return@coroutineScope false

        // ==========================================
        // 阶段 2: 等待并点击【辅导员通知】
        // ==========================================
        log("🔍 [步骤 2] 正在检索【辅导员通知】...")
        val noticeNode = waitForNode(timeoutMs = 8000) { root ->
            findNodeByKeywords(root, listOf("辅导员通知", "通知", "今日通知", "辅导员"))
        }
        if (!isActive) return@coroutineScope false

        if (noticeNode != null) {
            clickNodeOrParent(noticeNode)
            log("👉 已点击【辅导员通知】，等待 2.5 秒渲染...")
        } else {
            log("⚠️ 未检索到辅导员通知，尝试列表首项自适应点击...")
            adaptiveClick(width * 0.50f, height * 0.20f)
        }
        delay(2500)
        checkAndDismissPopups()
        if (!isActive) return@coroutineScope false

        // ==========================================
        // 阶段 3: 获取通知列表最新签到条目
        // ==========================================
        log("🔍 [步骤 3] 正在获取通知列表最新【签到】条目...")
        var clickedSign = false
        val signNode = waitForNode(timeoutMs = 7000) { root ->
            val signItems = root.findAccessibilityNodeInfosByText("签到")
            if (!signItems.isNullOrEmpty()) {
                signItems.lastOrNull { it.isVisibleToUser }
            } else null
        }
        if (!isActive) return@coroutineScope false

        if (signNode != null) {
            clickNodeOrParent(signNode)
            clickedSign = true
            log("👉 已点击最新【签到】条目，等待进入详情页...")
        } else {
            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    clickedSign = clickLatestMessageItem(root)
                }
            }
            if (!clickedSign) {
                log("⚠️ 列表底部自适应模拟点击兜底...")
                adaptiveClick(width * 0.50f, height * 0.77f)
            }
            log("👉 已触发最新条目，等待 3.5 秒进入详情页...")
        }
        delay(3500)
        checkAndDismissPopups()
        if (!isActive) return@coroutineScope false

        // ==========================================
        // ==========================================
        // 阶段 4: 进入签到页面 (图 a: 等待定位完成并点击“拍照签到”)
        // ==========================================
        log("🔍 [步骤 4 (图 a)] 进入签到主页，等待 GPS 自动定位...")
        showToast("等待 GPS 定位完成...")

        // 4.1 检查是否已处于完成打卡状态 (严格限制今日校园内部精准文字，防止误判)
        val alreadySignedNode = waitForNode(timeoutMs = 1500) { root ->
            if (root.packageName == "com.wisedu.cpdaily") {
                findNodeExactText(root, "已签到")
            } else null
        }
        if (alreadySignedNode != null) {
            log("🎉 检测到当前打卡按钮为【已签到】状态，今日无需重复打卡！")
            showToast("今日已签到，无需重复打卡！")
            return@coroutineScope true
        }

        // 4.2 轮询等待定位完成（按钮激活变为蓝色且可点击，最长 18s）
        var photoSignNode: AccessibilityNodeInfo? = null
        for (attempt in 1..18) {
            if (!isActive) return@coroutineScope false

            // 自适应检测并关闭【切换签到范围】底部选择框 (图三)
            SprintSignEngine.dismissRangePickerIfPresent(width, height)

            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    if (root.packageName == "com.wisedu.cpdaily") {
                        photoSignNode = findNodeByKeywords(root, listOf("拍照签到", "去签到", "拍照", "立即签到"))
                    }
                }
            }
            if (photoSignNode != null && photoSignNode?.isVisibleToUser == true) {
                val rect = Rect()
                photoSignNode?.getBoundsInScreen(rect)
                log("🎯 [定位成功] 探测到【拍照签到】按钮已激活！坐标: (${rect.centerX()}, ${rect.centerY()}) (耗时 ${attempt}s)")
                break
            }
            delay(1000)
        }

        if (photoSignNode != null) {
            val rect = Rect()
            photoSignNode?.getBoundsInScreen(rect)
            clickNodeOrParent(photoSignNode!!)
            val clickX = if (rect.centerX() > 0) rect.centerX().toFloat() else width * 0.50f
            val clickY = if (rect.centerY() > 0 && rect.centerY() < height * 0.85f) rect.centerY().toFloat() else height * 0.485f
            adaptiveClick(clickX, clickY)
            log("👉 已点击【拍照签到】按钮 ($clickX, $clickY)")
        } else {
            val centerX = width * 0.50f
            val centerY = height * 0.485f
            log("⚠️ 触发卡片中央【拍照签到】大圆圈坐标保底点击 (50%, 48.5%)...")
            adaptiveClick(centerX, centerY)
        }
        // 注意：点击拍照签到后紧接着弹出【拍照主题】弹窗，严禁调用 checkAndDismissPopups 防止误关弹窗！
        delay(1500)
        if (!isActive) return@coroutineScope false

        // ==========================================
        // 阶段 5: 拍照主题弹窗 (图 b: 点击“去拍照”)
        // ==========================================
        log("🔍 [步骤 5 (图 b)] 检测【拍照主题】弹窗，查找并点击【去拍照】按钮...")
        var cameraOpened = false

        for (attempt in 1..5) {
            if (!isActive) return@coroutineScope false

            // 1. 检查相机是否已经调起
            var isCameraActive = false
            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    val isCameraPkg = root.packageName?.contains("camera") == true
                    val hasShutter = findNodeByKeywords(root, listOf("快门", "拍摄", "shutter", "capture", "v9_shutter_button")) != null
                    if (isCameraPkg || hasShutter) {
                        isCameraActive = true
                    }
                }
            }
            if (isCameraActive) {
                cameraOpened = true
                log("🎯 探测到内置/系统相机已成功调起进入取景状态！")
                break
            }

            // 2. 查找并尝试触发无障碍节点
            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    if (root.packageName == "com.wisedu.cpdaily") {
                        val node = findNodeByKeywords(root, listOf("去拍照"))
                        if (node != null && node.isVisibleToUser) {
                            val r = Rect()
                            node.getBoundsInScreen(r)
                            log("📍 [去拍照控件] 扫描到节点，bounds: $r, isClickable: ${node.isClickable}")
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                }
            }

            // 3. 间隔 150ms 派发独占真实物理触控（根据轮次微调，彻底覆盖右侧去拍照按钮区）
            delay(150)
            val tapX = when (attempt) {
                1 -> width * 0.712f
                2 -> width * 0.700f
                3 -> width * 0.725f
                else -> width * 0.712f
            }
            val tapY = when (attempt) {
                1 -> height * 0.605f
                2 -> height * 0.595f
                3 -> height * 0.615f
                else -> height * 0.605f
            }

            log("👉 [第 $attempt 次触控] 向【去拍照】坐标 (${tapX.toInt()}, ${tapY.toInt()}) 派发真实物理触控手势...")
            val clickSuccess = adaptiveClick(tapX, tapY)
            log("👉 物理触控手势执行状态: $clickSuccess")

            delay(2500)

            // 4. 检查弹窗是否已经消失或相机调起
            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    val isCameraPkg = root.packageName?.contains("camera") == true
                    val stillOnDialog = findNodeByKeywords(root, listOf("拍照主题", "以校园建筑物")) != null
                    if (isCameraPkg || !stillOnDialog) {
                        cameraOpened = true
                    }
                }
            }
            if (cameraOpened) {
                log("🎯 【拍照主题】弹窗已顺利关闭并进入相机流程！")
                break
            }
            log("⚠️ 【拍照主题】弹窗仍未关闭，正在执行第 ${attempt + 1} 次重试...")
        }

        if (!cameraOpened) {
            log("❌ [步骤 5 未达成] 多次点击【去拍照】未能调起相机，触发重启重试！")
            return@coroutineScope false
        }
        delay(1000)
        if (!isActive) return@coroutineScope false

        // ==========================================
        // 阶段 6: 内置相机取景器 (图 c: 触发拍照快门)
        // ==========================================
        log("📸 [步骤 6 (图 c)] 相机取景就绪，等待 2.0 秒稳定对焦与光线...")
        showToast("正在拍摄打卡照片...")
        delay(2000)
        if (!isActive) return@coroutineScope false

        var shutterClicked = false
        withContext(Dispatchers.Main) {
            rootInActiveWindow?.let { root ->
                val shutter = findNodeByKeywords(root, listOf("快门", "拍摄", "拍照", "shutter", "capture", "v9_shutter_button", "shutter_button", "take photo"))
                if (shutter != null && shutter.isVisibleToUser) {
                    shutter.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    shutterClicked = true
                    log("👉 已通过快门控件节点完成拍摄")
                }
            }
        }

        delay(150)
        val shutterX = width * 0.4956f
        val shutterY = height * 0.8566f
        log("👉 触发快门物理自适应触控 (物理坐标 ${shutterX.toInt()}, ${shutterY.toInt()})...")
        adaptiveClick(shutterX, shutterY)
        val currentMode = PrefsUtil.getWorkMode(this@SignAccessibilityService)
        if (currentMode.isRoot) {
            RootUtil.triggerCameraShutter()
        } else if (currentMode.isSkz) {
            AdbUtil.keyevent(25)
        }

        log("⏳ 快门已按下，等待照片生成并进入确认预览界面 (3.0 秒)...")
        delay(3000)
        if (!isActive) return@coroutineScope false

        // ==========================================
        // 阶段 7: 照片预览确认 (图 d: 点击右下角“✓”)
        // ==========================================
        log("🔍 [步骤 7 (图 d)] 检测照片确认页面，查找右下角【✓】确认按钮...")
        showToast("确认使用照片...")

        var confirmSubmitted = false
        for (attempt in 1..5) {
            if (!isActive) return@coroutineScope false

            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    val node = findNodeByKeywords(root, listOf("✓", "√", "确定", "完成", "使用", "确认", "yes", "done", "check_view", "intent_done_apply", "btn_done", "shutter_confirm"))
                    if (node != null && node.isVisibleToUser) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
            }

            delay(150)
            // 根据真机红外实测 1280x2772 中【✓】中心 (884, 2527) 精准派发
            val tapX = when (attempt) {
                1 -> width * 0.6906f  // 严格命中 884
                2 -> width * 0.6906f
                3 -> width * 0.6750f  // 微调稍左
                4 -> width * 0.7050f  // 微调稍右
                else -> width * 0.6906f
            }
            val tapY = when (attempt) {
                1 -> height * 0.9116f  // 严格命中 2527
                2 -> height * 0.9080f  // 微调偏上 2517
                3 -> height * 0.9150f  // 微调偏下 2536
                4 -> height * 0.9116f
                else -> height * 0.9116f
            }
            log("👉 [第 $attempt 次触控] 向【✓】真实坐标 (${tapX.toInt()}, ${tapY.toInt()}) 派发物理触控...")
            val clickSuccess = adaptiveClick(tapX, tapY)
            log("👉 【✓】触控手势执行状态: $clickSuccess")

            delay(2500)

            // 检测确认页面是否已退出
            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    if (root.packageName == "com.wisedu.cpdaily") {
                        val hasWatermark = findNodeByTextOrDesc(root, "动态防伪条") != null
                        val hasSuccess = findNodeByTextOrDesc(root, "签到成功") != null
                        val hasSignPage = findNodeByKeywords(root, listOf("本专科学生每日签到打卡", "返回APP首页")) != null
                        if (hasWatermark || hasSuccess || hasSignPage) {
                            confirmSubmitted = true
                        }
                    }
                }
            }
            if (confirmSubmitted) {
                log("🎯 照片确认已成功提交，进入打卡结果核验！")
                break
            }
            log("⚠️ 照片确认界面未退出，正在重试点击【✓】...")
        }

        // ==========================================
        // 阶段 8: 签到成功凭证检测 (图 e: 严格核验“动态防伪条 / 签到成功”蓝色卡片)
        // ==========================================
        log("🔍 [步骤 8 (图 e)] 正在检测【动态防伪条 / 签到成功】官方凭证页面 (最长 18s)...")
        showToast("正在严格核验签到结果...")

        var signSucceeded = false
        for (checkAttempt in 1..18) {
            if (!isActive) return@coroutineScope false
            withContext(Dispatchers.Main) {
                rootInActiveWindow?.let { root ->
                    // 核心防护 1：包名必须属于今日校园，严防本应用终端日志、通知栏或系统 UI 干扰
                    if (root.packageName == "com.wisedu.cpdaily") {
                        val hasWatermark = findNodeByTextOrDesc(root, "动态防伪条")?.isVisibleToUser == true
                        val hasSuccessText = findNodeByTextOrDesc(root, "签到成功")?.isVisibleToUser == true
                        val hasBackHome = findNodeByTextOrDesc(root, "返回APP首页")?.isVisibleToUser == true
                        val hasStudentInfo = (findNodeByTextOrDesc(root, "学号")?.isVisibleToUser == true ||
                                              findNodeByTextOrDesc(root, "姓名")?.isVisibleToUser == true)

                        // 核心强判定：必须出现黄色“动态防伪条”，或出现“签到成功”同时伴随“返回APP首页”或学籍信息
                        if (hasWatermark || (hasSuccessText && (hasBackHome || hasStudentInfo))) {
                            signSucceeded = true
                        }
                    }
                }
            }
            if (signSucceeded) {
                log("🎯 [打卡成功凭据] 成功捕获到今日校园官方【签到成功 / 动态防伪条】蓝色凭据页面！(耗时 ${checkAttempt}s)")
                showToast("🎉 今日打卡彻底成功！")
                return@coroutineScope true
            }
            delay(1000)
        }

        log("❌ [步骤 8 校验未通过] 超时 18 秒未能在今日校园中检测到【签到成功】官方凭证！坚决判定为未成功")
        return@coroutineScope false
    }

    /**
     * 自适应点击：根据运行模式分发
     */
    private suspend fun adaptiveClick(x: Float, y: Float): Boolean {
        val mode = PrefsUtil.getWorkMode(this)
        return if (mode.isRoot) {
            RootUtil.tap(x.toInt(), y.toInt())
            true
        } else {
            NonRootUtil.dispatchClickAsync(this, x, y, durationMs = 120L)
        }
    }

    private suspend fun waitForNode(
        timeoutMs: Long,
        checkIntervalMs: Long = 400L,
        predicate: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val root = withContext(Dispatchers.Main) { rootInActiveWindow }
            if (root != null) {
                val found = predicate(root)
                if (found != null) return found
            }
            delay(checkIntervalMs)
        }
        return null
    }

    private fun findNodeByTextOrDesc(root: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val text = root.text?.toString() ?: ""
        val desc = root.contentDescription?.toString() ?: ""
        if (text.contains(targetText, ignoreCase = true) || desc.contains(targetText, ignoreCase = true)) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByTextOrDesc(child, targetText)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeExactText(root: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && (root.text?.toString() == targetText || root.contentDescription?.toString() == targetText)) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeExactText(child, targetText)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByKeywords(root: AccessibilityNodeInfo?, keywords: List<String>): AccessibilityNodeInfo? {
        if (root == null) return null
        val text = root.text?.toString() ?: ""
        val desc = root.contentDescription?.toString() ?: ""
        val viewId = root.viewIdResourceName ?: ""

        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true) || desc.contains(kw, ignoreCase = true) || viewId.contains(kw, ignoreCase = true)) {
                return root
            }
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByKeywords(child, keywords)
            if (found != null) return found
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            adaptiveClickSync(rect.centerX().toFloat(), rect.centerY().toFloat())
            return true
        }
        return false
    }

    private fun adaptiveClickSync(x: Float, y: Float) {
        val mode = PrefsUtil.getWorkMode(this)
        if (mode.isRoot) {
            RootUtil.tap(x.toInt(), y.toInt())
        } else if (mode.isSkz) {
            AdbUtil.tap(x.toInt(), y.toInt())
        } else {
            NonRootUtil.dispatchClick(this, x, y)
        }
    }

    private fun clickLatestMessageItem(root: AccessibilityNodeInfo): Boolean {
        val items = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, items)
        if (items.isNotEmpty()) {
            val latest = items.maxByOrNull {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                rect.bottom
            }
            latest?.let {
                clickNodeOrParent(it)
                return true
            }
        }
        return false
    }

    private fun collectClickableNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isClickable && node.isVisibleToUser) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectClickableNodes(node.getChild(i), list)
        }
    }

    private suspend fun finishTaskAndCleanUp(succeeded: Boolean) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mode = PrefsUtil.getWorkMode(this)

        if (succeeded) {
            log("📸 打卡成功！自动截取今日校园官方防伪卡片凭据存证...")
            val savePath = "/sdcard/AutoSign/sign_success_$timestamp.png"
            if (mode.isRoot) {
                RootUtil.takeScreenshot(savePath)
            } else {
                NonRootUtil.takeScreenshot(this)
            }
            log("💾 官方打卡成功凭据截图已存至: $savePath")
        } else {
            log("⚠️ 打卡未成功，保存现场异常截图以供排查...")
            val savePath = "/sdcard/AutoSign/sign_fail_$timestamp.png"
            if (mode.isRoot) {
                RootUtil.takeScreenshot(savePath)
            } else {
                NonRootUtil.takeScreenshot(this)
            }
            log("📁 现场异常截图已留存: $savePath")
        }

        delay(1000)
        log("🔒 执行自动息屏...")
        if (mode.isRoot) {
            RootUtil.lockScreen()
        } else {
            NonRootUtil.lockScreen(this)
        }
        log("🏁 本次打卡流水线全流程结束。")
    }

    override fun onInterrupt() {
        log("⚠️ 无障碍服务被系统中断")
        isServiceConnected = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        instance = null
        serviceScope.cancel()
    }
}
