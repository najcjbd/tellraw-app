package com.tellraw.app.util

import com.tellraw.app.util.TextComponentHelper.ComponentStyle
import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentStyleTest {

    @Test
    fun markerRoundTripKeepsStyleOnNonTextComponent() {
        val original = TextComponent(
            ComponentType.TRANSLATE,
            "chat.type.text",
            style = ComponentStyle(color = "red", bold = true, font = "minecraft:uniform")
        )
        val markers = TextComponentHelper.componentsToText(listOf(original))
        val parsed = TextComponentHelper.parseTextComponents(markers).single()
        assertEquals("chat.type.text", parsed.content)
        assertEquals("red", parsed.style.color)
        assertEquals(true, parsed.style.bold)
        assertEquals("minecraft:uniform", parsed.style.font)
    }

    @Test
    fun javaEmitsStyleFieldsOnTranslateScoreAndSelector() {
        val components = listOf(
            TextComponent(ComponentType.TRANSLATE, "chat.type.text", style = ComponentStyle(color = "red", bold = true)),
            TextComponent(ComponentType.SCORE, "Steve:kills", style = ComponentStyle(color = "gold")),
            TextComponent(ComponentType.SELECTOR, "@p", style = ComponentStyle(italic = true))
        )
        val java = TextComponentHelper.convertToJavaJson(components)
        assertTrue(java, java.contains("\"translate\":\"chat.type.text\""))
        assertTrue(java, java.contains("\"color\":\"red\""))
        assertTrue(java, java.contains("\"bold\":true"))
        assertTrue(java, java.contains("\"color\":\"gold\""))
        assertTrue(java, java.contains("\"italic\":true"))
    }

    @Test
    fun bedrockUsesSectionCodesForTextAndWarnsForStructuralComponents() {
        val textWarnings = mutableListOf<String>()
        val text = TextComponent(
            ComponentType.TEXT, "Hello",
            style = ComponentStyle(color = "red", bold = true)
        )
        val bedrockText = TextComponentHelper.convertToBedrockJson(listOf(text), warnings = textWarnings)
        assertTrue(bedrockText, bedrockText.contains("§c§lHello"))
        assertTrue(textWarnings.isEmpty())

        val warnings = mutableListOf<String>()
        val translate = TextComponent(
            ComponentType.TRANSLATE, "chat.type.text",
            style = ComponentStyle(color = "red")
        )
        TextComponentHelper.convertToBedrockJson(listOf(translate), warnings = warnings)
        assertTrue(warnings.any { it.contains("translate") && it.contains("样式") })
    }

    @Test
    fun structuralKeysAreNotKeptAsStyle() {
        val r = CommandInputParser.parse(
            """{"text":"A","extra":[{"text":"B"}]}"""
        )!!
        val java = TextComponentHelper.convertToJavaJson(r.components)
        assertEquals("""{"text":"AB"}""", java)
        assertTrue(r.components.none { it.style.extra.has("extra") })

        val tr = CommandInputParser.parse(
            """{"translate":"k","with":["a"],"fallback":"f"}"""
        )!!
        val style = tr.components.single().style
        assertEquals(0, style.extra.size())
    }

    @Test
    fun scoreNameWithCommaAndColonSurvivesRoundTrip() {
        val r = CommandInputParser.parse(
            """{"score":{"name":"@e[tag=a,limit=1]","objective":"obj"}}"""
        )!!
        val java = com.google.gson.JsonParser.parseString(r.javaJson).asJsonObject
        val score = java.getAsJsonObject("score")
        assertEquals("@e[tag=a,limit=1]", score.get("name").asString)
        assertEquals("obj", score.get("objective").asString)

        val nbt = CommandInputParser.parse(
            """{"score":{"name":"@e[nbt={id:\"minecraft:pig\"}]","objective":"obj"}}"""
        )!!
        val nbtJava = com.google.gson.JsonParser.parseString(nbt.javaJson).asJsonObject
        assertEquals(
            """@e[nbt={id:"minecraft:pig"}]""",
            nbtJava.getAsJsonObject("score").get("name").asString
        )
    }

    @Test
    fun keybindAndNbtComponentsRoundTrip() {
        val keybind = CommandInputParser.parse(
            """{"keybind":"key.jump","color":"red"}"""
        )!!
        assertEquals(ComponentType.KEYBIND, keybind.components.single().type)
        assertTrue(keybind.javaJson, keybind.javaJson.contains("\"keybind\":\"key.jump\""))
        assertTrue(keybind.javaJson, keybind.javaJson.contains("\"color\":\"red\""))

        val nbt = CommandInputParser.parse(
            """{"nbt":"Health","interpret":true,"source":"entity","separator":", "}"""
        )!!
        val nbtComponent = nbt.components.single()
        assertEquals(ComponentType.NBT, nbtComponent.type)
        val markers = TextComponentHelper.componentsToText(nbt.components)
        val parsed = TextComponentHelper.parseTextComponents(markers).single()
        assertEquals("Health", TextComponentHelper.parseNbtContent(parsed.content)?.get("path")?.asString)
        assertTrue(nbt.javaJson, nbt.javaJson.contains("\"nbt\":\"Health\""))
        assertTrue(nbt.javaJson, nbt.javaJson.contains("\"interpret\":true"))

        val entityNbt = CommandInputParser.parse(
            """{"nbt":"Health","entity":"@s"}"""
        )!!
        assertTrue(entityNbt.javaJson, entityNbt.javaJson.contains("\"entity\":\"@s\""))

        val warnings = mutableListOf<String>()
        TextComponentHelper.convertToBedrockJson(nbt.components, warnings = warnings)
        assertTrue(warnings.any { it.contains("nbt") })
    }

    @Test
    fun selectorSeparatorCanCarryComponentStyle() {
        val r = CommandInputParser.parse(
            """{"selector":"@a","separator":{"text":" | ","color":"red"}}"""
        )!!
        val java = com.google.gson.JsonParser.parseString(r.javaJson).asJsonObject
        val separator = java.getAsJsonObject("separator")
        assertEquals(" | ", separator.get("text").asString)
        assertEquals("red", separator.get("color").asString)
    }

    @Test
    fun sectionCodesInsideWithJsonAreConvertedForJava() {
        val comp = TextComponentHelper.TextComponent(
            ComponentType.TRANSLATE,
            "chat.type.text",
            mutableListOf(
                TextComponentHelper.SubComponent(
                    TextComponentHelper.SubComponentType.WITH_JSON,
                    """[{"text":"§cHi"}]"""
                )
            )
        )
        val java = TextComponentHelper.convertToJavaJson(listOf(comp))
        assertTrue(java, java.contains("\"text\":\"Hi\""))
        assertTrue(java, java.contains("\"color\":\"red\""))
    }

    @Test
    fun parserKeepsStyleOnTranslateInsteadOfDroppingIt() {
        val r = CommandInputParser.parse("""{"translate":"chat.type.text","color":"red","bold":true}""")!!
        val component = r.components.single()
        assertEquals("red", component.style.color)
        assertEquals(true, component.style.bold)
        assertTrue(r.javaJson, r.javaJson.contains("\"color\":\"red\""))
        assertTrue(r.warnings.joinToString("\n"), r.warnings.none { it.contains("样式字段暂未保留") })
    }

    @Test
    fun textStyleIsNotDoubleAppliedInBedrock() {
        val r = CommandInputParser.parse("""{"text":"A","color":"red","extra":[{"text":"B"}]}""")!!
        val bedrock = TextComponentHelper.convertToBedrockJson(r.components)
        assertTrue(bedrock, bedrock.contains("§cA"))
        assertTrue(bedrock, bedrock.contains("§cB"))
    }

    @Test
    fun simpleTranslateTextStyleUsesFieldsAndBedrockCodes() {
        val table = SimpleTranslatePlanner.Table(
            rows = listOf(
                SimpleTranslatePlanner.Row(
                    params = "tag=a",
                    content = listOf(
                        WithArgDraft.Text("A", color = "red", bold = true)
                    )
                )
            )
        )
        val warnings = mutableListOf<String>()
        val result = SimpleTranslatePlanner.plan(
            table = table,
            javaMessageJson = """{"translate":"__simpler_translate__"}""",
            bedrockMessageJson = """{"rawtext":[{"translate":"__simpler_translate__"}]}""",
            javaSelector = "@a",
            bedrockSelector = "@a",
            javaPrefix = null,
            bedrockPrefix = null,
            includeDefaultFallback = false,
            context = null,
            nbtSyntax = VersionDiff.NbtSyntax.MODERN,
            target = null,
            mNHandling = "font",
            mnCFEnabled = false,
            separatorAsTextComponent = false,
            bedrockColorToRgb = true,
            warnings = warnings
        )
        assertTrue(result.java.single(), result.java.single().contains("\"color\":\"red\""))
        assertTrue(result.java.single(), result.java.single().contains("\"bold\":true"))
        assertTrue(result.bedrock.single(), result.bedrock.single().contains("§c§lA"))
        assertTrue(warnings.joinToString("\n"), warnings.none { it.contains("无法表达") })
    }
}
