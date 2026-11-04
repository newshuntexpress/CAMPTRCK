package com.example.data.model

import com.google.firebase.Timestamp

data class AppUpdateInfo(
    val latestVersionCode: Long = 1L,
    val latestVersionName: String = "1.0",
    val minRequiredVersionCode: Long = 1L,
    val updateTitle: String = "Awazel Camp System Update",
    val releaseNotes: String = "Automatic multi-device synchronization active across all devices.",
    val downloadUrl: String = "",
    val isMandatory: Boolean = false,
    val updatedAt: Timestamp? = null
)
