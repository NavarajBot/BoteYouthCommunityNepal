package com.example.data

data class AppUpdateInfo(
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val releaseNotes: String = "",
    val isMandatory: Boolean = false,
    val downloadUrl: String = ""
)
