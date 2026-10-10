package com.tellraw.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 目标选择器 -> 实体种类（玩家 / 非玩家 / 不确定）判定。
 * 玩家主手用 SelectedItem、非玩家主手用 equipment.mainhand，所以这个判定要"能确定才下结论"。
 */
class SelectorEntityKindTest {

    private fun kind(s: String) = SelectorEntityKind.analyze(s)
    private val P = SelectorEntityKind.Kind.PLAYER_ONLY
    private val N = SelectorEntityKind.Kind.NON_PLAYER_ONLY
    private val A = SelectorEntityKind.Kind.AMBIGUOUS

    @Test
    fun playerOnlySelectors() {
        assertEquals(P, kind("@a"))
        assertEquals(P, kind("@p"))
        assertEquals(P, kind("@r"))
        assertEquals(P, kind("@a[tag=x]"))
        assertEquals(P, kind("@e[type=player]"))
        assertEquals(P, kind("@e[type=minecraft:player,distance=..5]"))
    }

    @Test
    fun nonPlayerOnlySelectors() {
        assertEquals(N, kind("@e[type=zombie]"))
        assertEquals(N, kind("@e[type=!player]"))
        assertEquals(N, kind("@e[x=1,y=2,z=3,type=zombie]"))
    }

    @Test
    fun ambiguousSelectors() {
        assertEquals(A, kind("@e"))
        assertEquals(A, kind("@e[sort=nearest,limit=1]"))
        assertEquals(A, kind("@s"))
        assertEquals(A, kind("@n"))
        assertEquals(A, kind("@e[type=!zombie]"))       // 排除僵尸 -> 可能是玩家或别的
        assertEquals(A, kind("@e[type=#minecraft:skeletons]")) // 实体标签成员未知
        assertEquals(A, kind("@e[type=zombie,type=skeleton]")) // 多值
        assertEquals(A, kind("@something"))
    }
}
