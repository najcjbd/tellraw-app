package com.tellraw.app.mctest

import android.content.Context
import com.google.gson.GsonBuilder
import com.tellraw.app.TestApplication
import com.tellraw.app.model.SelectorType
import com.tellraw.app.util.SelectorConverter
import com.tellraw.app.util.VersionDiff
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * C-后续·生成器（选择器/物品语料）：用**程序真实输出**的 Java 选择器产出 `cases_sel.json`。
 * 需要 Android Context（Robolectric），所以这半在 **CI** 上跑；语料交给 tools/mctest/run_cases.py 真机验证。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class, packageName = "com.tellraw.app")
class McSelectorCaseGeneratorTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun generate() {
        val out = mutableListOf<Map<String, Any>>()

        fun case(name: String, bedrock: String, setUp: List<String>, match: Boolean) {
            val java = SelectorConverter.filterSelectorParameters(
                bedrock, SelectorType.JAVA, context, VersionDiff.NbtSyntax.MODERN
            ).first
            out.add(
                linkedMapOf(
                    "name" to name,
                    "edition" to "java",
                    "setup" to setUp,
                    "java" to "execute if entity $java run tellraw @a {\"text\":\"$name\"}",
                    "expectChat" to name,
                    "expect" to (if (match) "present" else "absent"),
                    "versions" to listOf(">=1.20.5")
                )
            )
        }

        case("msel-hotbar-hit", "@a[hasitem={item=diamond,location=slot.hotbar,slot=0}]",
            listOf("item replace entity <bot> hotbar.0 with minecraft:diamond 1"), true)
        case("msel-hotbar-miss", "@a[hasitem={item=diamond,location=slot.hotbar,slot=0}]",
            listOf("clear @a"), false)
        case("msel-armor-helmet-hit", "@a[hasitem={item=diamond_helmet,location=slot.armor.head}]",
            listOf("item replace entity <bot> armor.head with minecraft:diamond_helmet 1"), true)
        case("msel-mainhand-sword-hit", "@a[hasitem={item=diamond_sword,location=slot.weapon.mainhand,slot=0}]",
            listOf("item replace entity <bot> weapon.mainhand with minecraft:diamond_sword 1"), true)

        val f = File("build/mctest/cases_sel.json")
        f.parentFile.mkdirs()
        f.writeText(GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(out))
        assertTrue("cases_sel.json 应写入且非空", f.exists() && f.length() > 0)
    }
}
