package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FishNetWaypointEntity::class,
        FishCatchLogEntity::class,
        ShipMaintenanceEntity::class,
        EmergencySosEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MaritimeDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun catchLogDao(): CatchLogDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun emergencySosDao(): EmergencySosDao

    companion object {
        @Volatile
        private var INSTANCE: MaritimeDatabase? = null

        fun getDatabase(context: Context): MaritimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaritimeDatabase::class.java,
                    "nusantara_maritime_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
