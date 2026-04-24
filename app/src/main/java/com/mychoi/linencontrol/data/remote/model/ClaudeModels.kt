package com.mychoi.linencontrol.data.remote.model

import com.google.gson.annotations.SerializedName

data class ClaudeRequest(
    val model: String = "claude-sonnet-4-6",
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val messages: List<Message>
) {
    data class Message(
        val role: String = "user",
        val content: List<ContentItem>
    )
}

data class ContentItem(
    val type: String,
    val text: String? = null,
    val source: ImageSource? = null
)

data class ImageSource(
    val type: String = "base64",
    @SerializedName("media_type") val mediaType: String = "image/jpeg",
    val data: String
)

data class ClaudeResponse(
    val content: List<ContentBlock>
) {
    data class ContentBlock(
        val type: String,
        val text: String?
    )

    fun getText(): String = content.firstOrNull { it.type == "text" }?.text ?: ""
}
