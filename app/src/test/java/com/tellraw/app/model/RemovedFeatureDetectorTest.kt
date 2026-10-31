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
        assertEquals(VersionSpec.parse("26.1"), b.suggested)

        assertEquals(
            FeatureGate.RemovedFeature.SWING_ANIMATION,
            RemovedFeatureDetector.detect("item {swing_animation:1}")!!.feature
        )

        assertNull(RemovedFeatureDetector.detect("{\"text\":\"a\"}"))
        // 标签/名字的值里出现同样单词，不算使用了该特性
        assertNull(RemovedFeatureDetector.detect("@a[tag=swing_animation]"))
        assertNull(RemovedFeatureDetector.detect("@a[tag=map_color]"))
        assertNull(RemovedFeatureDetector.detect("@a[tag=HurtByTimestamp]"))
    }
}
