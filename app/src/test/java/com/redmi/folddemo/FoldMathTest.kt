package com.redmi.folddemo

import org.junit.Assert.assertEquals
import org.junit.Test

class FoldMathTest {
    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.0001f) {
        assertEquals(expected.toDouble(), actual.toDouble(), tolerance.toDouble())
    }

    @Test
    fun foldProgressRemovesDeadZoneAndClampsEitherRotationDirection() {
        assertClose(0f, FoldMath.progressFromAngle(1f, maxAngleDegrees = 65f, deadZoneDegrees = 1.5f))
        assertClose(0f, FoldMath.progressFromAngle(-1f, maxAngleDegrees = 65f, deadZoneDegrees = 1.5f))
        assertClose(1f, FoldMath.progressFromAngle(80f, maxAngleDegrees = 65f, deadZoneDegrees = 1.5f))
        assertClose(1f, FoldMath.progressFromAngle(-80f, maxAngleDegrees = 65f, deadZoneDegrees = 1.5f))
        assertClose((30f - 1.5f) / (65f - 1.5f), FoldMath.progressFromAngle(-30f, 65f, 1.5f))
    }

    @Test
    fun homographyIsIdentityWhenFlat() {
        val warp = FoldMath.homography(progress = 0f, t1 = 3.25f, t2 = 2.7f)
        assertClose(9f, warp.a)
        assertClose(0f, warp.d)
        assertClose(0f, warp.g)
    }

    @Test
    fun homographyMatchesApprovedFullLiftWarp() {
        val warp = FoldMath.homography(progress = 1f, t1 = 3.25f, t2 = 2.7f)
        val scale = 16f / (16f - 2f * 2.7f)
        assertClose(12.25f * scale, warp.a)
        assertClose(2.7f * scale, warp.d)
        assertClose(scale - 1f, warp.g)
    }

    @Test
    fun quaternionTwistExtractsLocalYAxisRotation() {
        val baseline = Quaternion.IDENTITY
        val halfAngle = Math.toRadians(20.0).toFloat()
        val current = Quaternion(
            x = 0f,
            y = kotlin.math.sin(halfAngle),
            z = 0f,
            w = kotlin.math.cos(halfAngle),
        )

        assertClose(40f, FoldMath.relativeTwistYDegrees(baseline, current), 0.001f)
    }

    @Test
    fun signedSensorAngleSelectsTheNaturallyMirroredLiftSide() {
        val rightLift = FoldMath.poseFromAngle(-30f)
        val leftLift = FoldMath.poseFromAngle(30f)

        assertEquals(FoldSide.RIGHT_EDGE_LIFT, rightLift.side)
        assertEquals(FoldSide.LEFT_EDGE_LIFT, leftLift.side)
        assertClose(rightLift.progress, leftLift.progress)
    }

    @Test
    fun flatPoseRetainsThePreviouslyActiveSide() {
        val pose = FoldMath.poseFromAngle(0.5f, previousSide = FoldSide.LEFT_EDGE_LIFT)

        assertClose(0f, pose.progress)
        assertEquals(FoldSide.LEFT_EDGE_LIFT, pose.side)
    }
}
