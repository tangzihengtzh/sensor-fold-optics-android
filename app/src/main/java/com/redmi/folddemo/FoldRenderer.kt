package com.redmi.folddemo

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FoldRenderer(
    private val config: FoldConfig,
) : android.opengl.GLSurfaceView.Renderer {
    private val vertices: FloatBuffer = ByteBuffer.allocateDirect(VERTICES.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(VERTICES)
            position(0)
        }

    @Volatile
    private var progress = 0f
    private var program = 0
    private var texture = 0
    private var bitmap: Bitmap? = null
    private var textureWidth = 1f

    private var positionLocation = -1
    private var domainLocation = -1
    private var textureLocation = -1
    private var warpALocation = -1
    private var warpDLocation = -1
    private var warpGLocation = -1
    private var foldSinLocation = -1
    private var textureWidthLocation = -1
    private var radialBlurEnabledLocation = -1
    private var maxBlurRadiusLocation = -1
    private var distanceExponentLocation = -1
    private var angleExponentLocation = -1
    private var dispersionEnabledLocation = -1
    private var dispersionLocation = -1
    private var maskEnabledLocation = -1
    private var maskOpacityLocation = -1
    private var maskReachLocation = -1
    private var maskDistanceExponentLocation = -1
    private var maskAngleExponentLocation = -1

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    fun setBitmap(value: Bitmap) {
        bitmap = value
        if (program != 0) uploadBitmap(value)
    }

    override fun onSurfaceCreated(unused: GL10?, eglConfig: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glDisable(GLES30.GL_DITHER)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        resolveLocations()
        bitmap?.let(::uploadBitmap)
        Log.i(TAG, "OpenGL renderer=${GLES30.glGetString(GLES30.GL_RENDERER)}")
        Log.i(TAG, "OpenGL version=${GLES30.glGetString(GLES30.GL_VERSION)}")
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        if (texture == 0 || program == 0) return

        val p = progress
        val warp = FoldMath.homography(p, config.t1, config.t2)

        GLES30.glUseProgram(program)
        vertices.position(0)
        GLES30.glEnableVertexAttribArray(positionLocation)
        GLES30.glVertexAttribPointer(
            positionLocation,
            2,
            GLES30.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            vertices,
        )
        vertices.position(2)
        GLES30.glEnableVertexAttribArray(domainLocation)
        GLES30.glVertexAttribPointer(
            domainLocation,
            2,
            GLES30.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            vertices,
        )

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glUniform1i(textureLocation, 0)
        GLES30.glUniform1f(warpALocation, warp.a)
        GLES30.glUniform1f(warpDLocation, warp.d)
        GLES30.glUniform1f(warpGLocation, warp.g)
        GLES30.glUniform1f(foldSinLocation, FoldMath.foldSin(p))
        GLES30.glUniform1f(textureWidthLocation, textureWidth)
        GLES30.glUniform1i(radialBlurEnabledLocation, if (config.radialBlur.enabled) 1 else 0)
        GLES30.glUniform1f(maxBlurRadiusLocation, config.radialBlur.maxRadiusPx)
        GLES30.glUniform1f(distanceExponentLocation, config.radialBlur.distanceExponent)
        GLES30.glUniform1f(angleExponentLocation, config.radialBlur.angleExponent)
        GLES30.glUniform1i(dispersionEnabledLocation, if (config.chromaticDispersion.enabled) 1 else 0)
        GLES30.glUniform1f(dispersionLocation, config.chromaticDispersion.maxSeparationPx)
        GLES30.glUniform1i(maskEnabledLocation, if (config.cornerMask.enabled) 1 else 0)
        GLES30.glUniform1f(maskOpacityLocation, config.cornerMask.maxOpacity)
        GLES30.glUniform1f(maskReachLocation, config.cornerMask.reach)
        GLES30.glUniform1f(maskDistanceExponentLocation, config.cornerMask.distanceExponent)
        GLES30.glUniform1f(maskAngleExponentLocation, config.cornerMask.angleExponent)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun uploadBitmap(value: Bitmap) {
        if (texture != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
        }
        val names = IntArray(1)
        GLES30.glGenTextures(1, names, 0)
        texture = names[0]
        textureWidth = value.width.toFloat().coerceAtLeast(1f)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, value, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        checkGlError("uploadBitmap")
        Log.i(TAG, "Texture uploaded ${value.width}x${value.height}")
    }

    private fun resolveLocations() {
        positionLocation = GLES30.glGetAttribLocation(program, "aPosition")
        domainLocation = GLES30.glGetAttribLocation(program, "aDomain")
        textureLocation = GLES30.glGetUniformLocation(program, "uTexture")
        warpALocation = GLES30.glGetUniformLocation(program, "uWarpA")
        warpDLocation = GLES30.glGetUniformLocation(program, "uWarpD")
        warpGLocation = GLES30.glGetUniformLocation(program, "uWarpG")
        foldSinLocation = GLES30.glGetUniformLocation(program, "uFoldSin")
        textureWidthLocation = GLES30.glGetUniformLocation(program, "uTextureWidth")
        radialBlurEnabledLocation = GLES30.glGetUniformLocation(program, "uRadialBlurEnabled")
        maxBlurRadiusLocation = GLES30.glGetUniformLocation(program, "uMaxBlurRadiusPx")
        distanceExponentLocation = GLES30.glGetUniformLocation(program, "uDistanceExponent")
        angleExponentLocation = GLES30.glGetUniformLocation(program, "uAngleExponent")
        dispersionEnabledLocation = GLES30.glGetUniformLocation(program, "uChromaticDispersionEnabled")
        dispersionLocation = GLES30.glGetUniformLocation(program, "uDispersionPx")
        maskEnabledLocation = GLES30.glGetUniformLocation(program, "uCornerMaskEnabled")
        maskOpacityLocation = GLES30.glGetUniformLocation(program, "uMaskMaxOpacity")
        maskReachLocation = GLES30.glGetUniformLocation(program, "uMaskReach")
        maskDistanceExponentLocation = GLES30.glGetUniformLocation(program, "uMaskDistanceExponent")
        maskAngleExponentLocation = GLES30.glGetUniformLocation(program, "uMaskAngleExponent")
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        val result = GLES30.glCreateProgram()
        GLES30.glAttachShader(result, vertexShader)
        GLES30.glAttachShader(result, fragmentShader)
        GLES30.glLinkProgram(result)
        val status = IntArray(1)
        GLES30.glGetProgramiv(result, GLES30.GL_LINK_STATUS, status, 0)
        val log = GLES30.glGetProgramInfoLog(result)
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        if (status[0] == 0) {
            GLES30.glDeleteProgram(result)
            error("OpenGL program link failed: $log")
        }
        return result
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            error("OpenGL shader compile failed: $log")
        }
        return shader
    }

    private fun checkGlError(operation: String) {
        val error = GLES30.glGetError()
        check(error == GLES30.GL_NO_ERROR) { "$operation failed with GL error 0x${error.toString(16)}" }
    }

    companion object {
        private const val TAG = "FoldRenderer"
        private const val VERTEX_STRIDE_BYTES = 4 * Float.SIZE_BYTES

        private val VERTICES = floatArrayOf(
            -1f, 1f, 0f, 0f,
            -1f, -1f, 0f, 16f,
            1f, 1f, 9f, 0f,
            1f, -1f, 9f, 16f,
        )

        private const val VERTEX_SHADER = """
            #version 300 es
            in vec2 aPosition;
            in vec2 aDomain;
            out vec2 vDomain;
            void main() {
                vDomain = aDomain;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            uniform sampler2D uTexture;
            uniform float uWarpA;
            uniform float uWarpD;
            uniform float uWarpG;
            uniform bool uRadialBlurEnabled;
            uniform float uMaxBlurRadiusPx;
            uniform float uDistanceExponent;
            uniform float uFoldSin;
            uniform float uAngleExponent;
            uniform float uTextureWidth;
            uniform bool uChromaticDispersionEnabled;
            uniform float uDispersionPx;
            uniform bool uCornerMaskEnabled;
            uniform float uMaskMaxOpacity;
            uniform float uMaskReach;
            uniform float uMaskDistanceExponent;
            uniform float uMaskAngleExponent;
            in vec2 vDomain;
            out vec4 fragColor;

            const int OPTICAL_TAP_RADIUS = 6;

            vec4 sampleOpticalTexture(vec2 uv, float radiusPx, float dispersionPx) {
                vec4 accumulated = vec4(0.0);
                float weightSum = 0.0;
                for (int tap = -OPTICAL_TAP_RADIUS; tap <= OPTICAL_TAP_RADIUS; tap++) {
                    float normalizedOffset = float(tap) / float(OPTICAL_TAP_RADIUS);
                    float gaussianPosition = normalizedOffset * 2.4;
                    float weight = exp(-0.5 * gaussianPosition * gaussianPosition);
                    float offsetPx = normalizedOffset * radiusPx;
                    vec2 greenUv = vec2(clamp(uv.x + offsetPx / uTextureWidth, 0.0, 1.0), uv.y);
                    float dispersionUv = dispersionPx / uTextureWidth;
                    vec2 redUv = vec2(clamp(greenUv.x + dispersionUv, 0.0, 1.0), uv.y);
                    vec2 blueUv = vec2(clamp(greenUv.x - dispersionUv, 0.0, 1.0), uv.y);
                    vec4 greenSample = texture(uTexture, greenUv);
                    vec4 spectralSample = dispersionPx > 0.0001
                        ? vec4(
                            texture(uTexture, redUv).r,
                            greenSample.g,
                            texture(uTexture, blueUv).b,
                            greenSample.a
                        )
                        : greenSample;
                    accumulated += spectralSample * weight;
                    weightSum += weight;
                }
                return accumulated / weightSum;
            }

            vec4 applyCornerMask(vec4 surfaceColor) {
                if (!uCornerMaskEnabled) return surfaceColor;
                float xRatio = clamp(vDomain.x / 9.0, 0.0, 1.0);
                float topBoundaryY = uWarpD * vDomain.x / max(uWarpA, 0.0001);
                float bottomBoundaryY = 16.0 - topBoundaryY;
                float inwardDistance = min(vDomain.y - topBoundaryY, bottomBoundaryY - vDomain.y);
                float inwardFactor = 1.0 - smoothstep(0.0, uMaskReach, inwardDistance);
                float distanceFactor = pow(xRatio, uMaskDistanceExponent);
                float angleFactor = pow(uFoldSin, uMaskAngleExponent);
                float gapExists = step(0.0001, topBoundaryY);
                float maskOpacity = clamp(
                    uMaskMaxOpacity * angleFactor * distanceFactor * inwardFactor * gapExists,
                    0.0,
                    1.0
                );
                return vec4(mix(surfaceColor.rgb, vec3(0.0), maskOpacity), surfaceColor.a);
            }

            void main() {
                float warpDenominator = uWarpA - uWarpG * vDomain.x;
                float sourceU = vDomain.x / warpDenominator;
                float sourceV = (vDomain.y * (uWarpG * sourceU + 1.0) - uWarpD * sourceU) / 16.0;
                bool outside = sourceU < 0.0 || sourceU > 1.0 || sourceV < 0.0 || sourceV > 1.0;
                if (outside) {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }

                vec2 uv = vec2(sourceU, sourceV);
                vec4 surfaceColor;
                if (uRadialBlurEnabled || uChromaticDispersionEnabled) {
                    float xRatio = clamp(vDomain.x / 9.0, 0.0, 1.0);
                    float distanceFactor = pow(xRatio, uDistanceExponent);
                    float angleFactor = pow(uFoldSin, uAngleExponent);
                    float opticalFactor = angleFactor * distanceFactor;
                    float radiusPx = uRadialBlurEnabled ? uMaxBlurRadiusPx * opticalFactor : 0.0;
                    float dispersionPx = uChromaticDispersionEnabled ? uDispersionPx * opticalFactor : 0.0;
                    surfaceColor = sampleOpticalTexture(uv, radiusPx, dispersionPx);
                } else {
                    surfaceColor = texture(uTexture, uv);
                }
                fragColor = applyCornerMask(surfaceColor);
            }
        """
    }
}
