package com.tellraw.app.data.repository

import com.tellraw.app.TestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * 回归测试：`HistoryRepository` 与 [SettingsRepository] 共用 `tellraw_config.json`，
 * 它保存时**只能改历史两个键，必须保留其它键**（否则"清空全部历史"会把别的 8 项设置冲掉）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class, packageName = "com.tellraw.app")
class HistoryRepositoryConfigTest {

    private lateinit var context: android.content.Context
    private lateinit var configFile: File

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        configFile = File(context.filesDir, "tellraw_config.json")
        if (configFile.exists()) configFile.delete()
    }

    @Test
    fun testSaveConfigKeepsOtherKeys() = runBlocking {
        // 预置一份"设置文件"（模拟 SettingsRepository 已写入的内容）
        configFile.writeText(
            """
            {
              "nbt_syntax": "legacy",
              "mn_mixed_mode": true
            }
            """.trimIndent()
        )

        val repo = HistoryRepository(context)
        repo.setStorageUri("content://test/dir")
        repo.setStorageFilename("MyHistory.txt")

        val after = configFile.readText()
        // 关键：别的键必须还在
        assertTrue("不应冲掉 nbt_syntax：$after", after.contains("\"nbt_syntax\": \"legacy\""))
        assertTrue("不应冲掉 mn_mixed_mode：$after", after.contains("\"mn_mixed_mode\": true"))
        // 历史两个键必须更新
        assertTrue("应写入 history_storage_uri：$after", after.contains("\"history_storage_uri\": \"content://test/dir\""))
        assertTrue("应写入 history_storage_filename：$after", after.contains("\"history_storage_filename\": \"MyHistory.txt\""))
    }

    @Test
    fun testSaveConfigOnEmptyFileCreatesBothKeys() = runBlocking {
        val repo = HistoryRepository(context)
        repo.setStorageUri("content://x")
        repo.setStorageFilename("h.txt")

        val after = configFile.readText()
        assertTrue(after.contains("\"history_storage_uri\": \"content://x\""))
        assertTrue(after.contains("\"history_storage_filename\": \"h.txt\""))
        assertFalse("空文件下不该凭空多出别的键", after.contains("nbt_syntax"))
    }
}
