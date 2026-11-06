package com.autosign.cpdaily

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autosign.cpdaily.ui.components.ChoiceItem
import com.autosign.cpdaily.ui.components.ChoiceOption
import com.autosign.cpdaily.ui.components.NavItem
import com.autosign.cpdaily.ui.components.SegmentedGroup
import com.autosign.cpdaily.ui.icons.PixelIcons
import com.autosign.cpdaily.ui.theme.PixelExpressiveTheme

class KeepAliveActivity : AppCompatActivity() {

    enum class PhoneBrand(val displayName: String, val systemDesc: String) {
        XIAOMI("小米 / Redmi / POCO", "Xiaomi HyperOS / MIUI 后台保活策略"),
        HONOR("荣耀 / 华为", "MagicOS / HarmonyOS 启动管理策略"),
        OPPO("OPPO / 一加 / 真我", "ColorOS / RealmeUI 电池与自启动策略"),
        VIVO("vivo / iQOO", "OriginOS / FuntouchOS 后台高耗电与关联启动策略"),
        SAMSUNG("三星 Galaxy", "OneUI 电池不受限制与快速启动策略"),
        OTHER("通用 Android 设备", "AOSP 原生电池优化与多任务加锁策略")
    }

    private data class KeepAliveStep(
        val title: String,
        val desc: String,
        val actionText: String,
        val icon: ImageVector,
        val onClick: () -> Unit
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val initialBrand = detectPhoneBrand()

        setContent {
            PixelExpressiveTheme {
                KeepAliveScreen(initialBrand = initialBrand)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun KeepAliveScreen(initialBrand: PhoneBrand) {
        var currentBrand by remember { mutableStateOf(initialBrand) }
        var lockGuideMessage by remember { mutableStateOf<String?>(null) }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        val brandOptions = remember {
            PhoneBrand.entries.map { brand ->
                ChoiceOption(
                    value = brand,
                    label = brand.displayName,
                    description = brand.systemDesc
                )
            }
        }

        val steps = remember(currentBrand) {
            buildStepsForBrand(
                brand = currentBrand,
                onShowLockGuide = { guide -> lockGuideMessage = guide }
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "防断连保护设置",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = PixelIcons.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SegmentedGroup(title = "当前设备品牌") {
                        item { index, count ->
                            ChoiceItem(
                                index = index,
                                count = count,
                                title = "手机品牌与系统",
                                subtitle = currentBrand.systemDesc,
                                icon = PixelIcons.Smartphone,
                                options = brandOptions,
                                selectedValue = currentBrand,
                                dialogTitle = "选择手机品牌",
                                onOptionSelected = { currentBrand = it }
                            )
                        }
                    }
                }

                item {
                    SegmentedGroup(title = "必配防杀保活步骤") {
                        steps.forEach { step ->
                            item { index, count ->
                                NavItem(
                                    index = index,
                                    count = count,
                                    title = step.title,
                                    subtitle = step.desc,
                                    icon = step.icon,
                                    valueText = step.actionText,
                                    valueColor = MaterialTheme.colorScheme.primary,
                                    onClick = step.onClick
                                )
                            }
                        }
                    }
                }
            }
        }

        if (lockGuideMessage != null) {
            AlertDialog(
                onDismissRequest = { lockGuideMessage = null },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                title = {
                    Text(
                        text = "锁定后台任务教程",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                text = {
                    Text(
                        text = lockGuideMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { lockGuideMessage = null }) {
                        Text("我知道了")
                    }
                }
            )
        }
    }

    private fun buildStepsForBrand(
        brand: PhoneBrand,
        onShowLockGuide: (String) -> Unit
    ): List<KeepAliveStep> {
        return when (brand) {
            PhoneBrand.XIAOMI -> listOf(
                KeepAliveStep(
                    title = "忽略电池优化",
                    desc = "保证应用在后台正常运行，以便及时接收通知消息和唤醒打卡",
                    actionText = "设置",
                    icon = PixelIcons.BatteryChargingFull,
                    onClick = { requestIgnoreBatteryOptimization() }
                ),
                KeepAliveStep(
                    title = "允许应用自启动",
                    desc = "在应用异常退出后可自恢复，设备后台运行更稳定",
                    actionText = "设置",
                    icon = PixelIcons.RocketLaunch,
                    onClick = { openXiaomiAutoStart() }
                ),
                KeepAliveStep(
                    title = "锁定后台任务",
                    desc = "避免打卡辅助被系统或自己手动误清理",
                    actionText = "教程",
                    icon = PixelIcons.Security,
                    onClick = {
                        onShowLockGuide(
                            "小米 / HyperOS / MIUI 锁定后台指引：\n\n1. 从屏幕底部轻扫并悬停（或按多任务键），呼出【多任务后台】界面；\n2. 找到【s1ngerAutoSign】应用卡片；\n3. 长按卡片（或向下滑动卡片），在弹出的小工具栏中点击【🔒 锁头图标】；\n4. 当卡片右上角出现小锁时，即表示锁定成功！"
                        )
                    }
                )
            )

            PhoneBrand.VIVO -> listOf(
                KeepAliveStep(
                    title = "允许应用关联启动",
                    desc = "应用可被其他应用或系统广播唤醒拉起",
                    actionText = "设置",
                    icon = PixelIcons.RocketLaunch,
                    onClick = { openVivoAutoStart() }
                ),
                KeepAliveStep(
                    title = "忽略电池优化",
                    desc = "保证应用在后台正常运行，以便及时接收通知消息和来电",
                    actionText = "设置",
                    icon = PixelIcons.BatteryChargingFull,
                    onClick = { requestIgnoreBatteryOptimization() }
                ),
                KeepAliveStep(
                    title = "允许后台活动 (允许高耗电)",
                    desc = "避免应用被系统清理，熄屏时准时唤醒执行打卡",
                    actionText = "设置",
                    icon = PixelIcons.Bolt,
                    onClick = { openVivoBackgroundActivity() }
                ),
                KeepAliveStep(
                    title = "允许应用自启动",
                    desc = "在应用异常退出后可自动恢复，后台调度更可靠",
                    actionText = "设置",
                    icon = PixelIcons.Tune,
                    onClick = { openVivoAutoStart() }
                ),
                KeepAliveStep(
                    title = "锁定后台任务",
                    desc = "避免打卡任务被系统或自己手动误清理",
                    actionText = "教程",
                    icon = PixelIcons.Security,
                    onClick = {
                        onShowLockGuide(
                            "vivo / iQOO (OriginOS) 锁定后台指引：\n\n1. 从屏幕底部上滑并悬停，呼出【多任务后台】界面；\n2. 找到【s1ngerAutoSign】应用卡片；\n3. 向下滑动卡片，卡片上方将出现【🔒 锁头标识】；\n4. 锁定后清理所有后台任务时将自动保留本软件！"
                        )
                    }
                )
            )

            PhoneBrand.OPPO -> listOf(
                KeepAliveStep(
                    title = "忽略电池优化",
                    desc = "保证应用在后台正常运行，以便及时接收通知消息和来电",
                    actionText = "设置",
                    icon = PixelIcons.BatteryChargingFull,
                    onClick = { requestIgnoreBatteryOptimization() }
                ),
                KeepAliveStep(
                    title = "允许后台活动",
                    desc = "避免应用被系统清理，允许常驻后台运行",
                    actionText = "设置",
                    icon = PixelIcons.Bolt,
                    onClick = { openOppoAutoStart() }
                ),
                KeepAliveStep(
                    title = "允许应用自启动",
                    desc = "在应用异常退出后可自恢复，设备运行更稳定",
                    actionText = "设置",
                    icon = PixelIcons.RocketLaunch,
                    onClick = { openOppoAutoStart() }
                ),
                KeepAliveStep(
                    title = "锁定后台任务",
                    desc = "避免打卡服务被系统或自己手动误清理",
                    actionText = "教程",
                    icon = PixelIcons.Security,
                    onClick = {
                        onShowLockGuide(
                            "OPPO / 一加 / 真我 (ColorOS) 锁定后台指引：\n\n1. 呼出【多任务后台卡片】列表；\n2. 找到【s1ngerAutoSign】应用卡片；\n3. 点击卡片右上角【⋮ 更多选项】；\n4. 在菜单中点击【锁定】；\n5. 卡片右上角显示锁头标志即锁定成功！"
                        )
                    }
                )
            )

            PhoneBrand.HONOR -> listOf(
                KeepAliveStep(
                    title = "忽略电池优化",
                    desc = "保证应用在后台正常运行，以便及时接收通知消息和来电",
                    actionText = "设置",
                    icon = PixelIcons.BatteryChargingFull,
                    onClick = { requestIgnoreBatteryOptimization() }
                ),
                KeepAliveStep(
                    title = "应用启动管理 (设为手动管理)",
                    desc = "关闭自动管理，勾选【允许自启动】、【允许关联启动】、【允许后台活动】",
                    actionText = "设置",
                    icon = PixelIcons.RocketLaunch,
                    onClick = { openHonorAutoStart() }
                ),
                KeepAliveStep(
                    title = "锁定后台任务",
                    desc = "避免打卡服务被系统或自己手动误清理",
                    actionText = "教程",
                    icon = PixelIcons.Security,
                    onClick = {
                        onShowLockGuide(
                            "荣耀 (MagicOS) 锁定后台指引：\n\n1. 呼出多任务卡片界面；\n2. 找到【s1ngerAutoSign】卡片；\n3. 向下滑动卡片，卡片右上角将出现【🔒 锁头】；\n4. 锁定后一键清理后台时本应用将安全常驻！"
                        )
                    }
                )
            )

            PhoneBrand.SAMSUNG -> listOf(
                KeepAliveStep(
                    title = "忽略电池优化",
                    desc = "保证应用在后台正常运行，以便及时接收通知消息和来电",
                    actionText = "设置",
                    icon = PixelIcons.BatteryChargingFull,
                    onClick = { requestIgnoreBatteryOptimization() }
                ),
                KeepAliveStep(
                    title = "允许后台不受限制运行",
                    desc = "在电池选项中选择【不受限制】，防止系统休眠深度清理",
                    actionText = "设置",
                    icon = PixelIcons.Bolt,
                    onClick = { openAppDetails() }
                ),
                KeepAliveStep(
                    title = "锁定后台任务",
                    desc = "避免打卡服务被系统或自己手动误清理",
                    actionText = "教程",
                    icon = PixelIcons.Security,
                    onClick = {
                        onShowLockGuide(
                            "三星 (OneUI) 锁定后台指引：\n\n1. 呼出多任务管理列表；\n2. 点击【s1ngerAutoSign】卡片上方的【圆圈应用图标】；\n3. 在弹出菜单中点击【保持打开以供快速启动】（或【锁定此应用程序】）；\n4. 图标右下角显示锁头即锁定完成！"
                        )
                    }
                )
            )

            PhoneBrand.OTHER -> listOf(
                KeepAliveStep(
                    title = "忽略电池优化",
                    desc = "保证应用在后台正常运行，以便及时接收通知消息和来电",
                    actionText = "设置",
                    icon = PixelIcons.BatteryChargingFull,
                    onClick = { requestIgnoreBatteryOptimization() }
                ),
                KeepAliveStep(
                    title = "应用系统设置与权限",
                    desc = "检查电池优化、自启动与后台活动权限",
                    actionText = "设置",
                    icon = PixelIcons.Tune,
                    onClick = { openAppDetails() }
                ),
                KeepAliveStep(
                    title = "锁定后台任务",
                    desc = "避免打卡服务被系统或自己手动误清理",
                    actionText = "教程",
                    icon = PixelIcons.Security,
                    onClick = {
                        onShowLockGuide(
                            "通用设备锁定后台指引：\n\n1. 呼出系统多任务管理界面；\n2. 找到【s1ngerAutoSign】卡片；\n3. 长按卡片或点击卡片菜单，选择【锁定】；\n4. 确保在多任务清理时不被杀掉！"
                        )
                    }
                )
            )
        }
    }

    private fun detectPhoneBrand(): PhoneBrand {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> PhoneBrand.XIAOMI
            manufacturer.contains("honor") || brand.contains("honor") || manufacturer.contains("huawei") -> PhoneBrand.HONOR
            manufacturer.contains("oppo") || brand.contains("oppo") || brand.contains("oneplus") || brand.contains("realme") -> PhoneBrand.OPPO
            manufacturer.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo") -> PhoneBrand.VIVO
            manufacturer.contains("samsung") || brand.contains("samsung") -> PhoneBrand.SAMSUNG
            else -> PhoneBrand.OTHER
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    return
                } else {
                    Toast.makeText(this, "✅ 已处于电池无限制/忽略优化状态", Toast.LENGTH_SHORT).show()
                }
            }
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            openAppDetails()
        }
    }

    private fun openXiaomiAutoStart() {
        if (!safeStartIntent(
                Intent().setComponent(
                    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                )
            )
        ) {
            openAppDetails()
        }
    }

    private fun openOppoAutoStart() {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.AppListActivity"))
        )
        for (intent in intents) {
            if (safeStartIntent(intent)) return
        }
        openAppDetails()
    }

    private fun openVivoAutoStart() {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.safeguard.PurviewTabActivity"))
        )
        for (intent in intents) {
            if (safeStartIntent(intent)) return
        }
        openAppDetails()
    }

    private fun openVivoBackgroundActivity() {
        val intent = Intent().setComponent(
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
        )
        if (!safeStartIntent(intent)) {
            openAppDetails()
        }
    }

    private fun openHonorAutoStart() {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.optimize.bootstart.BootStartActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"))
        )
        for (intent in intents) {
            if (safeStartIntent(intent)) return
        }
        openAppDetails()
    }

    private fun openAppDetails() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "打开设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun safeStartIntent(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}