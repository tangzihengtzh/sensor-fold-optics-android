package com.redmi.folddemo

import android.content.Context
import org.json.JSONObject

data class RadialBlurConfig(
    val enabled: Boolean,
    val maxRadiusPx: Float,
    val distanceExponent: Float,
    val angleExponent: Float,
)

data class ChromaticDispersionConfig(
    val enabled: Boolean,
    val maxSeparationPx: Float,
)

data class CornerMaskConfig(
    val enabled: Boolean,
    val maxOpacity: Float,
    val reach: Float,
    val distanceExponent: Float,
    val angleExponent: Float,
)

data class FoldConfig(
    val t1: Float,
    val t2: Float,
    val radialBlur: RadialBlurConfig,
    val chromaticDispersion: ChromaticDispersionConfig,
    val cornerMask: CornerMaskConfig,
) {
    companion object {
        fun load(context: Context): FoldConfig {
            val json = context.assets.open("demo_fold_config.json")
                .bufferedReader()
                .use { it.readText() }
            return parse(json)
        }

        fun parse(json: String): FoldConfig {
            val root = JSONObject(json)
            val warp = root.getJSONObject("texture").getJSONObject("warp")
            val optics = root.getJSONObject("optics")
            val blur = optics.getJSONObject("radialBlur")
            val dispersion = optics.getJSONObject("chromaticDispersion")
            val mask = optics.getJSONObject("cornerMask")

            return FoldConfig(
                t1 = warp.getDouble("t1").toFloat().coerceIn(-4f, 6f),
                t2 = warp.getDouble("t2").toFloat().coerceIn(-3f, 6f),
                radialBlur = RadialBlurConfig(
                    enabled = blur.optBoolean("enabled", true),
                    maxRadiusPx = blur.getDouble("maxRadiusPx").toFloat().coerceIn(0f, 40f),
                    distanceExponent = blur.getDouble("distanceExponent").toFloat().coerceIn(0.25f, 8f),
                    angleExponent = blur.getDouble("angleExponent").toFloat().coerceIn(0.25f, 8f),
                ),
                chromaticDispersion = ChromaticDispersionConfig(
                    enabled = dispersion.optBoolean("enabled", true),
                    maxSeparationPx = dispersion.getDouble("maxSeparationPx").toFloat().coerceIn(0f, 16f),
                ),
                cornerMask = CornerMaskConfig(
                    enabled = mask.optBoolean("enabled", true),
                    maxOpacity = mask.getDouble("maxOpacity").toFloat().coerceIn(0f, 1f),
                    reach = mask.getDouble("reach").toFloat().coerceIn(0.1f, 8f),
                    distanceExponent = mask.getDouble("distanceExponent").toFloat().coerceIn(0.25f, 8f),
                    angleExponent = mask.getDouble("angleExponent").toFloat().coerceIn(0.25f, 8f),
                ),
            )
        }
    }
}
