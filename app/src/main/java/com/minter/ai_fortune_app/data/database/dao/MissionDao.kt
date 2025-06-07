//package com.minter.ai_fortune_app.data.database.dao
//
//import androidx.room.*
//import com.minter.ai_fortune_app.data.database.MissionEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface MissionDao {
//
//    @Query("SELECT * FROM missions ORDER BY createdDate DESC")
//    fun getAllMissions(): Flow<List<MissionEntity>>
//
//    @Query("SELECT * FROM missions WHERE id = :id")
//    suspend fun getMissionById(id: String): MissionEntity?
//
//    @Query("SELECT * FROM missions WHERE createdDate = :date ORDER BY createdDate DESC LIMIT 1")
//    suspend fun getTodayMission(date: String): MissionEntity?
//
//    @Query("SELECT * FROM missions WHERE status = :status ORDER BY createdDate DESC")
//    suspend fun getMissionsByStatus(status: String): List<MissionEntity>
//
//    @Query("SELECT * FROM missions WHERE status IN ('NOT_STARTED', 'RECOMMENDED', 'ACCEPTED') AND createdDate != :today")
//    suspend fun getIncompleteMissions(today: String): List<MissionEntity>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertMission(mission: MissionEntity)
//
//    @Update
//    suspend fun updateMission(mission: MissionEntity)
//
//    @Delete
//    suspend fun deleteMission(mission: MissionEntity)
//
//    @Query("DELETE FROM missions WHERE id = :id")
//    suspend fun deleteMissionById(id: String)
//
//    @Query("DELETE FROM missions WHERE status IN ('NOT_STARTED', 'RECOMMENDED', 'ACCEPTED') AND createdDate != :today")
//    suspend fun deleteIncompleteMissions(today: String)
//
//    @Query("DELETE FROM missions")
//    suspend fun deleteAllMissions()
//}