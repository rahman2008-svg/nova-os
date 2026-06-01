package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    // App Usage Stats Queries
    @Query("SELECT * FROM app_usage_stats ORDER BY launchCount DESC")
    fun getAllAppUsageStats(): Flow<List<AppUsageStats>>

    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppUsage(packageName: String): AppUsageStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppUsage(appUsageStats: AppUsageStats)

    @Query("UPDATE app_usage_stats SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun updateAppHiddenStatus(packageName: String, isHidden: Boolean)

    @Query("UPDATE app_usage_stats SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun updateAppFavoriteStatus(packageName: String, isFavorite: Boolean)

    // Launcher Notes Queries
    @Query("SELECT * FROM launcher_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<LauncherNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: LauncherNote)

    @Delete
    suspend fun deleteNote(note: LauncherNote)

    @Query("DELETE FROM launcher_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Launcher Settings Queries
    @Query("SELECT * FROM launcher_settings")
    fun getAllSettings(): Flow<List<LauncherSetting>>

    @Query("SELECT value FROM launcher_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: LauncherSetting)
}
