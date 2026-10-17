package com.tellraw.app.model

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * 内置实体头像：`res/drawable-nodpi/entity_<id>.png`（Minecraft Wiki 的 EntitySprite 渲染图）。
 * 找不到资源时返回 null，由界面回退到"分类色 + 首字"。
 */
object EntityAvatars {

    private val cache = mutableMapOf<String, ImageBitmap?>()

    fun load(context: Context, id: String): ImageBitmap? {
        val key = EntityIdAliases.canonical(id)
        if (cache.containsKey(key)) return cache[key]
        val name = "entity_" + key.removePrefix("minecraft:").replace('-', '_')
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        val bitmap = if (resId == 0) null else runCatching {
            BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
        }.getOrNull()
        cache[key] = bitmap
        return bitmap
    }
}
