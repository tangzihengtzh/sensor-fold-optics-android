package com.redmi.folddemo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Homography(val a: Float, val d: Float, val g: Float)

enum class FoldSide(val awayDirection: Float) {
    RIGHT_EDGE_LIFT(1f),
    LEFT_EDGE_LIFT(-1f),
}

data class FoldPose(
    val progress: Float,
    val side: FoldSide,
)

data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    fun normalized(): Quaternion {
        val length = sqrt(x * x + y * y + z * z + w * w)
        if (length < 1e-7f) return IDENTITY
        return Quaternion(x / length, y / length, z / length, w / length)
    }

    fun inverse(): Quaternion {
        val normalized = normalized()
        return Quaternion(-normalized.x, -normalized.y, -normalized.z, normalized.w)
    }

    operator fun times(other: Quaternion): Quaternion = Quaternion(
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
        w = w * other.w - x * other.x - y * other.y - z * other.z,
    ).normalized()

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        fun fromRotationVector(values: FloatArray): Quaternion {
            val x = values.getOrElse(0) { 0f }
            val y = values.getOrElse(1) { 0f }
            val z = values.getOrElse(2) { 0f }
            val w = if (values.size >= 4) {
                values[3]
            } else {
                sqrt((1f - x * x - y * y - z * z).coerceAtLeast(0f))
            }
            return Quaternion(x, y, z, w).normalized()
        }
    }
}

object FoldMath {
    const val PANEL_WIDTH = 9f
    const val PANEL_HEIGHT = 16f
    const val MAX_LIFT_ANGLE_DEGREES = 65f
    const val SENSOR_DEAD_ZONE_DEGREES = 1.5f

    fun progressFromAngle(
        angleDegrees: Float,
        maxAngleDegrees: Float = MAX_LIFT_ANGLE_DEGREES,
        deadZoneDegrees: Float = SENSOR_DEAD_ZONE_DEGREES,
    ): Float {
        val magnitude = abs(angleDegrees)
        if (magnitude <= deadZoneDegrees) return 0f
        val range = (maxAngleDegrees - deadZoneDegrees).coerceAtLeast(0.001f)
        return ((magnitude - deadZoneDegrees) / range).coerceIn(0f, 1f)
    }

    fun poseFromAngle(
        angleDegrees: Float,
        maxAngleDegrees: Float = MAX_LIFT_ANGLE_DEGREES,
        deadZoneDegrees: Float = SENSOR_DEAD_ZONE_DEGREES,
        previousSide: FoldSide = FoldSide.RIGHT_EDGE_LIFT,
    ): FoldPose {
        val progress = progressFromAngle(angleDegrees, maxAngleDegrees, deadZoneDegrees)
        val side = when {
            progress == 0f -> previousSide
            angleDegrees < 0f -> FoldSide.RIGHT_EDGE_LIFT
            else -> FoldSide.LEFT_EDGE_LIFT
        }
        return FoldPose(progress, side)
    }

    fun poseFromSignedProgress(
        signedProgress: Float,
        previousSide: FoldSide = FoldSide.RIGHT_EDGE_LIFT,
    ): FoldPose {
        val clamped = signedProgress.coerceIn(-1f, 1f)
        val progress = abs(clamped)
        val side = when {
            progress < 0.0001f -> previousSide
            clamped < 0f -> FoldSide.RIGHT_EDGE_LIFT
            else -> FoldSide.LEFT_EDGE_LIFT
        }
        return FoldPose(progress, side)
    }

    fun homography(progress: Float, t1: Float, t2: Float): Homography {
        val p = progress.coerceIn(0f, 1f)
        val farX = PANEL_WIDTH + t1 * p
        val inset = t2 * p
        val scale = PANEL_HEIGHT / (PANEL_HEIGHT - 2f * inset).coerceAtLeast(0.001f)
        return Homography(
            a = farX * scale,
            d = inset * scale,
            g = scale - 1f,
        )
    }

    fun relativeTwistYDegrees(baseline: Quaternion, current: Quaternion): Float {
        var relative = baseline.inverse() * current
        if (relative.w < 0f) {
            relative = Quaternion(-relative.x, -relative.y, -relative.z, -relative.w)
        }
        val twistLength = sqrt(relative.y * relative.y + relative.w * relative.w)
        if (twistLength < 1e-7f) return 0f
        val twistY = relative.y / twistLength
        val twistW = relative.w / twistLength
        return (2f * atan2(twistY, twistW) * 180f / PI.toFloat())
    }

    fun foldSin(progress: Float): Float = sin(
        progress.coerceIn(0f, 1f) * MAX_LIFT_ANGLE_DEGREES * PI.toFloat() / 180f,
    )

    fun quaternionForYDegrees(angleDegrees: Float): Quaternion {
        val halfAngle = angleDegrees * PI.toFloat() / 360f
        return Quaternion(0f, sin(halfAngle), 0f, cos(halfAngle))
    }
}
