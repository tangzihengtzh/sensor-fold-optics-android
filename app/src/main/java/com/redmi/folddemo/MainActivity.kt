package com.redmi.folddemo

import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var setupScreen: SetupScreen
    private lateinit var sensorController: FoldSensorController
    private var surfaceView: FoldSurfaceView? = null
    private var selectedImageUri: Uri? = null
    private var demoActive = false
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorController = FoldSensorController(this) { pose ->
            if (demoActive) surfaceView?.setFoldPose(pose)
        }
        if (!sensorController.isSupported) {
            Log.e(TAG, "Required gyroscope or rotation-vector sensor is unavailable")
            Toast.makeText(this, "设备缺少所需姿态传感器", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupScreen = SetupScreen(
            context = this,
            initialConfig = FoldConfig.load(this),
            onChooseImage = ::launchImagePicker,
            onStartDemo = ::decodeAndBeginDemo,
        )
        setContentView(setupScreen)
        configureSetupWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                ::handleBack,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        if (demoActive) {
            configureFullscreen()
            surfaceView?.onResume()
            sensorController.start()
        } else {
            configureSetupWindow()
        }
    }

    override fun onPause() {
        resumed = false
        sensorController.stop()
        surfaceView?.onPause()
        super.onPause()
    }

    @Deprecated("Deprecated in Android; retained for the platform document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMAGE) return
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            Log.i(TAG, "Image selection canceled")
            return
        }

        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { error ->
            Log.w(TAG, "Could not persist image permission; current session remains usable", error)
        }
        selectedImageUri = uri
        setupScreen.setSelectedImage(uri)
    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated("Used as the Android 10-12 fallback; Android 13+ uses OnBackInvokedDispatcher")
    override fun onBackPressed() {
        handleBack()
    }

    private fun handleBack() {
        if (demoActive) showSetup() else finish()
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun decodeAndBeginDemo(config: FoldConfig) {
        val uri = selectedImageUri ?: return
        setupScreen.isEnabled = false
        Thread {
            runCatching { decodeImage(uri) }
                .onSuccess { bitmap -> runOnUiThread { beginDemo(bitmap, config) } }
                .onFailure { error ->
                    Log.e(TAG, "Unable to decode selected image", error)
                    runOnUiThread {
                        setupScreen.isEnabled = true
                        Toast.makeText(this, "无法读取所选图片", Toast.LENGTH_LONG).show()
                    }
                }
        }.apply {
            name = "FoldImageDecoder"
            start()
        }
    }

    private fun decodeImage(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val largest = maxOf(info.size.width, info.size.height)
            if (largest > MAX_TEXTURE_DIMENSION) {
                val scale = MAX_TEXTURE_DIMENSION.toFloat() / largest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    }

    private fun beginDemo(bitmap: Bitmap, config: FoldConfig) {
        val rendererView = FoldSurfaceView(this, config)
        surfaceView = rendererView
        demoActive = true
        setContentView(rendererView)
        rendererView.setImage(bitmap)
        sensorController.resetBaseline()
        if (resumed) {
            rendererView.onResume()
            sensorController.start()
        }
        configureFullscreen()
        setupScreen.isEnabled = true
        Log.i(
            TAG,
            "Bidirectional demo ready with image ${bitmap.width}x${bitmap.height}; sensor=${sensorController.sensorName}",
        )
    }

    private fun showSetup() {
        sensorController.stop()
        surfaceView?.onPause()
        surfaceView = null
        demoActive = false
        setContentView(setupScreen)
        configureSetupWindow()
    }

    private fun configureSetupWindow() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.decorView.windowInsetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun configureFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            window.setDecorFitsSystemWindows(false)
            window.decorView.windowInsetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    companion object {
        private const val TAG = "RedmiFoldDemo"
        private const val REQUEST_IMAGE = 1001
        private const val MAX_TEXTURE_DIMENSION = 4096
    }
}
