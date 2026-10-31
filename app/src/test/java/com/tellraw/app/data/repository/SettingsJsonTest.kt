package com.tellraw.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsJsonTest {

    @Test
    fun escapeUnescapeRoundTripForTrickyValues() {
        val values = listOf(
            "execute as @a run",
            """execute as @a[nbt={Name:'"Steve"'}] run""",
            """C:\path\to\thing""",
            "line1\nline2\r\nline3\ttabbed",
            "quote\" backslash\\ slash/ unicode\u0FC8\u0F34",
            "\u0001control",
            ""
        )
        for (value in values) {
            assertEquals(value, jsonUnescape(jsonEscape(value)))
        }
    }

    @Test
    fun escapedValueSurvivesConfigExtraction() {
        val prefix = """execute as @a[nbt={CustomName:'{"text":"hi"}'}] run"""
        val json = """
            {
              "execute_prefix_input": "${jsonEscape(prefix)}",
              "as_sensitive": false,
              "target_version": "1.21.5"
            }
        """.trimIndent()
        assertEquals(prefix.trim(), extractConfigValue(json, "execute_prefix_input"))
        assertEquals("false", extractConfigValue(json, "as_sensitive"))
        assertEquals("1.21.5", extractConfigValue(json, "target_version"))
        assertNull(extractConfigValue(json, "missing_key"))
    }

    @Test
    fun newlineInValueDoesNotBreakConfigExtraction() {
        val prefix = "execute\nas @a\nrun"
        val json = """{ "execute_prefix_input": "${jsonEscape(prefix)}", "as_sensitive": true }"""
        assertEquals(prefix, extractConfigValue(json, "execute_prefix_input"))
        assertEquals("true", extractConfigValue(json, "as_sensitive"))
    }
}
