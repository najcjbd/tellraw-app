package com.tellraw.app.mctest

import com.google.gson.GsonBuilder
import com.tellraw.app.util.TextFormatter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * C 第 2 步·生成器：用**程序真实生成的**双版本命令产出 `cases.json`，
 * 交给 `tools/mctest/run_cases.py` 在各版本真机验证（"程序该怎么做 / 有没有做对"）。
 *
 * 纯 JVM：文本转换的 `TextFormatter.convertTo*Json(..., context = null)` 不需要 Android Context，
 * 所以本地(aarch64)和 CI 都能跑；不改动任何 app 行为，只是把它的输出誊出来。
 */
class McCaseGeneratorTest {

    private val messages = listOf(
        "hi",
        "a b c",
        "§cRed§aGreen",
        "§gGold",          // 基岩独有色：Java 侧应出精确 RGB，基岩侧保留 §g
        "§lBold§rPlain",
        "中文测试"
    )

    @Test
    fun generateCases() {
        val cases = messages.map { m ->
            val java = "tellraw @a " + TextFormatter.convertToJavaJson(m, "font", false)
            val bedrock = "tellraw @a " + TextFormatter.convertToBedrockJson(m, "font", false)
            linkedMapOf(
                "name" to ("msg-" + m.filter { it.isLetterOrDigit() }.ifBlank { "blank" }),
                "edition" to "both",
                "java" to java,
                "bedrock" to bedrock,
                "expectChat" to stripSectionCodes(m)
            )
        }
        val out = File("build/mctest/cases.json")
        out.parentFile.mkdirs()
        out.writeText(GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(cases))
        assertTrue("cases.json 应写入且非空", out.exists() && out.length() > 0)
    }

    /** 客户端渲染后会剥掉 § 码，所以"bot 收到的可见文本"= 去掉所有 `§x`。 */
    private fun stripSectionCodes(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '§' && i + 1 < s.length) i += 2 else sb.append(s[i++])
        }
        return sb.toString()
    }
}
