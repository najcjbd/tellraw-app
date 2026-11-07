package com.tellraw.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySelectorTest {
    @Test
    fun extractsSelectorFromGeneratedForms() {
        assertEquals("@a", selectorFromCommand("tellraw @a {\"text\":\"x\"}"))
        assertEquals("@p", selectorFromCommand("execute as @a at @s run tellraw @p {\"text\":\"x\"}"))
        assertEquals("@a", selectorFromCommand("say hello"))
        assertEquals("@a", selectorFromCommand(""))
        assertEquals("@e[type=zombie]", selectorFromCommand("tellraw @e[type=zombie] {\"text\":\"x\"}"))
        assertEquals(
            "@a[tag=t1,tag=t2]",
            selectorFromCommand(
                "execute as @a if entity @s run tag @s add t1\ntellraw @a[tag=t1,tag=t2] {\"text\":\"x\"}\ntag @a[tag=t1] remove t1"
            )
        )
    }
}
