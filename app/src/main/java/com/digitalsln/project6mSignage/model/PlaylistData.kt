package com.digitalsln.project6mSignage.model

data class PlaylistData(

    val id: Int,
    val contentType: Int,
    val slideContentUrl: String,
    val autoReplayVideo: Boolean,
    val interval: Int,
    val fromDate: String?,
    val toDate: String?,
    val fromTime: String?,
    val toTime: String?,
    val days: Int?
)