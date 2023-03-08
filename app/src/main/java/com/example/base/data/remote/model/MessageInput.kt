package com.example.base.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageInput(
    val content: String,
)
