package com.autosign.cpdaily

import android.app.KeyguardManager
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.autosign.cpdaily.model.WorkMode
import com.autosign.cpdaily.service.SignAccessibilityService
import com.autosign.cpdaily.ui.components.ChoiceItem
import com.autosign.cpdaily.ui.components.ChoiceOption
import com.autosign.cpdaily.ui.components.HeroCardState
import com.autosign.cpdaily.ui.components.HeroStatusCard
import com.autosign.cpdaily.ui.components.ListItem
import com.autosign.cpdaily.ui.components.NavItem
import com.autosign.cpdaily.ui.components.PixelSingleChoiceDialog
import com.autosign.cpdaily.ui.components.SectionHeader
import com.autosign.cpdaily.ui.components.SegmentedGroup
import com.autosign.cpdaily.ui.components.SwitchItem
import com.autosign.cpdaily.ui.icons.PixelIcons
import com.autosign.cpdaily.ui.theme.PixelExpressiveTheme
import com.autosign.cpdaily.ui.theme.pixelCardBackgroundColor
import com.autosign.cpdaily.util.AdbUtil
import com.autosign.cpdaily.util.AlarmScheduler
import com.autosign.cpdaily.util.LogRepository
import com.autosign.cpdaily.util.NonRootUtil
import com.autosign.cpdaily.util.PrefsUtil
import com.autosign.cpdaily.util.RootUtil
import com.autosign.cpdaily.util.ScreenRecorder
import com.autosign.cpdaily.util.SprintSignEngine
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var testJob: Job? = null
    private var requestedTabState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntentExtra(intent)

        setContent {
            PixelExpressiveTheme {
                MainAppScreen(
                    externalTabState = requestedTabState.intValue,
                    onExternalTabConsumed = { requestedTabState.intValue = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtra(intent)
    }

    private fun handleIntentExtra(intent: Intent?) {
        val targetTab = intent?.getStringExtra("extra_tab")
        if (targetTab == "test") {
            requestedTabState.intValue = 1
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainAppScreen(
        externalTabState: Int,
        onExternalTabConsumed: (Int) -> Unit
    ) {
        var selectedTab by remember { mutableIntStateOf(externalTabState) }
        LaunchedEffect(externalTabState) {
            if (selectedTab != externalTabState) {
                selectedTab = externalTabState
            }
        }

        // 核心业务状态
        var workMode by remember { mutableStateOf(PrefsUtil.getWorkMode(this)) }
        var alarmEnabled by remember { mutableStateOf(PrefsUtil.isAlarmEnabled(this)) }
        var signHour by remember { mutableIntStateOf(PrefsUtil.getSignHour(this)) }
        var signMinute by remember { mutableIntStateOf(PrefsUtil.getSignMinute(this)) }
        var signStartHour by remember { mutableIntStateOf(PrefsUtil.getSignStartHour(this)) }
        var signStartMinute by remember { mutableIntStateOf(PrefsUtil.getSignStartMinute(this)) }
        var pinCode by remember { mutableStateOf(PrefsUtil.getPin(this)) }
        var maxRetries by remember { mutableIntStateOf(PrefsUtil.getMaxRetries(this)) }
        var recordScreenEnabled by remember { mutableStateOf(PrefsUtil.isRecordScreenEnabled(this)) }
        var appLang by remember { mutableStateOf(PrefsUtil.getAppLanguage(this)) }
        var signedDays by remember { mutableIntStateOf(PrefsUtil.getSignCount(this)) }
        var isExpired by remember { mutableStateOf(PrefsUtil.isPreviewExpired(this)) }
        var isA11yActive by remember { mutableStateOf(checkAccessibilityActiveDirect(this)) }
        var hasRoot by remember { mutableStateOf(false) }
        var hasShizuku by remember { mutableStateOf(false) }
        var isShizukuGranted by remember { mutableStateOf(false) }
        var isTaskRunning by remember { mutableStateOf(SignAccessibilityService.isTaskRunning) }
        var deviceName by remember { mutableStateOf("${Build.MANUFACTURER} ${Build.MODEL}") }

        // 弹窗控制状态
        var showFirstLangDialog by remember { mutableStateOf(!PrefsUtil.isLanguageChosen(this)) }
        var showAgreementDialog by remember {
            mutableStateOf(PrefsUtil.isLanguageChosen(this) && !PrefsUtil.isAgreementAccepted(this))
        }
        var showA11yGuideDialog by remember { mutableStateOf(false) }
        var showPinDialog by remember { mutableStateOf(false) }
        var showMissingPinPrompt by remember { mutableStateOf(false) }
        var showExpiredDialog by remember { mutableStateOf(false) }

        // 日志状态
        val logLines = remember {
            mutableStateListOf<String>().apply {
                addAll(LogRepository.getAllLogs())
            }
        }

        DisposableEffect(Unit) {
            val listener: (String) -> Unit = { line ->
                runOnUiThread {
                    logLines.add(line)
                    isTaskRunning = SignAccessibilityService.isTaskRunning
                }
            }
            LogRepository.uiListener = listener
            onDispose {
                if (LogRepository.uiListener === listener) {
                    LogRepository.uiListener = null
                }
            }
        }

        // 刷新系统与权限状态函数
        fun refreshSystemState(promptA11yIfMissing: Boolean = false) {
            signedDays = PrefsUtil.getSignCount(this)
            isExpired = PrefsUtil.isPreviewExpired(this)
            isTaskRunning = SignAccessibilityService.isTaskRunning
            isA11yActive = checkAccessibilityActiveDirect(this)
            lifecycleScope.launch(Dispatchers.IO) {
                val rootAvail = RootUtil.isRootAvailable()
                val shizukuAvail = AdbUtil.isShizukuAvailable()
                val shizukuGranted = AdbUtil.isShizukuPermissionGranted()
                if (rootAvail && !checkAccessibilityActiveDirect(this@MainActivity)) {
                    RootUtil.enableAccessibilityService()
                    delay(500)
                } else if (!rootAvail && shizukuGranted && !checkAccessibilityActiveDirect(this@MainActivity)) {
                    AdbUtil.enableAccessibilityService()
                    delay(500)
                }
                val a11yNow = checkAccessibilityActiveDirect(this@MainActivity)
                val customName = getCustomDeviceName()
                withContext(Dispatchers.Main) {
                    hasRoot = rootAvail
                    hasShizuku = shizukuAvail
                    isShizukuGranted = shizukuGranted
                    isA11yActive = a11yNow
                    deviceName = customName
                    if (!a11yNow && promptA11yIfMissing) {
                        showA11yGuideDialog = true
                    }
                }
            }
        }

        // 监听生命周期回到前台时自动刷新状态
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshSystemState(promptA11yIfMissing = false)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // 首次启动且已同意协议时检测环境
        LaunchedEffect(Unit) {
            if (PrefsUtil.isLanguageChosen(this@MainActivity) && PrefsUtil.isAgreementAccepted(this@MainActivity)) {
                refreshSystemState(promptA11yIfMissing = true)
            } else {
                refreshSystemState(promptA11yIfMissing = false)
            }
        }

        // 计算显示时间字符串
        val displayAlarmTime = remember(workMode, signHour, signMinute, signStartHour, signStartMinute) {
            if (workMode.isSprint) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, signStartHour)
                    set(Calendar.MINUTE, signStartMinute)
                    set(Calendar.SECOND, 0)
                    add(Calendar.SECOND, -20)
                }
                String.format(
                    "%02d:%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND)
                )
            } else {
                String.format("%02d:%02d", signHour, signMinute)
            }
        }
        val displayStartTime = remember(signStartHour, signStartMinute) {
            String.format("%02d:%02d", signStartHour, signStartMinute)
        }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        val topBarTitle = when (selectedTab) {
            0 -> stringResource(R.string.app_name)
            1 -> stringResource(R.string.nav_test)
            else -> stringResource(R.string.nav_settings)
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.heightIn(min = 80.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            onExternalTabConsumed(0)
                            refreshSystemState(false)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) PixelIcons.HomeFilled else PixelIcons.Home,
                                contentDescription = stringResource(R.string.nav_home)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home)) },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            onExternalTabConsumed(1)
                        },
                        icon = {
                            Icon(
                                imageVector = PixelIcons.Science,
                                contentDescription = stringResource(R.string.nav_test)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_test)) },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            onExternalTabConsumed(2)
                        },
                        icon = {
                            Icon(
                                imageVector = PixelIcons.Settings,
                                contentDescription = stringResource(R.string.nav_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        colors = navItemColors
                    )
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> HomeTabContent(
                    contentPadding = innerPadding,
                    workMode = workMode,
                    alarmEnabled = alarmEnabled,
                    displayAlarmTime = displayAlarmTime,
                    signedDays = signedDays,
                    isExpired = isExpired,
                    isTaskRunning = isTaskRunning,
                    isA11yActive = isA11yActive,
                    hasRoot = hasRoot,
                    hasShizuku = hasShizuku,
                    isShizukuGranted = isShizukuGranted,
                    deviceName = deviceName,
                    onHeroClick = {
                        selectedTab = 2
                        onExternalTabConsumed(2)
                    },
                    onA11yClick = {
                        if (!checkAccessibilityActiveDirect(this@MainActivity)) {
                            showA11yGuideDialog = true
                        } else {
                            Toast.makeText(this@MainActivity, "✅ 无障碍辅助服务正在正常运行中", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShizukuClick = {
                        if (hasShizuku && !isShizukuGranted) {
                            AdbUtil.requestShizukuPermission()
                        } else {
                            refreshSystemState(false)
                        }
                    },
                    onRefreshClick = {
                        refreshSystemState(promptA11yIfMissing = true)
                        Toast.makeText(this@MainActivity, "🔄 系统权限与状态已刷新", Toast.LENGTH_SHORT).show()
                    },
                    onJumpSettings = {
                        selectedTab = 2
                        onExternalTabConsumed(2)
                    }
                )

                1 -> TestTabContent(
                    contentPadding = innerPadding,
                    workMode = workMode,
                    logLines = logLines,
                    onTestNow = {
                        if (PrefsUtil.isPreviewExpired(this@MainActivity)) {
                            showExpiredDialog = true
                            return@TestTabContent
                        }
                        appendLog("🚀 开始立即测试当前模式【${workMode.getTitle(this@MainActivity)}】完整打卡流程...")
                        isTaskRunning = true
                        testJob?.cancel()
                        testJob = lifecycleScope.launch(Dispatchers.IO) {
                            if (workMode.isSprint) {
                                SprintSignEngine.executeSprint(this@MainActivity, isSimulation = true)
                            } else if (workMode.isSkz) {
                                appendLog("🚀 [SKZ 模式] 验证 Shizuku 调试桥环境...")
                                if (!AdbUtil.isShizukuAvailable()) {
                                    appendLog("❌ Shizuku 服务未运行！请先打开 Shizuku 应用启动服务。")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@MainActivity, "Shizuku 服务未运行", Toast.LENGTH_LONG).show()
                                    }
                                    isTaskRunning = false
                                    return@launch
                                }
                                if (!AdbUtil.isShizukuPermissionGranted()) {
                                    appendLog("⚠️ Shizuku 未授权，正在发起授权请求...")
                                    AdbUtil.requestShizukuPermission()
                                    delay(1500)
                                }
                                val idResult = AdbUtil.execCommand("id").trim()
                                appendLog("✅ Shizuku 验证通过！Shell 身份: $idResult")
                                if (!checkAccessibilityActiveDirect(this@MainActivity)) {
                                    appendLog("🔄 正在通过 Shizuku 自动激活无障碍服务...")
                                    AdbUtil.enableAccessibilityService()
                                    delay(800)
                                }
                                appendLog("🚀 正在通过 ADB Shell 调起今日校园...")
                                AdbUtil.launchCpdaily(this@MainActivity)
                                delay(600)
                                if (!isActive) return@launch
                                SignAccessibilityService.startTask()
                            } else {
                                if (!checkAccessibilityActiveDirect(this@MainActivity) && workMode.isRoot) {
                                    RootUtil.enableAccessibilityService()
                                    delay(800)
                                }
                                if (!isActive) return@launch
                                RootUtil.launchCpdaily(this@MainActivity)
                                delay(500)
                                if (!isActive) return@launch
                                SignAccessibilityService.startTask()
                            }
                        }
                    },
                    onTestScreenOff = {
                        if (PrefsUtil.isPreviewExpired(this@MainActivity)) {
                            showExpiredDialog = true
                            return@TestTabContent
                        }
                        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        if (pinCode.isBlank() && km.isDeviceSecure) {
                            showMissingPinPrompt = true
                        } else {
                            startUnlockTest(workMode, pinCode)
                        }
                    },
                    onForceStop = {
                        testJob?.cancel()
                        testJob = null
                        SignAccessibilityService.resetTask()
                        isTaskRunning = false
                        lifecycleScope.launch(Dispatchers.IO) {
                            RootUtil.forceStopCpdaily()
                            withContext(Dispatchers.Main) {
                                refreshSystemState(false)
                            }
                        }
                        appendLog("🛑 已平稳停止所有打卡与测试任务！")
                        Toast.makeText(this@MainActivity, "已停止任务", Toast.LENGTH_SHORT).show()
                    },
                    onCopyLogs = {
                        val report = LogRepository.generateDiagnosticReport(this@MainActivity)
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("s1ngerAutoSign_logs", report))
                        Toast.makeText(this@MainActivity, "📋 诊断日志已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                        appendLog("📋 诊断日志已复制到系统剪贴板")
                    },
                    onExportTxt = {
                        try {
                            val txtFile = LogRepository.exportLogToTxtFile(this@MainActivity)
                            val uri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                txtFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "s1ngerAutoSign 诊断日志文件")
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(shareIntent, "分享 s1ngerAutoSign 日志 TXT 文件"))
                            Toast.makeText(this@MainActivity, "📁 诊断日志 TXT 文件已生成并唤起分享！", Toast.LENGTH_SHORT).show()
                            appendLog("📁 诊断日志已成功导出为独立 TXT 文件：${txtFile.name}")
                        } catch (e: Exception) {
                            appendLog("❌ 导出 TXT 文件失败: ${e.message}")
                            Toast.makeText(this@MainActivity, "导出 TXT 失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearLogs = {
                        logLines.clear()
                        Toast.makeText(this@MainActivity, "🧹 已清屏（诊断日志仍安全保存在队列中）", Toast.LENGTH_SHORT).show()
                    },
                    onOpenLatestVideo = {
                        ScreenRecorder.openLatestRecording(this@MainActivity)
                    }
                )

                2 -> SettingsTabContent(
                    contentPadding = innerPadding,
                    workMode = workMode,
                    alarmEnabled = alarmEnabled,
                    recordScreenEnabled = recordScreenEnabled,
                    displayStartTime = displayStartTime,
                    displayAlarmTime = displayAlarmTime,
                    pinCode = pinCode,
                    maxRetries = maxRetries,
                    appLang = appLang,
                    onRecordScreenToggle = { enabled ->
                        recordScreenEnabled = enabled
                        PrefsUtil.setRecordScreenEnabled(this@MainActivity, enabled)
                        if (enabled) {
                            appendLog("🎥 已开启自动打卡录屏功能")
                            Toast.makeText(this@MainActivity, "自动打卡录屏已开启", Toast.LENGTH_SHORT).show()
                        } else {
                            appendLog("⏹ 已停用自动打卡录屏功能")
                            Toast.makeText(this@MainActivity, "自动打卡录屏已停用", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onWorkModeChange = { newMode ->
                        workMode = newMode
                        PrefsUtil.saveWorkMode(this@MainActivity, newMode)
                        appendLog("⚙️ 已切换运行模式为：${newMode.getTitle(this@MainActivity)}")
                        if (alarmEnabled) {
                            AlarmScheduler.scheduleDailyAlarm(this@MainActivity)
                        }
                    },
                    onAlarmToggle = { enabled ->
                        if (enabled && PrefsUtil.isPreviewExpired(this@MainActivity)) {
                            showExpiredDialog = true
                            return@SettingsTabContent
                        }
                        alarmEnabled = enabled
                        PrefsUtil.setAlarmEnabled(this@MainActivity, enabled)
                        if (enabled) {
                            AlarmScheduler.scheduleDailyAlarm(this@MainActivity)
                            appendLog("⏰ 已开启每日定时打卡")
                            Toast.makeText(this@MainActivity, "定时打卡已开启", Toast.LENGTH_SHORT).show()
                        } else {
                            AlarmScheduler.cancelDailyAlarm(this@MainActivity)
                            appendLog("⏹ 已停用每日定时打卡")
                            Toast.makeText(this@MainActivity, "定时打卡已停用", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPickStartTime = {
                        TimePickerDialog(
                            this@MainActivity,
                            { _, hourOfDay, minute ->
                                signStartHour = hourOfDay
                                signStartMinute = minute
                                PrefsUtil.saveSignStartTime(this@MainActivity, hourOfDay, minute)
                                if (alarmEnabled) {
                                    AlarmScheduler.scheduleDailyAlarm(this@MainActivity)
                                    appendLog(
                                        "⏰ 已更新签到开始时间为 ${
                                            String.format("%02d:%02d", hourOfDay, minute)
                                        } (极速模式将提前20秒预热)"
                                    )
                                }
                            },
                            signStartHour,
                            signStartMinute,
                            true
                        ).show()
                    },
                    onPickSignTime = {
                        TimePickerDialog(
                            this@MainActivity,
                            { _, hourOfDay, minute ->
                                signHour = hourOfDay
                                signMinute = minute
                                PrefsUtil.saveSignTime(this@MainActivity, hourOfDay, minute)
                                if (alarmEnabled) {
                                    AlarmScheduler.scheduleDailyAlarm(this@MainActivity)
                                    val timeStr = String.format("%02d:%02d", hourOfDay, minute)
                                    appendLog("⏰ 已重新设定打卡定时为每日 $timeStr")
                                }
                            },
                            signHour,
                            signMinute,
                            true
                        ).show()
                    },
                    onOpenPinDialog = { showPinDialog = true },
                    onMaxRetriesChange = { count ->
                        maxRetries = count
                        PrefsUtil.saveMaxRetries(this@MainActivity, count)
                        appendLog("⚙️ 已将打卡异常重试上限设定为 $count 次")
                    },
                    onOpenKeepAlive = {
                        startActivity(Intent(this@MainActivity, KeepAliveActivity::class.java))
                    },
                    onLanguageChange = { newLang ->
                        appLang = newLang
                        PrefsUtil.saveAppLanguage(this@MainActivity, newLang)
                        PrefsUtil.setLanguageChosen(this@MainActivity, true)
                        applyLanguage(newLang)
                    }
                )
            }
        }

        // 1. 首次启动语言选择弹窗
        if (showFirstLangDialog) {
            PixelSingleChoiceDialog(
                title = "欢迎 / Welcome",
                options = listOf(
                    ChoiceOption("zh", "简体中文 (Simplified Chinese)", "使用简体中文界面"),
                    ChoiceOption("en", "English", "Use English interface")
                ),
                selectedValue = appLang,
                cancelable = false,
                cancelText = null,
                onOptionSelected = { chosenLang ->
                    appLang = chosenLang
                    PrefsUtil.saveAppLanguage(this@MainActivity, chosenLang)
                    PrefsUtil.setLanguageChosen(this@MainActivity, true)
                    showFirstLangDialog = false
                    applyLanguage(chosenLang)
                    if (!PrefsUtil.isAgreementAccepted(this@MainActivity)) {
                        showAgreementDialog = true
                    }
                },
                onDismiss = {}
            )
        }

        // 2. 用户使用协议与免责声明弹窗 (28.dp 超大圆角)
        if (showAgreementDialog) {
            AgreementAlertDialog(
                onAgree = {
                    PrefsUtil.setAgreementAccepted(this@MainActivity, true)
                    showAgreementDialog = false
                    appendLog("✅ 用户已阅读并同意《用户使用协议与免责声明》")
                    refreshSystemState(promptA11yIfMissing = true)
                },
                onDisagree = {
                    showAgreementDialog = false
                    finishAffinity()
                }
            )
        }

        // 3. 无障碍开启引导弹窗 (28.dp 超大圆角)
        if (showA11yGuideDialog) {
            AccessibilityGuideAlertDialog(
                onGoSettings = {
                    showA11yGuideDialog = false
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "无法自动打开设置页，请手动在系统设置中开启无障碍",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDismiss = { showA11yGuideDialog = false }
            )
        }

        // 4. 锁屏 PIN 码录入弹窗 (28.dp 超大圆角)
        if (showPinDialog) {
            PinConfigAlertDialog(
                initialPin = pinCode,
                onSave = { newPin ->
                    pinCode = newPin
                    PrefsUtil.savePin(this@MainActivity, newPin)
                    showPinDialog = false
                    appendLog("💾 锁屏数字 PIN 码已更新并加密保存")
                    Toast.makeText(this@MainActivity, "✅ PIN 码已保存", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showPinDialog = false }
            )
        }

        // 5. 息屏测试前未配置 PIN 码提醒弹窗
        if (showMissingPinPrompt) {
            val isEn = appLang == "en"
            AlertDialog(
                onDismissRequest = { showMissingPinPrompt = false },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                title = {
                    Text(
                        text = if (isEn) "Lockscreen PIN Required" else "未填写锁屏 PIN 码",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                text = {
                    Text(
                        text = if (isEn) {
                            "Your device has a secure lockscreen, but no PIN is configured in AutoSign.\n\nPlease set your lockscreen PIN in Settings to ensure automatic unlocking."
                        } else {
                            "检测到当前设备已设置锁屏密码，但尚未在应用内填写锁屏 PIN 码。\n\n为确保息屏测试能够全自动唤醒并解锁，建议前往填写；若为免密锁屏可直接继续。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showMissingPinPrompt = false
                            selectedTab = 2
                            onExternalTabConsumed(2)
                            showPinDialog = true
                        }
                    ) {
                        Text(if (isEn) "Set PIN Now" else "前往填写")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showMissingPinPrompt = false
                            startUnlockTest(workMode, "")
                        }
                    ) {
                        Text(if (isEn) "Continue Anyway" else "仍要免密测试")
                    }
                }
            )
        }

        // 6. 预览版到期提示弹窗
        if (showExpiredDialog) {
            AlertDialog(
                onDismissRequest = { showExpiredDialog = false },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                title = {
                    Text(
                        text = "请更新正式版",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                text = {
                    Text(
                        text = "已签到 5 天，预览版体验已结束。请更新正式版以继续享受每日全自动无忧打卡服务！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showExpiredDialog = false }) {
                        Text("我知道了")
                    }
                }
            )
        }
    }

    @Composable
    private fun HomeTabContent(
        contentPadding: PaddingValues,
        workMode: WorkMode,
        alarmEnabled: Boolean,
        displayAlarmTime: String,
        signedDays: Int,
        isExpired: Boolean,
        isTaskRunning: Boolean,
        isA11yActive: Boolean,
        hasRoot: Boolean,
        hasShizuku: Boolean,
        isShizukuGranted: Boolean,
        deviceName: String,
        onHeroClick: () -> Unit,
        onA11yClick: () -> Unit,
        onShizukuClick: () -> Unit,
        onRefreshClick: () -> Unit,
        onJumpSettings: () -> Unit
    ) {
        val heroState = when {
            isExpired -> HeroCardState.ERROR
            isTaskRunning || (alarmEnabled && (isA11yActive || workMode.isRoot)) -> HeroCardState.ACTIVE
            else -> HeroCardState.INACTIVE
        }

        val heroTitle = when {
            isExpired -> stringResource(R.string.status_expired)
            isTaskRunning -> stringResource(R.string.status_running)
            alarmEnabled && (isA11yActive || workMode.isRoot) -> stringResource(R.string.status_working)
            else -> stringResource(R.string.status_idle)
        }

        val heroSubtitle = when {
            isExpired -> "预览体验期已满，请更新正式版继续使用"
            isTaskRunning -> "正在自动化执行【${workMode.getTitle(this)}】签到任务"
            alarmEnabled -> "${workMode.getTitle(this)} · 每日 $displayAlarmTime 自动唤醒执行"
            else -> "${workMode.getTitle(this)} · 点击前往设置开启每日定时打卡"
        }

        val heroIcon = when {
            isExpired -> PixelIcons.ErrorOutline
            isTaskRunning || workMode.isSprint -> PixelIcons.Bolt
            heroState == HeroCardState.ACTIVE -> PixelIcons.CheckCircle
            else -> PixelIcons.Schedule
        }

        val modeShortBadge = when {
            workMode.isSprint -> stringResource(R.string.mode_sprint)
            workMode.isRoot -> stringResource(R.string.mode_root)
            workMode.isSkz -> stringResource(R.string.mode_skz)
            else -> stringResource(R.string.mode_a11y)
        }

        val badges = listOf(
            "v${BuildConfig.VERSION_NAME}",
            modeShortBadge,
            if (alarmEnabled) "定时 $displayAlarmTime" else stringResource(R.string.status_idle),
            "已签 " + stringResource(R.string.days_unit, signedDays)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HeroStatusCard(
                    state = heroState,
                    title = heroTitle,
                    subtitle = heroSubtitle,
                    icon = heroIcon,
                    badges = badges,
                    onClick = onHeroClick
                )
            }

            item {
                SegmentedGroup(title = "权限与运行服务") {
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.a11y_status_label),
                            subtitle = if (isA11yActive) {
                                "辅助服务已连接，可自动识别今日校园界面并完成点击"
                            } else {
                                "未开启，点击前往系统设置开启无障碍服务"
                            },
                            icon = PixelIcons.AccessibilityNew,
                            valueText = if (isA11yActive) {
                                stringResource(R.string.status_enabled)
                            } else {
                                stringResource(R.string.status_disabled)
                            },
                            valueColor = if (isA11yActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            onClick = onA11yClick
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.root_status_label),
                            subtitle = if (hasRoot) {
                                "已获取 Superuser 硬件级亮屏、指令输入与极速拉起权限"
                            } else {
                                "未授权 Root，可使用无障碍模式完成日常打卡"
                            },
                            icon = PixelIcons.Security,
                            valueText = if (hasRoot) {
                                stringResource(R.string.status_granted)
                            } else {
                                stringResource(R.string.status_denied)
                            },
                            valueColor = if (hasRoot) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            onClick = onRefreshClick
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = "Shizuku 权限",
                            subtitle = when {
                                isShizukuGranted -> "Shizuku 调试桥已连接，已获得 ADB Shell 级自动化调度权限"
                                hasShizuku -> "Shizuku 运行中但尚未向本应用授权，点击向系统请求授权"
                                else -> "未检测到 Shizuku 服务运行，请打开 Shizuku 应用启动服务"
                            },
                            icon = PixelIcons.Terminal,
                            valueText = when {
                                isShizukuGranted -> stringResource(R.string.status_granted)
                                hasShizuku -> "去授权"
                                else -> "未运行"
                            },
                            valueColor = when {
                                isShizukuGranted -> MaterialTheme.colorScheme.primary
                                hasShizuku -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            onClick = onShizukuClick
                        )
                    }
                }
            }

            item {
                SegmentedGroup(title = "调度与设备环境") {
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.work_mode_label),
                            subtitle = workMode.getTitle(this@MainActivity),
                            icon = PixelIcons.Tune,
                            valueText = modeShortBadge,
                            valueColor = MaterialTheme.colorScheme.primary,
                            onClick = onJumpSettings
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.card_alarm_time),
                            subtitle = if (alarmEnabled) "每日定时自动唤醒" else "定时开关未启用",
                            icon = PixelIcons.Schedule,
                            valueText = displayAlarmTime,
                            valueColor = if (alarmEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            onClick = onJumpSettings
                        )
                    }
                    item { index, count ->
                        ListItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.app_version_label),
                            subtitle = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            icon = PixelIcons.Info,
                            onClick = onRefreshClick
                        )
                    }
                    item { index, count ->
                        ListItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.device_model_label),
                            subtitle = deviceName,
                            icon = PixelIcons.Smartphone
                        )
                    }
                    item { index, count ->
                        ListItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.system_version_label),
                            subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            icon = PixelIcons.Android
                        )
                    }
                    item { index, count ->
                        ListItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.fingerprint_label),
                            subtitle = Build.FINGERPRINT,
                            icon = PixelIcons.Fingerprint
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TestTabContent(
        contentPadding: PaddingValues,
        workMode: WorkMode,
        logLines: List<String>,
        onTestNow: () -> Unit,
        onTestScreenOff: () -> Unit,
        onForceStop: () -> Unit,
        onCopyLogs: () -> Unit,
        onExportTxt: () -> Unit,
        onClearLogs: () -> Unit,
        onOpenLatestVideo: () -> Unit
    ) {
        val logScrollState = rememberScrollState()
        LaunchedEffect(logLines.size) {
            logScrollState.animateScrollTo(logScrollState.maxValue)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SegmentedGroup(title = "全流程自动化演练") {
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.test_now),
                            subtitle = "亮屏状态下立即执行【${workMode.getTitle(this@MainActivity)}】完整打卡流程",
                            icon = PixelIcons.RocketLaunch,
                            onClick = onTestNow
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.test_screen_off),
                            subtitle = "3 秒后自动锁屏，测试息屏唤醒、密码解锁与自动签到全链路",
                            icon = PixelIcons.ScreenLockPortrait,
                            onClick = onTestScreenOff
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.force_stop_task),
                            subtitle = "平稳终止正在运行的测试任务并重置无障碍服务状态",
                            icon = PixelIcons.StopCircle,
                            valueText = "停止",
                            valueColor = MaterialTheme.colorScheme.error,
                            onClick = onForceStop
                        )
                    }
                }
            }

            item {
                SegmentedGroup(title = "诊断报告与日志工具") {
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.log_copy),
                            subtitle = "复制完整诊断报告到系统剪贴板",
                            icon = PixelIcons.ContentCopy,
                            onClick = onCopyLogs
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.log_export),
                            subtitle = "生成独立 TXT 诊断文件并调用系统分享",
                            icon = PixelIcons.Share,
                            onClick = onExportTxt
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.log_clear),
                            subtitle = "清空下方终端窗口显示（后台日志队列仍安全保留）",
                            icon = PixelIcons.DeleteSweep,
                            onClick = onClearLogs
                        )
                    }
                    item { index, count ->
                        val latestVideo = ScreenRecorder.getLatestRecording()
                        val videoSubtitle = if (latestVideo != null && latestVideo.exists()) {
                            "已归档: ${latestVideo.name} (${latestVideo.length() / 1024} KB)，供诊断调阅"
                        } else {
                            "每次打卡自动在后台静默录制，供排查复盘界面卡点"
                        }
                        NavItem(
                            index = index,
                            count = count,
                            title = "打卡全流程录像 (诊断专用)",
                            subtitle = videoSubtitle,
                            icon = PixelIcons.Videocam,
                            onClick = onOpenLatestVideo
                        )
                    }
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.log_terminal_title))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = MaterialTheme.shapes.large, // 20.dp
                    color = pixelCardBackgroundColor(),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    val textContent = if (logLines.isEmpty()) {
                        "等待任务执行，实时日志将在此处输出..."
                    } else {
                        logLines.joinToString("\n")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(logScrollState)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = textContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = if (logLines.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsTabContent(
        contentPadding: PaddingValues,
        workMode: WorkMode,
        alarmEnabled: Boolean,
        recordScreenEnabled: Boolean,
        displayStartTime: String,
        displayAlarmTime: String,
        pinCode: String,
        maxRetries: Int,
        appLang: String,
        onRecordScreenToggle: (Boolean) -> Unit,
        onWorkModeChange: (WorkMode) -> Unit,
        onAlarmToggle: (Boolean) -> Unit,
        onPickStartTime: () -> Unit,
        onPickSignTime: () -> Unit,
        onOpenPinDialog: () -> Unit,
        onMaxRetriesChange: (Int) -> Unit,
        onOpenKeepAlive: () -> Unit,
        onLanguageChange: (String) -> Unit
    ) {
        val modeOptions = listOf(
            ChoiceOption(
                value = WorkMode.ROOT_NORMAL,
                label = stringResource(R.string.mode_root_normal_title),
                description = stringResource(R.string.mode_root_normal_desc)
            ),
            ChoiceOption(
                value = WorkMode.ROOT_SPRINT,
                label = stringResource(R.string.mode_root_sprint_title),
                description = stringResource(R.string.mode_root_sprint_desc)
            ),
            ChoiceOption(
                value = WorkMode.ACCESSIBILITY_NORMAL,
                label = stringResource(R.string.mode_a11y_normal_title),
                description = stringResource(R.string.mode_a11y_normal_desc)
            ),
            ChoiceOption(
                value = WorkMode.ACCESSIBILITY_SKZ,
                label = stringResource(R.string.mode_a11y_skz_title),
                description = stringResource(R.string.mode_a11y_skz_desc)
            )
        )

        val retryOptions = listOf(
            ChoiceOption(1, stringResource(R.string.retry_1), "单步异常时重试 1 次"),
            ChoiceOption(2, stringResource(R.string.retry_2), "单步异常时重试 2 次"),
            ChoiceOption(3, stringResource(R.string.retry_3), "推荐配置，兼顾成功率与耗电"),
            ChoiceOption(5, stringResource(R.string.retry_5), "弱网或卡顿环境下深度重试 5 次")
        )

        val languageOptions = listOf(
            ChoiceOption("zh", "简体中文", "Simplified Chinese"),
            ChoiceOption("en", "English", "English interface")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SegmentedGroup(title = stringResource(R.string.core_work_mode)) {
                    item { index, count ->
                        ChoiceItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.core_work_mode),
                            subtitle = workMode.getDesc(this@MainActivity),
                            icon = PixelIcons.Tune,
                            options = modeOptions,
                            selectedValue = workMode,
                            onOptionSelected = onWorkModeChange
                        )
                    }
                }
            }

            item {
                SegmentedGroup(title = "定时与自动化调度") {
                    item { index, count ->
                        SwitchItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.enable_alarm),
                            subtitle = if (alarmEnabled) {
                                "已开启每日精确闹钟 ($displayAlarmTime)"
                            } else {
                                "开启后每日按设定时间自动唤醒并完成签到"
                            },
                            icon = PixelIcons.Schedule,
                            checked = alarmEnabled,
                            onCheckedChange = onAlarmToggle
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.sign_start_time),
                            subtitle = stringResource(R.string.sign_start_time_sub),
                            icon = PixelIcons.Bolt,
                            valueText = displayStartTime,
                            valueColor = MaterialTheme.colorScheme.primary,
                            enabled = workMode.isSprint,
                            onClick = onPickStartTime
                        )
                    }
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.daily_sign_time),
                            subtitle = if (workMode.isSprint) {
                                stringResource(R.string.sprint_time_sub_locked)
                            } else {
                                stringResource(R.string.daily_sign_time_sub)
                            },
                            icon = PixelIcons.Schedule,
                            valueText = displayAlarmTime,
                            valueColor = MaterialTheme.colorScheme.primary,
                            enabled = !workMode.isSprint,
                            onClick = onPickSignTime
                        )
                    }
                }
            }

            item {
                SegmentedGroup(title = "打卡辅助与排错") {
                    item { index, count ->
                        SwitchItem(
                            index = index,
                            count = count,
                            title = "自动打卡录屏",
                            subtitle = if (recordScreenEnabled) {
                                "打卡时自动在后台高清录屏，结束后生成 MP4 便于排查异常"
                            } else {
                                "关闭后打卡时不启动屏幕录制"
                            },
                            icon = PixelIcons.Videocam,
                            checked = recordScreenEnabled,
                            onCheckedChange = onRecordScreenToggle
                        )
                    }
                }
            }

            item {
                SegmentedGroup(title = stringResource(R.string.card_settings_title)) {
                    item { index, count ->
                        val pinDesc = if (pinCode.isBlank()) {
                            stringResource(R.string.pin_hint)
                        } else {
                            "已配置 ${pinCode.length} 位锁屏密码（仅加密存储于本地设备）"
                        }
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.lockscreen_pin),
                            subtitle = pinDesc,
                            icon = PixelIcons.Pin,
                            valueText = if (pinCode.isBlank()) "未设置" else "已设置",
                            valueColor = if (pinCode.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            onClick = onOpenPinDialog
                        )
                    }
                    item { index, count ->
                        ChoiceItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.retry_limit),
                            subtitle = stringResource(R.string.retry_limit_desc),
                            icon = PixelIcons.Replay,
                            options = retryOptions,
                            selectedValue = maxRetries,
                            onOptionSelected = onMaxRetriesChange
                        )
                    }
                }
            }

            item {
                SegmentedGroup(title = "后台保活与通用") {
                    item { index, count ->
                        NavItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.keep_alive_title),
                            subtitle = stringResource(R.string.keep_alive_desc),
                            icon = PixelIcons.BatteryChargingFull,
                            onClick = onOpenKeepAlive
                        )
                    }
                    item { index, count ->
                        ChoiceItem(
                            index = index,
                            count = count,
                            title = stringResource(R.string.language_title),
                            subtitle = "切换应用显示语言 / Select display language",
                            icon = PixelIcons.Language,
                            options = languageOptions,
                            selectedValue = appLang,
                            onOptionSelected = onLanguageChange
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PinConfigAlertDialog(
        initialPin: String,
        onSave: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        var text by remember { mutableStateOf(initialPin) }
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge, // 28.dp
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = stringResource(R.string.lockscreen_pin),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "用于息屏状态下自动唤醒并解锁屏幕。密码仅保存在您手机本地，绝不联网上传。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { input ->
                            text = input.filter { it.isDigit() }
                        },
                        label = { Text(stringResource(R.string.pin_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSave(text.trim()) }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }

    @Composable
    private fun AgreementAlertDialog(
        onAgree: () -> Unit,
        onDisagree: () -> Unit
    ) {
        val agreementMessage = """
欢迎使用 s1ngerAutoSign。在首次使用本软件前，请您务必认真阅读并理解以下重要事项：

1. 【权限使用与风险告知】
本软件基于 Android 系统的“无障碍服务”或“Root 权限”实现自动化屏幕点击与解锁。上述权限属于系统级敏感权限，开启可能会引发系统安全提示、被手机安全管家标记或存在偶发系统稳定性风险。若因环境冲突或使用不当造成异常，与本软件及开发者无关。

2. 【非作弊与合规声明】
本项目仅在本地手机端模拟人工触屏点击，旨在简化日常繁琐打卡操作，绝不包含且绝不通过修改系统定位、篡改打卡时间、抓包注入等任何作弊手段在签到中弄虚作假。所有打卡信息均由您设备本地真实产生。

3. 【照片记录与隐私保护声明】
请注意：自动签到打卡所拍摄的照片将直接上传并保存在今日校园官方系统中，请您注意个人肖像与生活隐私保护。本软件仅为手机本地自动化辅助工具，绝不在本地保存、记录任何签到照片，且本软件无任何联网上传功能、无任何远程服务器通信，绝无个人数据泄露风险。

4. 【安全提示与 PIN 码说明】
本软件可能会被部分手机安全中心提示存在风险；在使用息屏解锁功能时需由您在本地录入锁屏数字 PIN 码。我们郑重承诺：所有敏感信息均仅加密存储在您的手机本地设备中，绝不上传任何云端，绝不借此实现非法行为。

5. 【介意请勿使用】
若您对上述权限存在安全顾虑、无法接受相关风险或不同意本协议的任何条款，请点击“不同意并退出”并立即卸载本软件。
        """.trimIndent()

        AlertDialog(
            onDismissRequest = {},
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = stringResource(R.string.agreement_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = agreementMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onAgree) {
                    Text(stringResource(R.string.agree_and_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = onDisagree) {
                    Text(stringResource(R.string.disagree_and_exit))
                }
            }
        )
    }

    @Composable
    private fun AccessibilityGuideAlertDialog(
        onGoSettings: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = "开启无障碍辅助服务",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Text(
                    text = """
s1ngerAutoSign 需要开启系统的【无障碍辅助服务】权限以实现自动点击、定位与拍照打卡。

👉 开启步骤指引：
1. 点击下方【前往开启】将自动跳转至系统设置；
2. 在“已下载的服务”或“已安装的应用”中找到【s1ngerAutoSign】；
3. 将无障碍服务开关打开并点击【确定/允许】即可返回。
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onGoSettings) {
                    Text("前往开启")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("稍后开启")
                }
            }
        )
    }

    private fun applyLanguage(langTag: String) {
        val appLocales = LocaleListCompat.forLanguageTags(langTag)
        AppCompatDelegate.setApplicationLocales(appLocales)
    }

    private fun getCustomDeviceName(): String {
        try {
            val globalName = Settings.Global.getString(contentResolver, "device_name")?.trim()
            if (!globalName.isNullOrBlank() && globalName != "null") return globalName

            val bluetoothName = Settings.Secure.getString(contentResolver, "bluetooth_name")?.trim()
            if (!bluetoothName.isNullOrBlank() && bluetoothName != "null") return bluetoothName

            val systemName = Settings.System.getString(contentResolver, "device_name")?.trim()
            if (!systemName.isNullOrBlank() && systemName != "null") return systemName

            val marketName = RootUtil.execCommand("getprop ro.product.marketname").trim()
            if (marketName.isNotBlank() && marketName != "null") return marketName
        } catch (_: Exception) {
        }
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * 触发息屏全流程测试序列
     */
    private fun startUnlockTest(mode: WorkMode, pin: String) {
        appendLog("🧪 开始【${mode.getTitle(this)} 息屏模拟测试】...")
        appendLog("⏳ 手机将在 3 秒后自动息屏，请勿手动按键...")

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AutoSign:TestWakeLock"
        )
        wakeLock.acquire(25 * 1000L)

        testJob?.cancel()
        testJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(3000)
                if (!isActive) return@launch

                appendLog("🔒 执行自动息屏锁屏...")
                if (mode.isRoot) {
                    RootUtil.lockScreen()
                } else if (mode.isSkz) {
                    AdbUtil.lockScreen()
                } else {
                    SignAccessibilityService.instance?.let { service ->
                        NonRootUtil.lockScreen(service)
                    }
                }

                delay(5000)
                if (!isActive) return@launch

                if (mode.isSprint) {
                    appendLog("🚀 [极速模式] 执行硬件级亮屏与 PIN 解锁...")
                    RootUtil.wakeAndUnlock(this@MainActivity, pin)
                    delay(800)
                    if (!isActive) return@launch
                    appendLog("⚡ 启动极速冲榜演练直达详情页...")
                    SprintSignEngine.executeSprint(this@MainActivity, isSimulation = true)
                } else if (mode.isRoot) {
                    appendLog("💡 [Root 普通模式] 执行硬件级亮屏与 PIN 解锁...")
                    RootUtil.wakeAndUnlock(this@MainActivity, pin)
                    delay(1200)
                    if (!isActive) return@launch
                    appendLog("🚀 正在拉起今日校园...")
                    RootUtil.launchCpdaily(this@MainActivity)
                    delay(500)
                    if (!isActive) return@launch
                    SignAccessibilityService.startTask()
                } else if (mode.isSkz) {
                    appendLog("💡 [SKZ 模式] 执行 ADB 硬件级亮屏与 PIN 解锁...")
                    AdbUtil.wakeAndUnlock(this@MainActivity, pin)
                    delay(1200)
                    if (!isActive) return@launch
                    appendLog("🚀 正在通过 ADB Shell 调起今日校园...")
                    AdbUtil.launchCpdaily(this@MainActivity)
                    delay(600)
                    if (!isActive) return@launch
                    SignAccessibilityService.startTask()
                } else {
                    appendLog("💡 [无障碍模式] 触发全机型自适应唤醒与 PIN 解锁...")
                    val service = SignAccessibilityService.instance
                    if (service != null) {
                        NonRootUtil.wakeAndUnlockWithAccessibility(this@MainActivity, service, pin)
                    } else {
                        RootUtil.wakeAndUnlock(this@MainActivity, pin)
                    }
                    delay(1000)
                    if (!isActive) return@launch
                    appendLog("🚀 正在拉起今日校园...")
                    RootUtil.launchCpdaily(this@MainActivity)
                    delay(500)
                    if (!isActive) return@launch
                    SignAccessibilityService.startTask()
                }
            } catch (e: Exception) {
                appendLog("❌ 测试过程异常: ${e.message}")
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    private fun checkAccessibilityActiveDirect(context: Context): Boolean {
        if (SignAccessibilityService.isServiceConnected) return true
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                val service = splitter.next()
                if (service.contains("SignAccessibilityService", ignoreCase = true)) {
                    return true
                }
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun appendLog(msg: String) {
        SignAccessibilityService.log(msg)
    }
}
