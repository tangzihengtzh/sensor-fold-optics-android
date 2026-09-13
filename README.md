# Sensor Fold Optics for Android

[English](#english) · [简体中文](#简体中文)

## English

An application-level Android prototype that simulates foldable-display optics on a conventional slab phone. Device motion selects which edge acts as the virtual hinge, while an OpenGL ES shader applies a naturally mirrored fold effect to a user-selected image.

> This is a visual interaction prototype, not a system-level folding animation or a real foldable-device implementation.

### Features

- Bidirectional lift detection using the phone's rotation-vector sensor and gyroscope.
- Perspective image warping mirrored around the active left or right hinge.
- Hinge-aware X-axis radial blur with adjustable distance and angle exponents.
- Direction-aware RGB chromatic dispersion.
- Solid-black missing corners plus an adjustable black gradient mask.
- Pre-demo setup screen for choosing an image and tuning every visual parameter.
- Immersive, control-free rendering during the demonstration.
- Back navigation returns to the setup screen while preserving the selected image and parameter values.

### Requirements

- Android 10 or newer (API 29+)
- OpenGL ES 3.0+
- Gyroscope
- Game rotation-vector or rotation-vector sensor
- Portrait orientation

### Run the demo

1. Launch the app and choose an image from the Android document picker.
2. Adjust the perspective, blur, dispersion, and corner-mask parameters if needed.
3. Hold the phone in the position that should represent the flat state.
4. Tap **Start bidirectional demo**. That pose becomes the 0° baseline.
5. Lift either physical edge of the phone. The shader mirrors the full optical pipeline according to the detected direction.
6. Press Back to return to setup and recalibrate.

Canceling the document picker keeps the app on the setup screen. The chosen image remains local to the device and is not uploaded anywhere.

### Default calibration

The accepted defaults are bundled in `app/src/main/assets/demo_fold_config.json`:

| Effect | Default |
| --- | --- |
| Perspective warp | T1 `3.25`, T2 `2.7` |
| X radial blur | `40 px` maximum radius |
| Blur falloff | distance exponent `3.25`, angle exponent `2.05` |
| RGB dispersion | `16 px` maximum separation |
| Missing corners | solid black |
| Corner gradient mask | opacity `1`, reach `5`, distance exponent `1.75`, angle exponent `1.3` |

The exported desktop-demo progress, angle, frame vertices, camera, animation, and guide settings are intentionally ignored. Runtime progress comes entirely from device motion.

### Sensor behavior

The app prefers `TYPE_GAME_ROTATION_VECTOR`, which is driven by the gyroscope and accelerometer without depending on magnetic north. The first sensor sample after starting the demo establishes the baseline. Motion within 1.5° is treated as a dead zone; the remaining magnitude maps to a 0–65° fold range and is smoothed with an approximately 55 ms exponential filter.

Negative local-Y rotation produces a left hinge with the right edge lifted. Positive local-Y rotation produces a right hinge with the left edge lifted. Perspective mapping, missing corners, blur distance, gradient masking, and RGB channel direction are mirrored together.

### Build

Open the project in Android Studio, or build from a terminal with JDK 17 and Android SDK 36 configured:

```powershell
./gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

On macOS or Linux:

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

The debug APK is generated locally at `app/build/outputs/apk/debug/app-debug.apk`. Build outputs are excluded from version control.

### Project status

The original single-side stage was tested on a Redmi M332BF running Android 16 with an Adreno 830 GPU. The repository now contains the bidirectional renderer and pre-demo parameter setup screen.

---

## 简体中文

这是一个应用程序级 Android 视觉原型，用于在普通直板手机上模拟折叠屏抬起时的光学效果。程序根据手机姿态判断哪一侧是虚拟铰链，并通过 OpenGL ES 着色器对用户选择的图片应用自然镜像的折叠动画。

> 本项目是视觉交互原型，不是系统级折叠动画，也不代表手机具备真实折叠结构。

### 功能

- 结合旋转矢量传感器和陀螺仪识别左右两侧抬起。
- 图片透视变形会围绕当前左侧或右侧虚拟铰链自然镜像。
- 沿 X 方向的径向模糊，距离指数和角度指数均可调。
- 随抬起方向镜像的 RGB 色散。
- 缺角固定填充纯黑，并可叠加参数可调的黑色渐变蒙版。
- 演示前可选择图片并调整全部视觉参数。
- 开始演示后进入无控件的沉浸式全屏画面。
- 从演示返回设置页时保留所选图片与参数。

### 运行要求

- Android 10 或更高版本（API 29+）
- OpenGL ES 3.0+
- 陀螺仪
- 游戏旋转矢量或旋转矢量传感器
- 竖屏使用

### 使用方法

1. 启动应用，在 Android 系统文件选择器中选择一张图片。
2. 根据需要调整透视、模糊、色散和缺角蒙版参数。
3. 将手机保持在准备作为“平放”的姿态。
4. 点击“开始双向演示”，此刻姿态将成为 0° 基准。
5. 以手机任一侧边缘为支点抬起，完整光学效果会随检测到的方向镜像。
6. 按返回键回到设置页并重新标定。

取消选图不会退出应用。所选图片只在设备本地读取，不会上传至任何位置。

### 默认校准参数

已验收的默认值保存在 `app/src/main/assets/demo_fold_config.json`：

| 效果 | 默认值 |
| --- | --- |
| 图片透视变形 | T1 `3.25`，T2 `2.7` |
| X 径向模糊 | 最大半径 `40 px` |
| 模糊衰减 | 距离指数 `3.25`，角度指数 `2.05` |
| RGB 色散 | 最大偏移 `16 px` |
| 缺角 | 固定纯黑 |
| 缺角渐变蒙版 | 强度 `1`，延伸 `5`，距离指数 `1.75`，角度指数 `1.3` |

桌面 Demo 导出配置中的进度、角度、边框顶点、相机、动画和辅助线参数会被有意忽略；运行进度完全由手机姿态产生。

### 传感器逻辑

应用优先使用 `TYPE_GAME_ROTATION_VECTOR`。该融合姿态由陀螺仪和加速度计驱动，不依赖磁北。点击开始后的首个传感器样本会建立零点；1.5° 内为静止死区，其余角度幅值映射至 0–65°，并经过约 55 ms 的指数平滑。

绕设备局部 Y 轴负向旋转时，左侧为铰链、右侧抬起；正向旋转时，右侧为铰链、左侧抬起。图片透视、缺角、模糊距离场、渐变蒙版和 RGB 通道方向会整体镜像。

### 编译

可以使用 Android Studio 打开项目，也可以在配置好 JDK 17 与 Android SDK 36 的终端中执行：

```powershell
./gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

macOS 或 Linux：

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

调试 APK 会在本机生成至 `app/build/outputs/apk/debug/app-debug.apk`，编译产物不会加入版本控制。

### 项目状态

最初的单侧版本已在搭载 Android 16 和 Adreno 830 GPU 的 Redmi M332BF 上完成真机测试。当前仓库包含双侧镜像渲染和演示前参数设置页。
