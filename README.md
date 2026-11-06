# s1ngerAutoSign - 今日校园全自动打卡独立应用

基于 **Jetpack Compose + Material 3 Expressive** 构建的 Android 独立自动签到应用，支持 **Root 极速冲榜**、**Root 普通模式**、**Shizuku (SKZ) 免 Root 模式**与**纯无障碍模式**四种工作引擎，实现每日定时唤醒、自动解锁、调起今日校园、拍照/定位打卡、高清录屏存证与自动锁屏的全流程无人值守自动化。

---

## ✨ 核心特性

### 1. 四大运行模式（Root / 免 Root 全覆盖）
- **Root 极速模式 (`ROOT_SPRINT`)**：通过 Root 权限预热 GPS 与嗅探最新签到详情页 URL / Scheme，定时唤醒后直达签到详情页完成极速冲榜打卡。
- **Root 普通模式 (`ROOT_NORMAL`)**：通过 Root Shell (`su`) 执行硬件级亮屏、上滑、自动输入 PIN 码解锁并拉起今日校园，配合无障碍辅助服务完成签到流水线。
- **无障碍 - SKZ 模式 (`ACCESSIBILITY_SKZ`)**：专为**未 Root 设备**打造，接入 **Shizuku API**（UID 2000 Shell 权限），无需 Root 即可静默激活无障碍服务、执行息屏唤醒、PIN 码自动解锁、冷启动应用及后台高清录屏。
- **纯无障碍模式 (`ACCESSIBILITY_NORMAL`)**：零特权依赖，使用 Android 原生 `AccessibilityService` 手势与节点检索完成唤醒解锁与导航签到。

### 2. 全自动签到状态机 (`SignAccessibilityService`)
1. **每日定时精确唤醒**：基于 `AlarmManager.setExactAndAllowWhileIdle` 穿透系统 Doze 深度休眠，支持自定义每日打卡时间与提前预热。
2. **息屏自动唤醒与 PIN 解锁**：自动点亮屏幕 $\rightarrow$ 上滑调出密码盘 $\rightarrow$ 自动键入锁屏数字 PIN 码 $\rightarrow$ 解锁进入系统。
3. **智能 UI 节点与 WebView DOM 混合检索**：
   - 步骤 1：自动点击今日校园底部导航【讯息】Tab。
   - 步骤 2：自动检索并进入【辅导员通知】。
   - 步骤 3：自动定位通知列表中最新一条【签到】消息并进入详情页。
   - 步骤 4：智能判断打卡状态（若【已签到】则自动跳过避免重复操作），支持内置相机拍照打卡与表单自动提交。
4. **全链路存证与自动息屏**：
   - **后台高清录屏 (`ScreenRecorder`)**：打卡启动时自动录制全流程 MP4 视频保存至 `/sdcard/Movies/AutoSign/`。
   - **防伪凭据截图**：打卡成功后自动截取今日校园官方打卡详情页保存至 `/sdcard/AutoSign/`。
   - **自动锁屏**：流水线结束后自动关闭屏幕并释放唤醒锁。
   - **一键诊断报告导出 (`LogRepository`)**：支持实时查看内置终端日志，或一键导出完整设备状态与运行日志 TXT 报告。

---

## 🛠️ 技术栈与工程架构

- **UI 框架**：Jetpack Compose + Material 3 Expressive (`1.5.0-alpha27`)，支持 Android 12+ Monet 动态取色与深浅色主题自适应。
- **构建环境**：Gradle `9.3.1` + Android Gradle Plugin `9.1.1` + Kotlin `2.4.10` (`compileSdk 37`, `minSdk 26`)。
- **特权与调试通信**：
  - Root Shell 交互：`util/RootUtil.kt`
  - Shizuku 跨进程 Binder & ADB Shell：`util/AdbUtil.kt` (`dev.rikka.shizuku:api:13.1.5`) + `dadb` 协议支持

```text
app/src/main/java/com/autosign/cpdaily/
├── App.kt                          # 全局初始化与 Shizuku 监听注册
├── MainActivity.kt                 # Compose 主界面（主页 / 测试演练终端 / 设置页）
├── KeepAliveActivity.kt            # 1像素保活与唤醒辅助 Activity
├── model/
│   └── WorkMode.kt                 # 四大工作模式枚举与多语言描述
├── receiver/
│   ├── AlarmReceiver.kt            # 每日精确定时闹钟唤醒广播接收器
│   └── BootReceiver.kt             # 开机自启自动重注册定时任务
├── service/
│   └── SignAccessibilityService.kt # 今日校园无障碍自动化核心状态机
├── ui/                             # Material 3 Expressive 主题、图标与分段卡片组件
└── util/
    ├── AdbUtil.kt                  # Shizuku (SKZ) 权限管理与 Shell 指令执行器
    ├── AlarmScheduler.kt           # 精确闹钟调度管理
    ├── GpsPrewarmer.kt             # 打卡前 GPS 硬件定位预热
    ├── LogRepository.kt            # 内存环形日志队列与 TXT 诊断报告导出
    ├── NonRootUtil.kt              # 纯无障碍唤醒、手势滑动与密码按键模拟
    ├── NotificationHelper.kt       # 打卡结果系统通知推送
    ├── PrefsUtil.kt                # 配置持久化管理
    ├── PushSniffer.kt              # 签到通知嗅探器
    ├── RootUtil.kt                 # Root Shell 唤醒、解锁与进程控制
    ├── ScreenRecorder.kt           # 自动打卡全流程后台高清录屏
    └── SprintSignEngine.kt         # 极速冲榜直达引擎
```

---

## 📱 快速开始

1. **编译打包**：
   ```bash
   ./gradlew assembleDebug
   ```
   生成的安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。
2. **权限配置**：
   - **Root 设备**：选择【Root 极速模式】或【Root 普通模式】，在 Magisk / KernelSU / APatch 弹窗中授予永久 Root 权限。
   - **非 Root 设备**：启动 **Shizuku**（无线调试激活），在应用内选择【无障碍 - SKZ 模式】并允许 Shizuku 授权，应用将通过 ADB Shell 自动启用无障碍辅助服务。
3. **参数设置与实机演练**：
   - 在【设置】页填入锁屏数字 PIN 码、设定每日定时打卡时间并开启定时开关。
   - 在【测试】页可随时点击【立即打卡测试】（亮屏测试）或【息屏全流程测试】（自动锁屏 $\rightarrow$ 延时唤醒 $\rightarrow$ 自动输入 PIN 解锁 $\rightarrow$ 打卡 $\rightarrow$ 录屏存证），通过底部实时终端查看每一步执行状态。
