package com.redmi.folddemo

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import kotlin.math.roundToInt

class SetupScreen(
    context: Context,
    initialConfig: FoldConfig,
    onChooseImage: () -> Unit,
    onStartDemo: (FoldConfig) -> Unit,
) : ScrollView(context) {
    private val accent = Color.rgb(180, 255, 38)
    private val secondary = Color.rgb(160, 164, 153)
    private val selectedImageLabel: TextView
    private val startButton: Button

    private val t1 = FloatControl("T1 · 水平透视", initialConfig.t1, -4f, 6f, 0.05f)
    private val t2 = FloatControl("T2 · 垂直透视", initialConfig.t2, -3f, 6f, 0.05f)
    private val blurEnabled = toggle("X 径向模糊", initialConfig.radialBlur.enabled)
    private val blurRadius = FloatControl("最大模糊半径 / px", initialConfig.radialBlur.maxRadiusPx, 0f, 40f, 0.5f)
    private val blurDistanceExponent = FloatControl("模糊距离指数", initialConfig.radialBlur.distanceExponent, 0.25f, 8f, 0.05f)
    private val blurAngleExponent = FloatControl("模糊角度指数", initialConfig.radialBlur.angleExponent, 0.25f, 8f, 0.05f)
    private val dispersionEnabled = toggle("RGB 色散", initialConfig.chromaticDispersion.enabled)
    private val dispersionSeparation = FloatControl("最大色散偏移 / px", initialConfig.chromaticDispersion.maxSeparationPx, 0f, 16f, 0.25f)
    private val maskEnabled = toggle("缺角黑色渐变蒙版", initialConfig.cornerMask.enabled)
    private val maskOpacity = FloatControl("蒙版最大强度", initialConfig.cornerMask.maxOpacity, 0f, 1f, 0.01f)
    private val maskReach = FloatControl("蒙版向中部延伸", initialConfig.cornerMask.reach, 0.1f, 8f, 0.05f)
    private val maskDistanceExponent = FloatControl("蒙版距离指数", initialConfig.cornerMask.distanceExponent, 0.25f, 8f, 0.05f)
    private val maskAngleExponent = FloatControl("蒙版角度指数", initialConfig.cornerMask.angleExponent, 0.25f, 8f, 0.05f)

    init {
        setBackgroundColor(Color.BLACK)
        isFillViewport = true

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(36))
        }
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        content.addView(TextView(context).apply {
            text = "FOLD OPTICS"
            setTextColor(accent)
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.18f
        })
        content.addView(TextView(context).apply {
            text = "双向折叠演示"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(7), 0, dp(6))
        })
        content.addView(TextView(context).apply {
            text = "选择壁纸并校准视觉参数。点击开始时的姿态将作为平放零点，之后左右任一侧抬起都会触发自然镜像效果。"
            setTextColor(secondary)
            textSize = 14f
            setLineSpacing(0f, 1.28f)
            setPadding(0, 0, 0, dp(22))
        })

        val chooseButton = actionButton("选择演示图片", false).apply {
            setOnClickListener { onChooseImage() }
        }
        content.addView(chooseButton)
        selectedImageLabel = TextView(context).apply {
            text = "尚未选择图片"
            setTextColor(secondary)
            textSize = 13f
            maxLines = 2
            setPadding(dp(2), dp(9), dp(2), dp(24))
        }
        content.addView(selectedImageLabel)

        content.addView(sectionTitle("图片透视变形"))
        content.addView(t1.view)
        content.addView(t2.view)

        content.addView(sectionTitle("径向光学"))
        content.addView(blurEnabled)
        content.addView(blurRadius.view)
        content.addView(blurDistanceExponent.view)
        content.addView(blurAngleExponent.view)

        content.addView(sectionTitle("玻璃色散"))
        content.addView(dispersionEnabled)
        content.addView(dispersionSeparation.view)

        content.addView(sectionTitle("缺角处理"))
        content.addView(TextView(context).apply {
            text = "变形后落在图片外的缺角固定为纯黑"
            setTextColor(secondary)
            textSize = 13f
            setPadding(0, 0, 0, dp(8))
        })
        content.addView(maskEnabled)
        content.addView(maskOpacity.view)
        content.addView(maskReach.view)
        content.addView(maskDistanceExponent.view)
        content.addView(maskAngleExponent.view)

        startButton = actionButton("开始双向演示", true).apply {
            isEnabled = false
            alpha = 0.38f
            setOnClickListener { onStartDemo(buildConfig()) }
        }
        content.addView(startButton, LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(56),
        ).apply { topMargin = dp(28) })
    }

    fun setSelectedImage(uri: Uri) {
        selectedImageLabel.text = uri.lastPathSegment ?: "已选择图片"
        selectedImageLabel.setTextColor(Color.WHITE)
        startButton.isEnabled = true
        startButton.alpha = 1f
    }

    private fun buildConfig() = FoldConfig(
        t1 = t1.value,
        t2 = t2.value,
        radialBlur = RadialBlurConfig(
            enabled = blurEnabled.isChecked,
            maxRadiusPx = blurRadius.value,
            distanceExponent = blurDistanceExponent.value,
            angleExponent = blurAngleExponent.value,
        ),
        chromaticDispersion = ChromaticDispersionConfig(
            enabled = dispersionEnabled.isChecked,
            maxSeparationPx = dispersionSeparation.value,
        ),
        cornerMask = CornerMaskConfig(
            enabled = maskEnabled.isChecked,
            maxOpacity = maskOpacity.value,
            reach = maskReach.value,
            distanceExponent = maskDistanceExponent.value,
            angleExponent = maskAngleExponent.value,
        ),
    )

    @Suppress("UseSwitchCompatOrMaterialCode")
    private fun toggle(label: String, checked: Boolean) = Switch(context).apply {
        text = label
        isChecked = checked
        setTextColor(Color.WHITE)
        textSize = 15f
        buttonTintList = null
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun sectionTitle(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(accent)
        textSize = 12f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.08f
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun actionButton(text: String, filled: Boolean) = Button(context).apply {
        this.text = text
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        isAllCaps = false
        gravity = Gravity.CENTER
        setTextColor(if (filled) Color.BLACK else accent)
        backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (filled) accent else Color.rgb(31, 34, 27),
        )
    }

    private inner class FloatControl(
        label: String,
        initialValue: Float,
        private val minimum: Float,
        private val maximum: Float,
        private val step: Float,
    ) {
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(7))
        }
        private val valueLabel = TextView(context)
        private val seekBar = SeekBar(context)
        val value: Float
            get() = (minimum + seekBar.progress * step).coerceAtMost(maximum)

        init {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 14f
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            valueLabel.apply {
                setTextColor(accent)
                textSize = 14f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.END
            }
            row.addView(valueLabel, LinearLayout.LayoutParams(dp(70), LayoutParams.WRAP_CONTENT))
            view.addView(row)

            seekBar.max = ((maximum - minimum) / step).roundToInt()
            seekBar.progress = ((initialValue.coerceIn(minimum, maximum) - minimum) / step).roundToInt()
            seekBar.progressTintList = android.content.res.ColorStateList.valueOf(accent)
            seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(accent)
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = updateLabel()
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
            view.addView(seekBar, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38)))
            updateLabel()
        }

        private fun updateLabel() {
            valueLabel.text = if (step >= 0.1f) "%.1f".format(value) else "%.2f".format(value)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
