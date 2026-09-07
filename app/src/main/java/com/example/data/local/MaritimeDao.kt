package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints ORDER BY isRetrieved ASC, timestamp DESC")
    fun getAllWaypoints(): Flow<List<FishNetWaypointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: FishNetWaypointEntity): Long

    @Update
    suspend fun updateWaypoint(waypoint: FishNetWaypointEntity)

    @Delete
    suspend fun deleteWaypoint(waypoint: FishNetWaypointEntity)

    @Query("UPDATE waypoints SET isRetrieved = :isRetrieved WHERE id = :id")
    suspend fun updateRetrievedStatus(id: Long, isRetrieved: Boolean)
}

@Dao
interface CatchLogDao {
    @Query("SELECT * FROM catch_logs ORDER BY timestamp DESC")
    fun getAllCatchLogs(): Flow<List<FishCatchLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatchLog(log: FishCatchLogEntity): Long

    @Delete
    suspend fun deleteCatchLog(log: FishCatchLogEntity)

    @Query("SELECT SUM(weightKg) FROM catch_logs")
    fun getTotalCatchWeightKg(): Flow<Double?>

    @Query("SELECT SUM(estimatedRevenue) FROM catch_logs")
    fun getTotalRevenue(): Flow<Double?>
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM ship_maintenances ORDER BY nextDueEngineHours ASC")
    fun getAllMaintenances(): Flow<List<ShipMaintenanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(item: ShipMaintenanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShipMaintenanceEntity>)

    @Update
    suspend fun updateMaintenance(item: ShipMaintenanceEntity)

    @Delete
    suspend fun deleteMaintenance(item: ShipMaintenanceEntity)

    @Query("SELECT COUNT(*) FROM ship_maintenances")
    suspend fun getMaintenanceCount(): Int
}

@Dao
interface EmergencySosDao {
    @Query("SELECT * FROM emergency_sos_records ORDER BY timestamp DESC")
    fun getAllSosRecords(): Flow<List<EmergencySosEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSosRecord(record: EmergencySosEntity): Long

    @Delete
    suspend fun deleteSosRecord(record: EmergencySosEntity)
}
