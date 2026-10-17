package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class VersionSpecTest {
    private val J = VersionSpec.Edition.JAVA
    private val B = VersionSpec.Edition.BEDROCK

    @Test fun parsesUserExamples() {
        assertEquals(VersionSpec(J, listOf(26, 1)), VersionSpec.parse("26.1"))
        assertEquals(VersionSpec(B, listOf(1, 26, 1)), VersionSpec.parse("1.26.1"))
        assertEquals(VersionSpec(J, listOf(1, 21, 2)), VersionSpec.parse("1.21.2"))
        assertEquals(VersionSpec(B, listOf(1, 21, 131)), VersionSpec.parse("1.21.131"))
    }

    @Test fun explicitPrefixWins() {
        assertEquals(J, VersionSpec.parse("java:1.21.2")!!.edition)
        assertEquals(B, VersionSpec.parse("bedrock:1.21.2")!!.edition)
        assertEquals(B, VersionSpec.parse("1.21.130.1")!!.edition)  // 四段 -> 基岩
    }

    @Test fun latestSentinelAndInvalid() {
        assertEquals(VersionSpec.LATEST, VersionSpec.parse("-1"))
        assertNull(VersionSpec.parse("abc"))
        assertNull(VersionSpec.parse(""))
        assertNull(VersionSpec.parse(null))
    }

    @Test fun compareAndAtLeast() {
        assertTrue(VersionSpec.parse("1.21.2")!! < VersionSpec.parse("1.21.11")!!)
        assertTrue(VersionSpec.parse("26.2")!! < VersionSpec.parse("26.3")!!)
        assertTrue(VersionSpec.parse("1.20.5")!!.atLeast(1, 20, 5))
        assertFalse(VersionSpec.parse("1.20.4")!!.atLeast(1, 20, 5))
        assertTrue(VersionSpec.parse("26.3")!! >= VersionSpec.parse("26.2")!!)
    }
}
