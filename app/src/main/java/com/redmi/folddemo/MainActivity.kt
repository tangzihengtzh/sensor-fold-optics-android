package com.redmi.folddemo

import android.app.Activity
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
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var surfaceView: FoldSurfaceView
    private lateinit var sensorController: FoldSensorController
    private var demoReady = false
    private var resumed = false
    private var pickerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = FoldConfig.load(this)
        surfaceView = FoldSurfaceView(this, config)
        sensorController = FoldSensorController(this) { progress ->
            surfaceView.setFoldProgress(progress)
        }
        setContentView(surfaceView)
        configureFullscreen()

        if (!sensorController.isSupported) {
            Log.e(TAG, "Required gyroscope or rotation-vector sensor is unavailable")
            Toast.makeText(this, "设备缺少所需姿态传感器", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        surfaceView.post { launchImagePicker() }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        configureFullscreen()
        surfaceView.onResume()
        if (demoReady) sensorController.start()
    }

    override fun onPause() {
        resumed = false
        sensorController.stop()
        surfaceView.onPause()
        super.onPause()
    }

    @Deprecated("Deprecated in Android; retained for the platform document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMAGE) return
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            Log.i(TAG, "Image selection canceled")
            finish()
            return
        }
        decodeImage(uri)
    }

    private fun launchImagePicker() {
        if (pickerStarted || isFinishing) return
        pickerStarted = true
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        Log.i(TAG, "Launching image picker")
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun decodeImage(uri: Uri) {
        Thread {
            runCatching {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val sourceWidth = info.size.width
                    val sourceHeight = info.size.height
                    val largest = maxOf(sourceWidth, sourceHeight)
                    if (largest > MAX_TEXTURE_DIMENSION) {
                        val scale = MAX_TEXTURE_DIMENSION.toFloat() / largest
                        decoder.setTargetSize(
                            (sourceWidth * scale).toInt().coerceAtLeast(1),
                            (sourceHeight * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }.onSuccess { bitmap ->
                runOnUiThread { beginDemo(bitmap) }
            }.onFailure { error ->
                Log.e(TAG, "Unable to decode selected image", error)
                runOnUiThread {
                    Toast.makeText(this, "无法读取所选图片", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.apply {
            name = "FoldImageDecoder"
            start()
        }
    }

    private fun beginDemo(bitmap: Bitmap) {
        surfaceView.setImage(bitmap)
        sensorController.resetBaseline()
        demoReady = true
        if (resumed) sensorController.start()
        configureFullscreen()
        Log.i(TAG, "Demo ready with image ${bitmap.width}x${bitmap.height}; sensor=${sensorController.sensorName}")
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
