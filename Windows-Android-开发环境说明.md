# 本机 Windows Android 开发环境说明

更新时间：2026-09-11

本文供后续 Coding Agent、Android Studio 和人工开发使用。

## 1. 开发用途

- Kotlin Android 应用开发
- Jetpack Compose UI
- 小米/Redmi 手机 USB ADB 真机调试
- 工程使用 Gradle Wrapper，不安装全局 Gradle
- 当前不使用 Emulator、AVD、System Image、NDK、CMake、LLDB

## 2. 统一目录

```text
D:\AndroidDev\
├─ AndroidStudio\        Android Studio 实际使用目录
├─ AndroidSdk\           Android SDK
├─ AndroidUserHome\      Android 工具用户数据
├─ Gradle\               Gradle 缓存与 Wrapper 下载
├─ StudioSystem\         Android Studio System Cache
├─ Git\                  Git for Windows
├─ Projects\             Android 工程
└─ Downloads\            安装包和临时文件
```

默认工程目录：`D:\AndroidDev\Projects\`

## 3. 已安装工具

| 工具 | 版本/路径 |
|---|---|
| Android Studio Stable | 2026.1，版本号 `261.26222.65.0-AI` |
| Android Studio | `D:\AndroidDev\AndroidStudio` |
| SDK Platform | `android-36` |
| Build-Tools | `37.0.0` |
| Platform-Tools / ADB | `37.0.1` |
| Command-line Tools | `22.0`，位于 `D:\AndroidDev\AndroidSdk\cmdline-tools\latest` |
| Android Studio JBR | `D:\AndroidDev\AndroidStudio\jbr` |
| Java | OpenJDK `25.0.3` |
| Git for Windows | `2.55.0.windows.5`，位于 `D:\AndroidDev\Git` |
| Gradle | 不安装全局版本；构建使用项目 Wrapper，验证时使用 Gradle `9.1.0` |

Android Studio System Cache：`D:\AndroidDev\StudioSystem`

## 4. 环境变量

用户级环境变量：

```text
ANDROID_HOME=D:\AndroidDev\AndroidSdk
ANDROID_USER_HOME=D:\AndroidDev\AndroidUserHome
GRADLE_USER_HOME=D:\AndroidDev\Gradle
JAVA_HOME=D:\AndroidDev\AndroidStudio\jbr
```

用户 PATH 已加入：

```text
D:\AndroidDev\AndroidSdk\platform-tools
D:\AndroidDev\AndroidSdk\cmdline-tools\latest\bin
%JAVA_HOME%\bin
D:\AndroidDev\Git\cmd
D:\AndroidDev\Git\bin
```

不要新建废弃的 `ANDROID_SDK_ROOT`。临时 PowerShell 进程若没有继承用户变量，应显式设置上述变量。

## 5. 常用检查

```powershell
java -version
git --version
adb --version
sdkmanager --version
adb devices -l
```

`sdkmanager` 当前会输出 deprecated 警告，但命令可以正常使用。

## 6. 真机调试流程

手机端需要：

1. 开启开发者选项和 USB 调试；
2. 小米系统如安装被限制，开启“通过 USB 安装”或“USB 调试（安全设置）”；
3. USB 连接后选择“文件传输 / Android Auto”；
4. 保持手机解锁，并在 RSA 弹窗中允许这台电脑进行 USB 调试。

电脑端检查：

```powershell
adb devices -l
```

目标状态：`<serial>    device`。如果为空，检查数据线、USB 模式、手机解锁状态和 RSA 授权。不要安装来源不明的万能驱动包。

## 7. HelloWorld 验证工程

已成功构建、安装并启动的最小 Compose 工程：

```text
D:\AndroidDev\Projects\HelloAndroid
```

包名：`com.example.helloandroid`

APK：`D:\AndroidDev\Projects\HelloAndroid\app\build\outputs\apk\debug\app-debug.apk`

构建：

```powershell
cd D:\AndroidDev\Projects\HelloAndroid
.\gradlew.bat assembleDebug
```

安装并启动：

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.example.helloandroid 1
```

已验证前台 Activity：`com.example.helloandroid/.MainActivity`

## 8. Agent 工作约定

- 新工程默认放入 `D:\AndroidDev\Projects\`。
- 使用工程自己的 `gradlew.bat`，不要安装全局 Gradle。
- 构建前确认 `JAVA_HOME` 指向 Android Studio JBR。
- 真机操作前先检查 `adb devices -l`，只对状态为 `device` 的设备执行安装或 Logcat 操作。
- 不要默认创建模拟器或下载 System Image。
- 不要主动安装 NDK、CMake、LLDB 或额外 JDK。
- 不要记录手机密码、RSA 私钥或其他敏感凭据。

## 9. 常用 ADB

```powershell
adb logcat
adb logcat -c
adb shell pm list packages | Select-String hello
adb shell am force-stop com.example.helloandroid
adb uninstall com.example.helloandroid
```

多设备时必须指定序列号：

```powershell
adb -s <serial> install -r <apk-path>
adb -s <serial> logcat
```

## 10. 已知事项

- 安装器未接受自定义目录，首次安装落在 `C:\Program Files\Android\Android Studio`；随后已复制到 `D:\AndroidDev\AndroidStudio`，后续应使用 D 盘路径。
- C 盘副本保留，不要将其误认为 Agent 应使用的路径。
- 当前没有配置模拟器或下载 AVD/System Image。
- 真机链路已验证：ADB 识别、APK 安装和应用启动均成功。
