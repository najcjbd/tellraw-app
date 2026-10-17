package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class FeatureGateTest {
    private fun v(s: String) = VersionSpec.parse(s)

    @Test fun tellrawAvailability() {
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.TELLRAW, v("1.7.1")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.TELLRAW, v("1.7.2")))
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.TELLRAW, v("bedrock:1.8.9")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.TELLRAW, v("bedrock:1.9.0")))
    }

    @Test fun javaGates() {
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_ITEMS, v("1.20.4")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_ITEMS, v("1.20.5")))
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_STOPWATCH, v("1.21.10")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_STOPWATCH, v("1.21.11")))
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_SLOTS, v("26.2")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_SLOTS, v("26.3")))
    }

    @Test fun bedrockGates() {
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.FAMILY_PARAM, v("bedrock:1.16.99")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.FAMILY_PARAM, v("bedrock:1.16.100")))
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.HAS_PROPERTY_PARAM, v("bedrock:1.20.60")))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.HAS_PROPERTY_PARAM, v("bedrock:1.20.70")))
        assertFalse(FeatureGate.isAvailable(FeatureGate.Feature.HASITEM_PARAM, v("java:1.21.5"))) // 基岩独有
    }

    @Test fun latestMeansAvailable() {
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_SLOTS, null))
        assertTrue(FeatureGate.isAvailable(FeatureGate.Feature.EXECUTE_SLOTS, VersionSpec.LATEST))
    }

    @Test fun removedFeatures() {
        assertFalse(FeatureGate.isRemovedIn(FeatureGate.RemovedFeature.MAP_COLOR, v("26.2")))
        assertTrue(FeatureGate.isRemovedIn(FeatureGate.RemovedFeature.MAP_COLOR, v("26.3")))
        assertTrue(FeatureGate.isRemovedIn(FeatureGate.RemovedFeature.MAP_COLOR, null))          // 最新 -> 已移除
        assertFalse(FeatureGate.isRemovedIn(FeatureGate.RemovedFeature.HURT_BY_TIMESTAMP, v("1.21.11")))
        assertTrue(FeatureGate.isRemovedIn(FeatureGate.RemovedFeature.HURT_BY_TIMESTAMP, v("26.2")))
    }
}
