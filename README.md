# Redmi Fold Optics Demo

独立 Android 应用级原型。整块手机屏幕代表 Web Demo 的抬起侧，手机左边缘是虚拟 Y 轴铰链；从右边缘抬起手机时，姿态传感器把绕设备局部 Y 轴的相对角度映射为 0–65° 折叠进度。

## 使用流程

1. 启动应用后，从系统相册/文件选择器选择一张图片。
2. 选择完成时保持手机处于准备作为“平放”的姿态，该姿态会成为 0° 基准。
3. 应用进入无控件、沉浸式全屏演示。
4. 保持左边缘作为支点，从右侧抬起手机；应用实时增加透视变形和光学效果。
5. 每次重新启动应用都会重新选择图片并重新标定 0°。

取消选图会直接退出应用。当前阶段没有页面控件、调试网格、相机参数或静态动画播放；这些内容也不会从桌面配置中读取。

## 使用的校准参数

应用内置 `app/src/main/assets/demo_fold_config.json`，只读取以下视觉参数：

- 图片透视变形：T1=3.25，T2=2.7。
- X 径向模糊：最大半径 40 px，X 距离指数 3.25，角度指数 2.05。
- RGB 色散：单侧最大偏移 16 px，与径向模糊共享距离和角度指数。
- 缺角：图片单应变换超出源域的区域绘制为纯黑。
- 黑色渐变蒙版：最大强度 1，向中部延伸 5，X 距离指数 1.75，角度指数 1.3。

导出配置中的 `progress`、`angleDegrees`、`frameVertices`、相机、动画和辅助线设置均被忽略。运行进度完全由传感器生成。

## 构建

在 PowerShell 中设置本机环境后运行：

```powershell
$env:JAVA_HOME='D:\AndroidDev\AndroidStudio\jbr'
$env:ANDROID_HOME='D:\AndroidDev\AndroidSdk'
$env:ANDROID_USER_HOME='D:\AndroidDev\AndroidUserHome'
$env:GRADLE_USER_HOME='D:\AndroidDev\Gradle'
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

## 传感器策略

应用要求硬件陀螺仪，并优先订阅 `TYPE_GAME_ROTATION_VECTOR`。该融合姿态由陀螺仪和加速度计驱动，不依赖磁北，适合短时相对角度演示且比直接积分陀螺仪更抗漂移。选择图片后的第一帧姿态作为基准，1.5° 内为静止死区，之后按角度绝对值映射并进行约 55 ms 的指数平滑。

当前用角度绝对值响应左右两个旋转方向，但画面只实现一块抬起侧；本阶段按“左边缘固定、右侧抬起”的方式验收。

## 真机验收

2026-09-11 已在 Redmi M332BF / Android 16 / Adreno 830 上完成安装和真机验证：系统选图器、OpenGL ES 3.2 初始化、图片加载、姿态传感器驱动和单侧折叠视觉均通过用户验收。
