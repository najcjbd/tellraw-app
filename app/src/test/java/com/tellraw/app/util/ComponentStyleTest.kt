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
