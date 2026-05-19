package com.example.spotifish.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackEventRequest(
    val songId: String,
    val eventType: String,
    val positionMs: Long = 0L,
)
