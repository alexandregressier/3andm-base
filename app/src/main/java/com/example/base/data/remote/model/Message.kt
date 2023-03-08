package com.example.base.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val author: String,
    val content: String,
)
