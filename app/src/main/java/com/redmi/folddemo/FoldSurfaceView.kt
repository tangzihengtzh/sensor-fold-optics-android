package com.redmi.folddemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView

@SuppressLint("ViewConstructor")
class FoldSurfaceView(
    context: Context,
    config: FoldConfig,
) : GLSurfaceView(context) {
    private val foldRenderer = FoldRenderer(config)

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.OPAQUE)
        setPreserveEGLContextOnPause(true)
        setRenderer(foldRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        keepScreenOn = true
    }

    fun setFoldProgress(progress: Float) {
        foldRenderer.setProgress(progress)
    }

    fun setImage(bitmap: Bitmap) {
        queueEvent { foldRenderer.setBitmap(bitmap) }
    }
}
