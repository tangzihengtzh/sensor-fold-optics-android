package com.redmi.folddemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoldConfigTest {
    @Test
    fun parsesOnlyApprovedVisualCalibrationValues() {
        val config = FoldConfig.parse(
            """
            {
              "geometry": { "progress": 1, "angleDegrees": 65 },
              "texture": { "warp": { "t1": 3.25, "t2": 2.7 } },
              "optics": {
                "radialBlur": {
                  "enabled": true,
                  "maxRadiusPx": 40,
                  "distanceExponent": 3.25,
                  "angleExponent": 2.05
                },
                "chromaticDispersion": {
                  "enabled": true,
                  "maxSeparationPx": 16
                },
                "cornerMask": {
                  "enabled": true,
                  "maxOpacity": 1,
                  "reach": 5,
                  "distanceExponent": 1.75,
                  "angleExponent": 1.3
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(3.25f, config.t1)
        assertEquals(2.7f, config.t2)
        assertTrue(config.radialBlur.enabled)
        assertEquals(40f, config.radialBlur.maxRadiusPx)
        assertEquals(3.25f, config.radialBlur.distanceExponent)
        assertEquals(2.05f, config.radialBlur.angleExponent)
        assertTrue(config.chromaticDispersion.enabled)
        assertEquals(16f, config.chromaticDispersion.maxSeparationPx)
        assertTrue(config.cornerMask.enabled)
        assertEquals(1f, config.cornerMask.maxOpacity)
        assertEquals(5f, config.cornerMask.reach)
        assertEquals(1.75f, config.cornerMask.distanceExponent)
        assertEquals(1.3f, config.cornerMask.angleExponent)
    }

    @Test
    fun bundledConfigurationContainsVisualValuesButNoCapturedFoldState() {
        val json = File("src/main/assets/demo_fold_config.json").readText()
        val config = FoldConfig.parse(json)

        assertEquals(3.25f, config.t1)
        assertEquals(2.7f, config.t2)
        assertEquals(40f, config.radialBlur.maxRadiusPx)
        assertEquals(16f, config.chromaticDispersion.maxSeparationPx)
        assertEquals(5f, config.cornerMask.reach)
        assertFalse(json.contains("\"progress\""))
        assertFalse(json.contains("\"angleDegrees\""))
        assertFalse(json.contains("\"frameVertices\""))
    }
}
