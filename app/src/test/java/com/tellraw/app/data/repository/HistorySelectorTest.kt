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

    @Test
    fun ignoresRunTellrawInsideMessageJson() {
        assertEquals(
            "@a",
            selectorFromCommand("tellraw @a {\"text\":\"seen run tellraw @e here\"}")
        )
        assertEquals(
            "@s",
            selectorFromCommand("execute as @a run tellraw @s {\"text\":\"a run tellraw @e b\"}")
        )
    }

    @Test
    fun stripsTemporaryGeneratorTags() {
        assertEquals(
            "@a[tag=user]",
            selectorFromCommand("tellraw @a[tag=user,tag=tellraw_recv_x] {\"text\":\"x\"}")
        )
        assertEquals(
            "@e[type=zombie]",
            selectorFromCommand(
                "execute as @a if entity @s run tag @s add tellraw_all_x\n" +
                    "execute as @e[tag=tellraw_all_x] run tellraw @e[type=zombie] {\"text\":\"x\"}\n" +
                    "tag @e[tag=tellraw_all_x] remove tellraw_all_x"
            )
        )
    }


    @Test
    fun keepsUserTagsThatOnlyLookTemporary() {
        assertEquals(
            "@a[tag=tellraw_helper]",
            selectorFromCommand("tellraw @a[tag=tellraw_helper] {\"text\":\"x\"}")
        )
    }


    @Test
    fun doesNotRewriteEmptyNbtLists() {
        assertEquals(
            "@e[nbt={Items:[]}]",
            selectorFromCommand("tellraw @e[nbt={Items:[]}] {\"text\":\"x\"}")
        )
    }

}
