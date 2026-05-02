package com.example.datausagemonitor.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLimitDao {
    @Query("SELECT * FROM usage_limits")
    fun getAllLimits(): Flow<List<UsageLimit>>

    @Query("SELECT * FROM usage_limits WHERE networkType = :networkType")
    suspend fun getLimitForNetwork(networkType: Int): UsageLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: UsageLimit)

    @Query("UPDATE usage_limits SET isEnabled = :enabled WHERE networkType = :networkType")
    suspend fun setEnabled(networkType: Int, enabled: Boolean)
}
