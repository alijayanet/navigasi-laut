package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waypoints")
data class FishNetWaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "SPOT_IKAN", "JARING_HANYUT", "RUMPON", "BUBU_RAWAI", "KARANG_DANGKAL"
    val latitude: Double,
    val longitude: Double,
    val depthMeters: Double = 0.0,
    val baitOrGearNotes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val targetRetrievalDate: String = "", // e.g., "Besok Pagi (05:30 WIB)"
    val driftDistanceMeters: Double = 0.0, // Estimasi pergeseran jaring oleh arus laut
    val currentHeadingDeg: Float = 0f,
    val isRetrieved: Boolean = false,
    val tagColor: String = "#00B4D8"
)

@Entity(tableName = "catch_logs")
data class FishCatchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStr: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fishSpecies: String, // "Tongkol", "Cakalang", "Tuna Sirip Kuning", "Kembung", "Teri", "Kakap Merah", "Lobster", "Cumi-cumi", "Tenggiri"
    val weightKg: Double,
    val pricePerKg: Double,
    val estimatedRevenue: Double,
    val fishingGear: String, // "Jaring Insang (Gillnet)", "Pancing Ulur (Handline)", "Purse Seine", "Bubu Dasar"
    val wppnriZone: String, // "WPP 711", "WPP 712", etc.
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val isSustainableQuotaCompliant: Boolean = true
)

@Entity(tableName = "ship_maintenances")
data class ShipMaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentName: String, // "Ganti Oli Mesin Diesel", "Filter Solar & Pembersih Tangki", "Impeller Pompa Air Laut Pendingin", "Aki & Sistem Kelistrikan", "Pembersihan Teritip/Lambung Kapal", "Pemeriksaan Propeller / As Baling-Baling", "Pemeriksaan Pelampung (Life Jacket) & Suar Darurat (Flare)"
    val currentEngineHours: Int,
    val intervalHours: Int,
    val nextDueEngineHours: Int,
    val lastServiceDate: String,
    val isOverdue: Boolean = false,
    val maintenanceGuide: String = "",
    val notes: String = ""
)

@Entity(tableName = "emergency_sos_records")
data class EmergencySosEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val distressType: String, // "MESIN_MATI", "KEBOCORAN_LAMBUNG", "ORANG_JATUH_LAUT", "CUACA_EKSTREM", "KEBAKARAN", "MEDIS"
    val latitude: Double,
    val longitude: Double,
    val vesselName: String,
    val captainName: String,
    val crewOnBoard: Int,
    val satelliteRelayStatus: String, // "TERKIRIM (SAT-EPIRB / BASARNAS 115)", "TERSIMPAN OFFLINE (ANTRIAN SMS)"
    val notes: String
)
