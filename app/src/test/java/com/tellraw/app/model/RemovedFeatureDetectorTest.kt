package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class RemovedFeatureDetectorTest {
    @Test fun detectsKnownRemovedMarkers() {
        val a = RemovedFeatureDetector.detect("{map_color:123}")!!
        assertEquals(FeatureGate.RemovedFeature.MAP_COLOR, a.feature)
        assertEquals(VersionSpec.parse("26.2"), a.suggested)

        val b = RemovedFeatureDetector.detect("data entity @s {HurtByTimestamp:0}")!!
        assertEquals(FeatureGate.RemovedFeature.HURT_BY_TIMESTAMP, b.feature)
        assertEquals(VersionSpec.parse("1.21.11"), b.suggested)

        assertNull(RemovedFeatureDetector.detect("{\"text\":\"a\"}"))
    }
}
