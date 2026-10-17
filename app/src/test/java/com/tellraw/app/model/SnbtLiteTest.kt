package com.tellraw.app.model

import org.junit.Assert.*
import org.junit.Test

class SnbtLiteTest {

    private fun q(s: String) = SnbtLite.parseCompound(s)!!
    private fun m(query: String, actual: String): Boolean =
        SnbtLite.matches(
            SnbtLite.Value.Compound(q(query)),
            SnbtLite.Value.Compound(q(actual))
        )

    @Test fun scalarAndCompoundSubset() {
        assertTrue(m("{Health:20.0f}", "{Health:20.0f,NoAI:0b}"))
        assertFalse(m("{Health:20.0f}", "{Health:19.0f}"))
        assertTrue(m("{a:{b:1}}", "{a:{b:1,c:2},z:3}"))
        assertFalse(m("{a:{b:1}}", "{a:{b:2}}"))
        // 数字类型后缀不影响数值比较
        assertTrue(m("{Slot:0b}", "{Slot:0}"))
        assertFalse(m("{Slot:0b}", "{Slot:1}"))
        // 布尔与 0/1 互通
        assertTrue(m("{NoAI:true}", "{NoAI:1b}"))
        assertTrue(m("{NoAI:1b}", "{NoAI:true}"))
    }

    @Test fun listSubsetAndTypedArray() {
        assertTrue(m("{Tags:[\"a\"]}", "{Tags:[\"a\",\"b\"]}"))
        assertTrue(m("{Tags:[\"a\",\"b\"]}", "{Tags:[\"b\",\"a\"]}"))
        assertFalse(m("{Tags:[\"a\",\"c\"]}", "{Tags:[\"a\",\"b\"]}"))
        // 类型化数组 [I;…]
        assertTrue(m("{arr:[I;1,2]}", "{arr:[I;1,2,3]}"))
    }

    @Test fun inventoryStyleObjects() {
        assertTrue(
            m(
                "{Inventory:[{Slot:0b,id:\"minecraft:stone\"}]}",
                "{Inventory:[{Slot:0b,id:\"minecraft:stone\",count:64},{Slot:1b,id:\"minecraft:dirt\"}]}"
            )
        )
        assertFalse(
            m(
                "{Inventory:[{Slot:0b,id:\"minecraft:diamond\"}]}",
                "{Inventory:[{Slot:0b,id:\"minecraft:stone\"}]}"
            )
        )
    }

    @Test fun customNameJsonText() {
        val actual = q("""{CustomName:'{"text":"Steve"}'}""")
        val query = q("""{CustomName:"Steve"}""")
        assertTrue(SnbtLite.matches(SnbtLite.Value.Compound(query), SnbtLite.Value.Compound(actual)))
    }

    @Test fun invalidSnbtReturnsNull() {
        assertNull(SnbtLite.parseCompound("{a:"))
    }
}
