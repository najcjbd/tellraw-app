package com.tellraw.app.ui.viewmodel

import com.tellraw.app.util.WithArgDraft

enum class WithBuilderMode { VISUAL, JSON }

/**
 * translate 的 with 参数构造器状态。
 * [targetPosition] 是打开时消息框光标位置（纯文本坐标）。
 */
data class WithBuilderState(
    val targetPosition: Int,
    val isNew: Boolean,
    val key: String,
    val fallback: String,
    val args: List<WithArgDraft>,
    val mode: WithBuilderMode,
    val rawJson: String,
    val rawError: String? = null
)
