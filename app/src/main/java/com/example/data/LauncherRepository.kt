package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class LauncherRepository(private val dao: LauncherDao) {

    val allAppUsageStats: Flow<List<AppUsageStats>> = dao.getAllAppUsageStats()
    val allNotes: Flow<List<LauncherNote>> = dao.getAllNotes()
    val allSettings: Flow<List<LauncherSetting>> = dao.getAllSettings()

    suspend fun getAppUsage(packageName: String): AppUsageStats? {
        return dao.getAppUsage(packageName)
    }

    suspend fun recordAppLaunch(packageName: String, appName: String, category: String) {
        val existing = dao.getAppUsage(packageName)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val newUsage = if (existing != null) {
            // Update hourly history
            val hours = existing.hourlyHistory.split(",").map { it.toIntOrNull() ?: 0 }.toMutableList()
            if (currentHour in 0..23) {
                hours[currentHour] = hours[currentHour] + 1
            }
            val newHourlyString = hours.joinToString(",")
            
            existing.copy(
                launchCount = existing.launchCount + 1,
                lastLaunchedTime = System.currentTimeMillis(),
                hourlyHistory = newHourlyString
            )
        } else {
            val hours = MutableList(24) { 0 }
            if (currentHour in 0..23) {
                hours[currentHour] = 1
            }
            AppUsageStats(
                packageName = packageName,
                appName = appName,
                category = category,
                launchCount = 1,
                lastLaunchedTime = System.currentTimeMillis(),
                hourlyHistory = hours.joinToString(","),
                isHidden = false,
                isFavorite = false
            )
        }
        dao.insertOrUpdateAppUsage(newUsage)
    }

    suspend fun setAppHidden(packageName: String, isHidden: Boolean) {
        dao.updateAppHiddenStatus(packageName, isHidden)
    }

    suspend fun setAppFavorite(packageName: String, isFavorite: Boolean) {
        dao.updateAppFavoriteStatus(packageName, isFavorite)
    }

    suspend fun insertNote(note: LauncherNote) {
        dao.insertNote(note)
    }

    suspend fun deleteNote(note: LauncherNote) {
        dao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        dao.deleteNoteById(id)
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(LauncherSetting(key, value))
    }

    suspend fun getSettingValue(key: String): String? {
        return dao.getSettingValue(key)
    }
}
