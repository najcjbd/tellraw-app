package com.tellraw.app.util

import com.google.gson.JsonParser
import com.tellraw.app.util.TextComponentHelper.ComponentType
import com.tellraw.app.util.TextComponentHelper.SubComponent
import com.tellraw.app.util.TextComponentHelper.SubComponentType
import com.tellraw.app.util.TextComponentHelper.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WithArgDraftCodecTest {

    private fun json(s: String) = JsonParser.parseString(s)
    private fun roundTrip(draft: WithArgDraft) = WithArgCodec.fromJson(draft.toJson())

    @Test
    fun textRoundTripPreservesStyles() {
        val draft = WithArgDraft.Text(
            text = "Steve",
            color = "red",
            bold = true,
            italic = false,
            underlined = true,
            strikethrough = false,
            obfuscated = true
        )
        val back = roundTrip(draft) as WithArgDraft.Text
        assertEquals("Steve", back.text)
        assertEquals("red", back.color)
        assertEquals(true, back.bold)
        assertEquals(false, back.italic)
        assertEquals(true, back.underlined)
        assertEquals(false, back.strikethrough)
        assertEquals(true, back.obfuscated)
    }

    @Test
    fun falseBoldIsDistinctFromAbsentBold() {
        val withFalse = WithArgDraft.Text("a", bold = false).toJson().asJsonObject
        val absent = WithArgDraft.Text("a", bold = null).toJson().asJsonObject
        assertTrue(withFalse.has("bold"))
        assertFalse(withFalse.get("bold").asBoolean)
        assertFalse(absent.has("bold"))
    }

    @Test
    fun unknownFieldsRetainedOnTextItem() {
        val source = json(
            """{"text":"a","color":"red","clickEvent":{"action":"run_command","value":"/say hi"},"hoverEvent":{"action":"show_text","contents":"x"},"font":"minecraft:uniform","insertion":"z","extra":[{"text":"b"}]}"""
        )
        val text = WithArgCodec.fromJson(source) as WithArgDraft.Text
        val out = text.toJson().asJsonObject
        assertEquals("red", out.get("color").asString)
        assertEquals("/say hi", out.getAsJsonObject("clickEvent").get("value").asString)
        assertEquals("minecraft:uniform", out.get("font").asString)
        assertEquals("z", out.get("insertion").asString)
        assertEquals(1, out.getAsJsonArray("extra").size())
    }

    @Test
    fun clearingManagedFieldRemovesRetainedKey() {
        val source = json("""{"text":"a","color":"red","bold":true}""")
        val text = WithArgCodec.fromJson(source) as WithArgDraft.Text
        val out = text.copy(color = null, bold = null).toJson().asJsonObject
        assertFalse(out.has("color"))
        assertFalse(out.has("bold"))
    }

    @Test
    fun nestedTranslateRoundTripSupportsDeepNesting() {
        val deepest = WithArgDraft.Text("Alex", color = "gold")
        val inner = WithArgDraft.Translate("chat.type.text", args = listOf(deepest))
        val outer = WithArgDraft.Translate("key.outer", fallback = "fallback", args = listOf(inner))
        val back = roundTrip(outer) as WithArgDraft.Translate
        assertEquals("key.outer", back.key)
        assertEquals("fallback", back.fallback)
        val backInner = back.args.single() as WithArgDraft.Translate
        assertEquals("chat.type.text", backInner.key)
        val backText = backInner.args.single() as WithArgDraft.Text
        assertEquals("Alex", backText.text)
        assertEquals("gold", backText.color)
    }

    @Test
    fun nestedTranslateWithoutArgsOmitsWith() {
        val out = WithArgDraft.Translate("key").toJson().asJsonObject
        assertFalse(out.has("with"))
        assertFalse(out.has("fallback"))
    }

    @Test
    fun selectorScoreKeybindNbtRoundTrip() {
        val args = listOf(
            WithArgDraft.Selector("@a[tag=x]", separator = ", "),
            WithArgDraft.Score("Steve", "kills"),
            WithArgDraft.Keybind("key.jump"),
            WithArgDraft.Nbt("Health", source = "entity", interpret = true, separator = "|")
        )
        val back = WithArgCodec.parseArgs(WithArgCodec.serialize(args))
        assertEquals(args, back)
    }

    @Test
    fun rawPrimitiveArrayAndUnknownObjectPassThrough() {
        val items = listOf(
            WithArgDraft.Raw(json("\"plain\"")),
            WithArgDraft.Raw(json("42")),
            WithArgDraft.Raw(json("""[{"text":"a"}]""")),
            WithArgDraft.Raw(json("""{"custom":{"x":1}}"""))
        )
        val back = WithArgCodec.parseArgs(WithArgCodec.serialize(items))
        assertEquals(items, back)
    }

    @Test
    fun objectArgsAreWrappedIntoArray() {
        val normalized = WithArgCodec.normalizeArgsJson("""{"text":"a"}""")
        assertEquals("""[{"text":"a"}]""", normalized)
    }

    @Test
    fun emptyArrayIsAllowed() {
        assertEquals("[]", WithArgCodec.normalizeArgsJson("[]"))
        assertTrue(WithArgCodec.parseArgs("[]").isEmpty())
    }

    @Test
    fun invalidArgsRejected() {
        for (bad in listOf("", "{", "\"abc\"", "null", "[null]", "[{\"text\":\"a\"}] junk")) {
            assertThrows(bad, WithArgCodec.WithArgsFormatException::class.java) {
                WithArgCodec.normalizeArgsJson(bad)
            }
        }
        assertThrows(WithArgCodec.WithArgsFormatException::class.java) {
            WithArgCodec.parseRaw("null")
        }
    }

    @Test
    fun prettyPrintParsesBackToSameJson() {
        val args = listOf(
            WithArgDraft.Translate("k", args = listOf(WithArgDraft.Text("a", bold = true))),
            WithArgDraft.Selector("@a")
        )
        val pretty = WithArgCodec.serializePretty(args)
        assertEquals(WithArgCodec.serialize(args), WithArgCodec.normalizeArgsJson(pretty))
    }

    @Test
    fun wrongTypedManagedFieldFallsBackToRaw() {
        val raw = WithArgCodec.fromJson(json("""{"text":123}"""))
        assertTrue(raw is WithArgDraft.Raw)
        val rawBool = WithArgCodec.fromJson(json("""{"text":"a","bold":"yes"}"""))
        assertTrue(rawBool is WithArgDraft.Raw)
        // 非数组 with 也原样保留，不丢弃
        val rawWith = WithArgCodec.fromJson(json("""{"translate":"k","with":"x"}"""))
        assertTrue(rawWith is WithArgDraft.Raw)
    }

    @Test
    fun sensitiveCharactersSurviveMarkerRoundTrip() {
        val json = """[{"text":"a_b\\cྈd༴e__f"},{"translate":"k","with":[{"text":"x_y"}]}]"""
        val component = TextComponent(
            ComponentType.TRANSLATE, "key",
            mutableListOf(SubComponent(SubComponentType.WITH_JSON, json))
        )
        val markers = TextComponentHelper.componentsToText(listOf(component))
        val parsed = TextComponentHelper.parseTextComponents(markers).single()
        val stored = parsed.subComponents.single { it.type == SubComponentType.WITH_JSON }.content
        assertEquals(json, stored)
        assertEquals(WithArgCodec.normalizeArgsJson(json), WithArgCodec.normalizeArgsJson(stored))
    }

    @Test
    fun bedrockIssuesDetectLimitedKinds() {
        val args = listOf(
            WithArgDraft.Text("a", color = "red"),
            WithArgDraft.Keybind("key.jump"),
            WithArgDraft.Nbt("Health"),
            WithArgDraft.Translate("k", fallback = "f", args = listOf(WithArgDraft.Text("x"))),
            WithArgDraft.Selector("@a", separator = ",")
        )
        val issues = WithArgCodec.bedrockIssues(args)
        assertTrue(issues.contains(WithArgCodec.BedrockIssue.STYLES))
        assertTrue(issues.contains(WithArgCodec.BedrockIssue.KEYBIND))
        assertTrue(issues.contains(WithArgCodec.BedrockIssue.NBT))
        assertTrue(issues.contains(WithArgCodec.BedrockIssue.FALLBACK))
        assertTrue(issues.contains(WithArgCodec.BedrockIssue.SELECTOR_SEPARATOR))
    }
}
