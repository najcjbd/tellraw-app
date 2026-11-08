package com.tellraw.app.model

import com.tellraw.app.util.ExecuteConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridCommandPlanParserTest {

    private fun player(
        uid: String,
        tags: Set<String> = emptySet(),
        items: Map<String, GridItem> = emptyMap(),
        predicates: Set<String> = emptySet(),
        relations: Map<String, Set<String>> = emptyMap()
    ) =
        GridEntity(
            uid = uid,
            id = "minecraft:player",
            tags = tags,
            items = items,
            predicates = predicates,
            relations = relations
        )

    @Test
    fun parsesTagAddTellrawAndIgnoresRemoveForSelection() {
        val input = """
            execute as @a at @s if items entity @s hotbar.* minecraft:diamond run tag @s add tellraw_a1_t
            execute as @a at @s if predicate my:special run tag @s add tellraw_a2_t
            tellraw @a[tag=tellraw_a1_t,tag=tellraw_a2_t] {"text":"ok"}
            tag @a[tag=tellraw_a1_t] remove tellraw_a1_t
            tag @a[tag=tellraw_a2_t] remove tellraw_a2_t
        """.trimIndent()
        val plan = GridCommandPlanParser.parse(input)
        requireNotNull(plan)
        assertEquals("@a[tag=tellraw_a1_t,tag=tellraw_a2_t]", plan.tellrawSelector)
        assertEquals(2, plan.conditions.size)
        assertTrue(plan.conditions.all { it.prefix.startsWith("execute as @a at @s") })

        val diamond = mapOf("hotbar.0" to GridItem("minecraft:diamond"))
        val entities = listOf(
            player("p1", items = diamond, predicates = setOf("my:special")),
            player("p2", items = diamond),
            player("p3", predicates = setOf("my:special")),
            player("p4")
        )
        val result = GridCommandPlanParser.evaluate(plan, entities, Triple(0.0, -60.0, 0.0))
        assertEquals(listOf("p1"), result.matched.map { it.uid })
    }

    @Test
    fun finalNonPlayerSelectorIsPreservedButTagsStillFilter() {
        val input = """
            execute as @a if items entity @s hotbar.* minecraft:diamond run tag @s add t
            execute as @a if predicate my:special run tag @s add u
            tellraw @e[tag=t,tag=u] {"text":"ok"}
        """.trimIndent()
        val plan = GridCommandPlanParser.parse(input)
        requireNotNull(plan)
        assertEquals("@e[tag=t,tag=u]", plan.tellrawSelector)
        val diamond = mapOf("hotbar.0" to GridItem("minecraft:diamond"))
        val entities = listOf(
            player("p1", items = diamond, predicates = setOf("my:special")),
            GridEntity(uid = "zombie", id = "minecraft:zombie", items = diamond, predicates = setOf("my:special"))
        )
        val result = GridCommandPlanParser.evaluate(plan, entities, Triple(0.0, -60.0, 0.0))
        // @e 保留；但 tag 只加在 as @a 的玩家上，所以非玩家不会带 tag
        assertEquals(listOf("p1"), result.matched.map { it.uid })
    }

    @Test
    fun onRelationTagsRelatedEntityInsteadOfBase() {
        val generated = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a on owner if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:special",
            ExecuteConverter.Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "owner"
        )
        assertNotNull(generated)
        val parsed = GridCommandPlanParser.parse(generated!!.commands.joinToString("\n"))
        assertNotNull(parsed)
        val diamond = mapOf("hotbar.0" to GridItem("minecraft:diamond"))
        val wolf = GridEntity(
            uid = "wolf",
            id = "minecraft:wolf",
            items = diamond,
            predicates = setOf("my:special")
        )
        val entities = listOf(
            player("p1", relations = mapOf("owner" to setOf("wolf"))),
            player("p2"),
            wolf
        )
        val result = GridCommandPlanParser.evaluate(parsed!!, entities, Triple(0.0, -60.0, 0.0))
        assertEquals(listOf("wolf"), result.matched.map { it.uid })
    }

    @Test
    fun reassigningPassengerReplacesOldVehicle() {
        val a = EditableGridEntity(uid = "a", relations = "passengers=p")
        val b = EditableGridEntity(uid = "b")
        val p = EditableGridEntity(uid = "p")
        val first = GridRelations.applyReciprocal(listOf(a, b, p), a, a)
        val aCleared = first.first { it.uid == "a" }.copy(relations = "")
        val second = GridRelations.applyReciprocal(first, first.first { it.uid == "a" }, aCleared)
        val bAssigned = second.first { it.uid == "b" }.copy(relations = "passengers=p")
        val third = GridRelations.applyReciprocal(second, second.first { it.uid == "b" }, bAssigned)
            .associateBy { it.uid }
        assertTrue(GridRelations.decode(third.getValue("a").relations)["passengers"].orEmpty().isEmpty())
        assertEquals(listOf("p"), GridRelations.decode(third.getValue("b").relations)["passengers"].orEmpty())
        assertEquals(listOf("b"), GridRelations.decode(third.getValue("p").relations)["vehicle"].orEmpty())
    }

    @Test
    fun relationCycleIsRejectedOnSave() {
        val a = EditableGridEntity(uid = "a")
        val b = EditableGridEntity(uid = "b")
        val firstA = a.copy(relations = "passengers=b")
        val first = GridRelations.applyReciprocal(listOf(a, b), a, firstA)
        val bNow = first.first { it.uid == "b" }
        val bCyclic = bNow.copy(relations = "passengers=a")
        val second = GridRelations.applyReciprocal(first, bNow, bCyclic).associateBy { it.uid }
        val aRel = GridRelations.decode(second.getValue("a").relations)
        val bRel = GridRelations.decode(second.getValue("b").relations)
        assertFalse(
            aRel["vehicle"].orEmpty().contains("b") && bRel["vehicle"].orEmpty().contains("a")
        )
    }

    @Test
    fun savingReverseSideRepairsOneWayRelation() {
        val a = EditableGridEntity(uid = "a", relations = "passengers=b")
        val b = EditableGridEntity(uid = "b")
        val repaired = GridRelations.applyReciprocal(listOf(a, b), b, b).associateBy { it.uid }
        assertEquals(listOf("a"), GridRelations.decode(repaired.getValue("b").relations)["vehicle"].orEmpty())
    }

    @Test
    fun relationsAreReciprocalAndRemovedTogether() {
        val a = EditableGridEntity(uid = "a", relations = "passengers=b")
        val b = EditableGridEntity(uid = "b")
        val added = GridRelations.applyReciprocal(listOf(a, b), a, a).associateBy { it.uid }
        assertTrue(GridRelations.decode(added.getValue("b").relations)["vehicle"].orEmpty().contains("a"))

        val aCleared = a.copy(relations = "")
        val removed = GridRelations.applyReciprocal(added.values.toList(), a, aCleared).associateBy { it.uid }
        assertTrue(GridRelations.decode(removed.getValue("b").relations)["vehicle"].orEmpty().isEmpty())
        assertTrue(GridRelations.decode(removed.getValue("a").relations)["passengers"].orEmpty().isEmpty())
    }

    @Test
    fun relationCoordinatesSyncRiderAndPassengersToVehicle() {
        val vehicle = EditableGridEntity(
            uid = "v", x = 10.0, y = 20.0, z = 30.0,
            relations = "passengers=p"
        )
        val rider = EditableGridEntity(
            uid = "r", x = 0.0, y = 0.0, z = 0.0,
            relations = "vehicle=v"
        )
        val passenger = EditableGridEntity(uid = "p", x = 1.0, y = 2.0, z = 3.0)
        val synced = GridRelations.syncPositions(listOf(vehicle, rider, passenger)).associateBy { it.uid }
        for (uid in listOf("v", "r", "p")) {
            assertEquals(10.0, synced.getValue(uid).x, 0.0)
            assertEquals(20.0, synced.getValue(uid).y, 0.0)
            assertEquals(30.0, synced.getValue(uid).z, 0.0)
        }
    }

    @Test
    fun relationCodecRoundTrips() {
        val encoded = GridRelations.encode(
            mapOf(
                "owner" to listOf("wolf"),
                "passengers" to listOf("p1", "p2")
            )
        )
        assertEquals(mapOf("owner" to listOf("wolf"), "passengers" to listOf("p1", "p2")), GridRelations.decode(encoded))
        assertEquals("owner=wolf;passengers=p1", GridRelations.withSelection(encoded, "passengers", listOf("p1")))
        assertEquals("owner=wolf", GridRelations.withSelection(encoded, "passengers", emptyList()))
        assertEquals("owner=wolf;passengers=p2", GridRelations.removeUid(encoded, "p1"))
    }

    @Test
    fun singleLineCommandIsNotMistakenForTagPlan() {
        val plan = GridCommandPlanParser.parse("tellraw @a {\"text\":\"ok\"}")
        assertEquals(null, plan)
    }

    @Test
    fun rotatedAsDoesNotChangeExecutor() {
        val generated = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a rotated as @e[type=zombie] if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:special",
            ExecuteConverter.Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "rot"
        )
        assertNotNull(generated)
        val parsed = GridCommandPlanParser.parse(generated!!.commands.joinToString("\n"))
        assertNotNull(parsed)
        val diamond = mapOf("hotbar.0" to GridItem("minecraft:diamond"))
        val entities = listOf(
            player("p1", items = diamond, predicates = setOf("my:special")),
            GridEntity(uid = "z", id = "minecraft:zombie")
        )
        val result = GridCommandPlanParser.evaluate(parsed!!, entities, Triple(0.0, -60.0, 0.0))
        assertEquals(listOf("p1"), result.matched.map { it.uid })
    }

    @Test
    fun originBeforeOnStaysWithOriginalExecutor() {
        val generated = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a at @s on vehicle if block ~ ~ ~ minecraft:stone " +
                "if predicate my:special",
            ExecuteConverter.Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "org"
        )
        assertNotNull(generated)
        val parsed = GridCommandPlanParser.parse(generated!!.commands.joinToString("\n"))
        assertNotNull(parsed)
        val vehicle = GridEntity(
            uid = "v", id = "minecraft:boat", x = 10.0, z = 10.0,
            predicates = setOf("my:special")
        )
        val player = player("p", relations = mapOf("vehicle" to setOf("v")))
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf("0,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val result = GridCommandPlanParser.evaluate(
            parsed!!, listOf(player, vehicle), Triple(0.0, -60.0, 0.0), world = world
        )
        assertEquals(listOf("v"), result.matched.map { it.uid })
    }

    @Test
    fun conditionBeforeOnIsEvaluatedOnPriorExecutor() {
        val generated = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a if items entity @s hotbar.* minecraft:diamond " +
                "on owner if predicate my:special",
            ExecuteConverter.Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "ord"
        )
        assertNotNull(generated)
        val parsed = GridCommandPlanParser.parse(generated!!.commands.joinToString("\n"))
        assertNotNull(parsed)
        val diamond = mapOf("hotbar.0" to GridItem("minecraft:diamond"))
        val wolf = GridEntity(
            uid = "wolf",
            id = "minecraft:wolf",
            predicates = setOf("my:special")
        )
        val entities = listOf(
            player("p1", items = diamond, relations = mapOf("owner" to setOf("wolf"))),
            wolf
        )
        val result = GridCommandPlanParser.evaluate(parsed!!, entities, Triple(0.0, -60.0, 0.0))
        assertEquals(listOf("wolf"), result.matched.map { it.uid })
    }

    @Test
    fun bareConditionsAndAtForksAreEvaluatedCorrectly() {
        val vip = player("vip", tags = setOf("vip"))
        val p2 = player("p2")
        val bare = GridCommandPlanParser.evaluateChain(
            "if entity @s[tag=vip]", "@a", listOf(vip, p2)
        )
        assertNotNull(bare)
        assertEquals(listOf("vip"), bare!!.matched.map { it.uid })

        val p = player("p")
        val near = GridEntity("near", "minecraft:zombie", x = 5.0)
        val far = GridEntity("far", "minecraft:zombie", x = 100.0)
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf("5,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val forked = GridCommandPlanParser.evaluateChain(
            "as @a at @e[type=zombie] if block ~ ~ ~ minecraft:stone",
            "@a", listOf(p, far, near), world = world
        )
        assertNotNull(forked)
        assertEquals(listOf("p"), forked!!.matched.map { it.uid })

        val empty = GridCommandPlanParser.evaluateChain(
            "as @a at @e[type=skeleton] if block 0 -60 0 minecraft:stone",
            "@a", listOf(p), world = world
        )
        assertNotNull(empty)
        assertTrue(empty!!.matched.isEmpty())
    }

    @Test
    fun missingScoreFailsInsteadOfNeutral() {
        val p = player("p")
        val r = GridConditionEvaluator.evaluate("if score @s obj matches 1", p, listOf(p))
        assertFalse(r.ok)
    }

    @Test
    fun inDimensionUsesThatDimensionsBlockData() {
        val p = player("p")
        val world = GridConditionEvaluator.GridWorld(
            dimension = "minecraft:overworld",
            blocks = mapOf("0,0,0" to GridConditionEvaluator.GridBlock("minecraft:dirt")),
            dimensions = mapOf(
                "minecraft:the_nether" to GridConditionEvaluator.DimensionWorld(
                    blocks = mapOf("0,0,0" to GridConditionEvaluator.GridBlock("minecraft:stone"))
                )
            )
        )
        val result = GridCommandPlanParser.evaluateChain(
            "in minecraft:the_nether if block 0 0 0 minecraft:stone",
            "@a", listOf(p), world = world
        )
        assertNotNull(result)
        assertEquals(listOf("p"), result!!.matched.map { it.uid })
    }

    @Test
    fun singleChainEvaluationUsesSameContextModel() {
        val p = player("p")
        val world = GridConditionEvaluator.GridWorld(
            dimension = "minecraft:overworld",
            blocks = mapOf("-1,-60,0" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val rotated = GridCommandPlanParser.evaluateChain(
            prefix = "as @a at @s rotated 90 0 if block ^ ^ ^1 minecraft:stone",
            targetSelector = "@a",
            entities = listOf(p),
            world = world
        )
        assertNotNull(rotated)
        assertEquals(listOf("p"), rotated!!.matched.map { it.uid })

        val nether = GridCommandPlanParser.evaluateChain(
            prefix = "in minecraft:the_nether if dimension minecraft:the_nether",
            targetSelector = "@a",
            entities = listOf(p),
            world = world
        )
        assertNotNull(nether)
        assertEquals(listOf("p"), nether!!.matched.map { it.uid })
    }

    @Test
    fun localCoordinatesUseRotatedContext() {
        val input = """
            execute as @a at @s rotated 0 0 if block ^ ^ ^1 minecraft:stone run tag @s add t1
            execute as @a at @s rotated 0 0 if predicate my:special run tag @s add t2
            tellraw @e[tag=t1,tag=t2] {"text":"ok"}
        """.trimIndent()
        val plan = GridCommandPlanParser.parse(input)
        assertNotNull(plan)
        val p = player("p", predicates = setOf("my:special"))
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf("0,-60,1" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val result = GridCommandPlanParser.evaluate(
            plan!!, listOf(p), Triple(0.0, -60.0, 0.0), world = world
        )
        assertEquals(listOf("p"), result.matched.map { it.uid })
    }

    @Test
    fun alignAndAnchoredAffectConditionPosition() {
        val aligned = """
            execute as @a positioned 0.5 -60.5 0.5 align xyz if block 0 -61 0 minecraft:stone run tag @s add t1
            execute as @a positioned 0.5 -60.5 0.5 align xyz if predicate my:special run tag @s add t2
            tellraw @e[tag=t1,tag=t2] {"text":"ok"}
        """.trimIndent()
        val alignedPlan = GridCommandPlanParser.parse(aligned)
        assertNotNull(alignedPlan)
        val p = player("p", predicates = setOf("my:special"))
        val world = GridConditionEvaluator.GridWorld(
            blocks = mapOf("0,-61,0" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val alignedResult = GridCommandPlanParser.evaluate(
            alignedPlan!!, listOf(p), Triple(0.0, -60.0, 0.0), world = world
        )
        assertEquals(listOf("p"), alignedResult.matched.map { it.uid })

        val eyes = """
            execute as @a at @s anchored eyes if block ^ ^ ^1 minecraft:stone run tag @s add t1
            execute as @a at @s anchored eyes if predicate my:special run tag @s add t2
            tellraw @e[tag=t1,tag=t2] {"text":"ok"}
        """.trimIndent()
        val eyesPlan = GridCommandPlanParser.parse(eyes)
        assertNotNull(eyesPlan)
        val eyesWorld = GridConditionEvaluator.GridWorld(
            blocks = mapOf("0,-59,1" to GridConditionEvaluator.GridBlock("minecraft:stone"))
        )
        val eyesResult = GridCommandPlanParser.evaluate(
            eyesPlan!!, listOf(p), Triple(0.0, -60.0, 0.0), world = eyesWorld
        )
        assertEquals(listOf("p"), eyesResult.matched.map { it.uid })
    }

    @Test
    fun inDimensionChangesConditionContext() {
        val input = """
            execute as @a in minecraft:the_nether if dimension minecraft:the_nether run tag @s add t1
            execute as @a in minecraft:the_nether if predicate my:special run tag @s add t2
            tellraw @e[tag=t1,tag=t2] {"text":"ok"}
        """.trimIndent()
        val plan = GridCommandPlanParser.parse(input)
        assertNotNull(plan)
        val p = player("p", predicates = setOf("my:special"))
        val world = GridConditionEvaluator.GridWorld(dimension = "minecraft:overworld")
        val result = GridCommandPlanParser.evaluate(
            plan!!, listOf(p), Triple(0.0, -60.0, 0.0), world = world
        )
        assertEquals(listOf("p"), result.matched.map { it.uid })
    }

    @Test
    fun generatedTagPlanCanBeSimulatedByGrid() {
        val generated = ExecuteConverter.buildMultiCommandTagPlan(
            "execute as @a at @s if items entity @s hotbar.* minecraft:diamond " +
                "if predicate my:special",
            ExecuteConverter.Direction.BEDROCK_TO_JAVA,
            tellrawTarget = "@s",
            tellrawJson = "{\"text\":\"ok\"}",
            requireUnmergeable = false,
            tagSuffix = "it"
        )
        assertNotNull(generated)
        val parsed = GridCommandPlanParser.parse(generated!!.commands.joinToString("\n"))
        assertNotNull(parsed)
        val diamond = mapOf("hotbar.0" to GridItem("minecraft:diamond"))
        val entities = listOf(
            player("p1", items = diamond, predicates = setOf("my:special")),
            player("p2", items = diamond),
            player("p3", predicates = setOf("my:special"))
        )
        val result = GridCommandPlanParser.evaluate(parsed!!, entities, Triple(0.0, -60.0, 0.0))
        assertEquals(listOf("p1"), result.matched.map { it.uid })
    }
}
