package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_stats")
data class AppUsageStats(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String, // Social, Work, Study, Games, Tools
    val launchCount: Int,
    val lastLaunchedTime: Long,
    val hourlyHistory: String = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", // CSV for 24 hours
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false
)

@Entity(tableName = "launcher_notes")
data class LauncherNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "launcher_settings")
data class LauncherSetting(
    @PrimaryKey val key: String,
    val value: String
)
