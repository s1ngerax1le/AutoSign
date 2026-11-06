package com.autosign.cpdaily.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.autosign.cpdaily.service.SignAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object NonRootUtil {
    private const val TAG = "s1nger_NonRoot"

    /**
     * 免 Root 模拟点击指定屏幕坐标
     */
    fun dispatchClick(service: AccessibilityService, x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100L))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * 免 Root 异步模拟点击指定屏幕坐标 (等待系统底层 touch UP 事件确认完成)
     */
    suspend fun dispatchClickAsync(
        service: AccessibilityService,
        x: Float,
        y: Float,
        durationMs: Long = 100L
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()

            val success = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "dispatchClickAsync completed at ($x, $y)")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "dispatchClickAsync cancelled at ($x, $y)")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, null)

            if (!success && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    /**
     * 免 Root 模拟长距离超速弹射上滑 (破除 MIUI / HyperOS / ColorOS / OriginOS 弹簧回弹阻尼)
     */
    suspend fun dispatchSwipeUpAsync(
        service: AccessibilityService,
        width: Int = 1080,
        height: Int = 2400
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val startX = width * 0.5f
            val startY = height * 0.88f
            val endX = width * 0.5f
            val endY = height * 0.08f // 拉至屏幕顶部 8% 处，超大跨度

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            // 220ms 极速弹射手势
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 220L))
                .build()

            val success = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "dispatchSwipeUpAsync completed")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "dispatchSwipeUpAsync cancelled")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, null)

            if (!success && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    /**
     * 智能多机型自适应定位系统 PIN 键屏幕像素坐标 (覆盖主流全面屏与标准屏)
     */
    private fun findPinKeyCoordinate(service: AccessibilityService, char: Char, width: Int, height: Int): Pair<Float, Float> {
        // 第一层：动态扫描原生系统无障碍窗口树 (精准匹配键盘按键，严禁匹配锁屏时钟/通知)
        try {
            val root = service.rootInActiveWindow
            if (root != null) {
                val node = findKeyNodeInTree(root, char, width, height)
                if (node != null) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    Log.d(TAG, "🎯 动态捕获物理按键 [$char]: $rect")
                    return Pair(rect.centerX().toFloat(), rect.centerY().toFloat())
                }
            }

            // 检查独立 Keyguard Window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (window in service.windows) {
                    val winRoot = window.root ?: continue
                    val node = findKeyNodeInTree(winRoot, char, width, height)
                    if (node != null) {
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        Log.d(TAG, "🎯 从窗口 ${window.id} 捕获按键 [$char]: $rect")
                        return Pair(rect.centerX().toFloat(), rect.centerY().toFloat())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dynamic node inspection warning: ${e.message}")
        }

        // 第二层：真实锁屏键盘几何网格校准
        // 键盘位于屏幕下半区 (通常 48% ~ 88% 屏高)
        // 3列: 1/4/7 (24%), 2/5/8/0 (50%), 3/6/9 (76%)
        // 4行 (均匀递进步长 ~9.5%):
        //   第1行 (1, 2, 3): 53.5%
        //   第2行 (4, 5, 6): 63.0%
        //   第3行 (7, 8, 9): 72.5%
        //   第4行 (0):       82.0% (核心修复：彻底解决此前 77% 落在 8 与 0 之间缝隙导致 0 键漏按的问题)
        val aspectRatio = height.toFloat() / width.toFloat()
        val isTallScreen = aspectRatio >= 1.95f // 20:9, 19.5:9 全面屏

        val col1X = width * 0.24f // 1, 4, 7
        val col2X = width * 0.50f // 2, 5, 8, 0
        val col3X = width * 0.76f // 3, 6, 9

        val row1Y = if (isTallScreen) height * 0.535f else height * 0.510f // 1, 2, 3
        val row2Y = if (isTallScreen) height * 0.630f else height * 0.610f // 4, 5, 6
        val row3Y = if (isTallScreen) height * 0.725f else height * 0.710f // 7, 8, 9
        val row4Y = if (isTallScreen) height * 0.820f else height * 0.810f // 0

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
            else -> Pair(col2X, row4Y)
        }
    }

    /**
     * 兼容全主流厂商的锁屏节点解析器 (MIUI/HyperOS, ColorOS, OriginOS, EMUI/MagicOS, OneUI, AOSP)
     */
    private fun findKeyNodeInTree(node: AccessibilityNodeInfo?, targetChar: Char, screenWidth: Int, screenHeight: Int): AccessibilityNodeInfo? {
        if (node == null) return null
        val viewId = (node.viewIdResourceName ?: "").lowercase()
        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""

        // 严格边界过滤：数字键盘必定在屏幕中下部 (45% ~ 95% 屏高)，杜绝匹配顶部时钟/电量/通知等带 0 元素
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val isInKeypadArea = rect.centerY() > screenHeight * 0.45f &&
                rect.centerY() < screenHeight * 0.95f &&
                rect.width() in 40..600 &&
                rect.height() in 40..600

        if (isInKeypadArea) {
            // 针对 0 键专项校验：0 键必定位于屏幕底部 (70% ~ 95% 屏高) 且水平居中 (30% ~ 70% 屏宽)
            if (targetChar == '0') {
                val isZeroPosition = rect.centerX() > screenWidth * 0.30f &&
                        rect.centerX() < screenWidth * 0.70f &&
                        rect.centerY() > screenHeight * 0.70f

                val isZeroMatch = viewId.endsWith("key0") ||
                        viewId.endsWith("key_0") ||
                        viewId.endsWith("num_0") ||
                        viewId.endsWith("num0") ||
                        viewId.endsWith("digit_0") ||
                        viewId.endsWith("digit0") ||
                        viewId.endsWith("numeric_key_0") ||
                        viewId.endsWith("pad_0") ||
                        desc == "0" || desc == "数字0" || desc == "数字 0" || desc.startsWith("0,") || desc.startsWith("0 ") || desc == "零" ||
                        text == "0"

                if (isZeroPosition && isZeroMatch) {
                    return node
                }
            } else {
                // 1..9 按键匹配
                val isKeyViewId = viewId.endsWith("key$targetChar") ||
                        viewId.endsWith("key_$targetChar") ||
                        viewId.endsWith("num_$targetChar") ||
                        viewId.endsWith("num$targetChar") ||
                        viewId.endsWith("digit_$targetChar") ||
                        viewId.endsWith("digit$targetChar") ||
                        viewId.endsWith("numeric_key_$targetChar") ||
                        viewId.endsWith("pad_$targetChar")

                val isKeyContent = desc == targetChar.toString() ||
                        desc == "数字$targetChar" ||
                        desc == "数字 $targetChar" ||
                        desc.startsWith("$targetChar,") ||
                        desc.startsWith("$targetChar ") ||
                        text == targetChar.toString()

                if (isKeyViewId || isKeyContent) {
                    return node
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findKeyNodeInTree(child, targetChar, screenWidth, screenHeight)
            if (found != null) return found
        }
        return null
    }

    /**
     * 智能检测屏幕上是否存在明确的“确定/完成/Enter”按键 (严防误判为删除键或拨号键)
     */
    private fun findExplicitEnterNode(service: AccessibilityService, screenHeight: Int): AccessibilityNodeInfo? {
        try {
            val root = service.rootInActiveWindow
            if (root != null) {
                val node = searchEnterNode(root, screenHeight)
                if (node != null) return node
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (window in service.windows) {
                    val winRoot = window.root ?: continue
                    val node = searchEnterNode(winRoot, screenHeight)
                    if (node != null) return node
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search enter node warning: ${e.message}")
        }
        return null
    }

    private fun searchEnterNode(node: AccessibilityNodeInfo?, screenHeight: Int): AccessibilityNodeInfo? {
        if (node == null) return null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.centerY() > screenHeight * 0.50f) {
            val viewId = (node.viewIdResourceName ?: "").lowercase()
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""

            val isEnter = viewId.contains("enter") || viewId.contains("key_enter") || viewId.contains("confirm") ||
                    text == "确定" || text == "完成" || text == "OK" || text == "✓" ||
                    desc == "确定" || desc == "完成" || desc.contains("确认")

            val isDeleteOrCancel = viewId.contains("delete") || viewId.contains("backspace") ||
                    text.contains("删除") || text.contains("取消") || text.contains("紧急") ||
                    desc.contains("删除") || desc.contains("退格")

            if (isEnter && !isDeleteOrCancel) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchEnterNode(child, screenHeight)
            if (found != null) return found
        }
        return null
    }

    /**
     * 全机型一站式全自动亮屏 + 智能破阻尼上滑 + 自适应 PIN 码解锁
     */
    suspend fun wakeAndUnlockWithAccessibility(
        context: Context,
        service: AccessibilityService,
        pin: String
    ) {
        SignAccessibilityService.log("💡 [多机型解锁 1/4] 唤醒并点亮屏幕...")
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val brightLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "s1nger:MultiBrandWakeLock"
            )
            brightLock.acquire(15000L)
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock warning: ${e.message}")
        }

        // 等待屏幕背光与系统准备
        delay(700)

        val dm = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        Log.d(TAG, "Screen resolution: ${width}x${height}")

        // 第一次长距超速弹射上滑 (跨越 88% -> 8% 屏高)
        SignAccessibilityService.log("👆 [多机型解锁 2/4] 执行破阻尼长距上滑呼出 PIN 键盘...")
        dispatchSwipeUpAsync(service, width, height)

        // 智能轮询监听系统键盘完全就绪状态 (自适应慢速/动效丰富的定制 ROM)
        var keypadDetected = false
        for (attempt in 1..8) {
            delay(180)
            val root = service.rootInActiveWindow
            val sampleNode = findKeyNodeInTree(root, '1', width, height)
                ?: findKeyNodeInTree(root, '5', width, height)
                ?: findKeyNodeInTree(root, '0', width, height)

            if (sampleNode != null) {
                keypadDetected = true
                SignAccessibilityService.log("🎯 探测到系统 PIN 键盘已展开就绪 (耗时 ${attempt * 180}ms)")
                break
            }

            // 如果等待 540ms (attempt=3) 键盘未响应（针对 MIUI / ColorOS 的防误触阻尼），补发一次强力上滑
            if (attempt == 3) {
                SignAccessibilityService.log("🔄 触发二次强力上滑保底补偿...")
                dispatchSwipeUpAsync(service, width, height)
            }
        }

        if (!keypadDetected) {
            SignAccessibilityService.log("⏳ 锁屏转场已静止，开始按键输入流程...")
            delay(500)
        } else {
            delay(200)
        }

        if (pin.isNotBlank()) {
            SignAccessibilityService.log("🔢 [多机型解锁 3/4] 开始自适应输入 PIN 码 (共 ${pin.length} 位)...")

            for ((index, char) in pin.withIndex()) {
                val (targetX, targetY) = findPinKeyCoordinate(service, char, width, height)

                SignAccessibilityService.log("👉 [${index + 1}/${pin.length}] 输入数字 [$char] -> 坐标 (${targetX.toInt()}, ${targetY.toInt()})")
                dispatchClickAsync(service, targetX, targetY, durationMs = 90L)

                // 预留 350ms 完成按键水波纹与系统防抖
                delay(350)
            }

            // 只有当屏幕上存在明确的“确定/完成/Enter”按键节点时才点击确认（绝大多数系统 4/6 位 PIN 输入完毕自动过锁屏，盲点必会导致误触 9 键或删除键）
            val enterNode = findExplicitEnterNode(service, height)
            if (enterNode != null) {
                val rect = Rect()
                enterNode.getBoundsInScreen(rect)
                SignAccessibilityService.log("🔓 [多机型解锁 4/4] 探测到确认按键，点击完成解锁 -> (${rect.centerX()}, ${rect.centerY()})")
                dispatchClickAsync(service, rect.centerX().toFloat(), rect.centerY().toFloat(), durationMs = 90L)
            } else {
                SignAccessibilityService.log("🔓 [多机型解锁 4/4] PIN 码已输入完毕，等待系统自动校验过锁屏...")
            }
            delay(800)
        } else {
            SignAccessibilityService.log("🔓 [多机型解锁 3/3] 免密滑动解锁完毕")
            delay(500)
        }
    }

    /**
     * 免 Root 自动息屏锁屏 (Android 9.0+ 原生 API)
     */
    fun lockScreen(service: AccessibilityService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            Log.d(TAG, "performGlobalAction GLOBAL_ACTION_LOCK_SCREEN, success=$success")
        } else {
            Log.w(TAG, "当前 Android 版本低于 9.0，无法调用原生无障碍锁屏 API")
        }
    }

    /**
     * 免 Root 自动截图 (Android 9.0+ 原生 API)
     */
    fun takeScreenshot(service: AccessibilityService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            Log.d(TAG, "performGlobalAction GLOBAL_ACTION_TAKE_SCREENSHOT, success=$success")
        }
    }
}
