package com.tellraw.app.model

import com.tellraw.app.model.FeatureGate.Feature
import org.junit.Assert.*
import org.junit.Test

class TargetVersionPlannerTest {
    private fun v(s: String) = VersionSpec.parse(s)

    @Test fun oldVersionsFallBackToSay() {
        val p = TargetVersionPlanner.plan(v("1.7.1"))
        assertEquals(TargetVersionPlanner.OutputKind.SAY, p.kind)
        assertTrue(p.notes.any { it.contains("say") })
        val b = TargetVersionPlanner.plan(v("bedrock:1.8.9"))
        assertEquals(TargetVersionPlanner.OutputKind.SAY, b.kind)
    }

    @Test fun tellrawAvailableUsesTellraw() {
        assertEquals(TargetVersionPlanner.OutputKind.TELLRAW, TargetVersionPlanner.plan(v("1.7.2")).kind)
        assertEquals(TargetVersionPlanner.OutputKind.TELLRAW, TargetVersionPlanner.plan(v("bedrock:1.9.0")).kind)
        assertEquals(TargetVersionPlanner.OutputKind.TELLRAW, TargetVersionPlanner.plan(null).kind)
    }

    @Test fun featureNotes() {
        val p = TargetVersionPlanner.plan(
            v("1.20.4"),
            setOf(Feature.EXECUTE_ITEMS, Feature.EXECUTE_STOPWATCH, Feature.EQUIPMENT_PREDICATE)
        )
        assertEquals(TargetVersionPlanner.OutputKind.TELLRAW, p.kind)
        assertEquals(3, p.notes.size)
        // 26.2 上 slots 还不存在
        assertTrue(TargetVersionPlanner.plan(v("26.2"), setOf(Feature.EXECUTE_SLOTS)).notes.isNotEmpty())
        assertTrue(TargetVersionPlanner.plan(v("26.3"), setOf(Feature.EXECUTE_SLOTS)).notes.isEmpty())
    }
}
