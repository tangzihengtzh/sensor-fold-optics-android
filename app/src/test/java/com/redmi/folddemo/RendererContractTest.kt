package com.redmi.folddemo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RendererContractTest {
    @Test
    fun rendererPortsTheApprovedContinuousOpticsPipeline() {
        val renderer = File("src/main/java/com/redmi/folddemo/FoldRenderer.kt").readText()

        assertTrue(renderer.contains("OPTICAL_TAP_RADIUS = 6"))
        assertTrue(renderer.contains("tap = -OPTICAL_TAP_RADIUS; tap <= OPTICAL_TAP_RADIUS"))
        assertTrue(renderer.contains("texture(uTexture, redUv).r"))
        assertTrue(renderer.contains("texture(uTexture, blueUv).b"))
        assertTrue(renderer.contains("localSourceU = hingeLocalX / warpDenominator"))
        assertTrue(renderer.contains("fragColor = vec4(0.0, 0.0, 0.0, 1.0)"))
        assertFalse(renderer.contains("Shear"))
        assertTrue(renderer.contains("applyCornerMask(surfaceColor)"))
        assertFalse(renderer.contains("frameVertices"))
    }

    @Test
    fun rendererMirrorsHomographyAndDispersionInHingeLocalSpace() {
        val renderer = File("src/main/java/com/redmi/folddemo/FoldRenderer.kt").readText()

        assertTrue(renderer.contains("uAwayDirection"))
        assertTrue(renderer.contains("hingeLocalX"))
        assertTrue(renderer.contains("globalSourceU"))
        assertTrue(renderer.contains("dispersionDirection"))
    }
}
