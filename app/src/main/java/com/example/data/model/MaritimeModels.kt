package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class VesselType(val label: String, val color: Color) {
    FISHING("Kapal Nelayan", FishingColor),
    CARGO("Kapal Kargo", CargoColor),
    TANKER("Kapal Tanker", TankerColor),
    PASSENGER("Kapal Penumpang / Ferry", PassengerColor),
    PATROL("Patroli BASARNAS / TNI AL", PatrolColor),
    TUGBOAT("Tugboat / Pandu", TugColor)
}

enum class SeaWaveCategory(val label: String, val minHeight: Float, val maxHeight: Float, val color: Color, val advice: String) {
    TENANG("Tenang (0.1 - 0.5 m)", 0.1f, 0.5f, SuccessGreen, "Sangat aman untuk seluruh jenis perahu nelayan & kapal pelabuhan"),
    RENDAH("Rendah (0.5 - 1.25 m)", 0.5f, 1.25f, SuccessGreen, "Aman untuk melaut, tetap perhatikan perubahan angin lokal"),
    SEDANG("Sedang (1.25 - 2.5 m)", 1.25f, 2.5f, WarningAmber, "Waspada untuk perahu jukung & kapal nelayan < 10 GT"),
    TINGGI("Tinggi (2.5 - 4.0 m)", 2.5f, 4.0f, WarningOrange, "Bahaya untuk kapal nelayan < 30 GT & tongkang. Hindari melaut!"),
    SANGAT_TINGGI("Sangat Tinggi (4.0 - 6.0 m)", 4.0f, 6.0f, DangerRed, "Dilarang keras melaut! Berbahaya bagi ferry & kapal kargo kecil"),
    EKSTREM("Ekstrem (> 6.0 m)", 6.0f, 10.0f, DangerRed, "Peringatan Siaga Merah BMKG. Bahaya bagi seluruh pelayaran!")
}

enum class MelautStatus(val label: String, val badgeColor: Color, val iconDescription: String) {
    AMAN("AMAN MELAUT", SuccessGreen, "Kondisi laut aman & kondusif"),
    WASPADA("WASPADA KAPAL KECIL", WarningAmber, "Perahu < 10 GT waspada gelombang & angin kencang"),
    BAHAYA("DILARANG MELAUT", DangerRed, "Peringatan BMKG: Cuaca buruk & gelombang tinggi!")
}

data class Vessel(
    val id: String,
    val name: String,
    val callSign: String,
    val mmsi: String,
    val type: VesselType,
    val latitude: Double,
    val longitude: Double,
    val speedKnots: Double,
    val headingDeg: Float,
    val destination: String,
    val status: String,
    val lengthMeters: Int,
    val draughtMeters: Double,
    val grossTonnage: Int,
    val lastUpdated: String,
    val originPort: String,
    val eta: String
)

data class DayForecast(
    val dayName: String,
    val dateStr: String,
    val waveHeight: Float,
    val maxWaveHeight: Float,
    val windSpeedKnots: Int,
    val windDirection: String,
    val condition: String,
    val rainChance: Int,
    val isSafe: Boolean
)

data class MaritimeWeatherArea(
    val id: String,
    val name: String,
    val wppnriZone: String,
    val latitude: Double,
    val longitude: Double,
    val waveHeight: Float,
    val maxWaveHeight: Float,
    val waveCategory: SeaWaveCategory,
    val windSpeedKnots: Int,
    val windDirection: String,
    val windBeaufort: Int,
    val currentSpeedKnots: Double,
    val currentDirection: String,
    val highTideTime: String,
    val highTideHeightM: Float,
    val lowTideTime: String,
    val lowTideHeightM: Float,
    val seaSurfaceTempC: Int,
    val visibilityKm: Int,
    val rainChance: Int,
    val statusMelaut: MelautStatus,
    val bmkgWarning: String?,
    val moonPhase: String,
    val lunarFishingRating: String, // "Sangat Baik (Bintang 5)", "Sedang", etc.
    val forecast7Days: List<DayForecast>
)

data class HighRiskZonePrediction(
    val id: String,
    val regionName: String,
    val wppZone: String,
    val currentWaveM: Float,
    val predictedMaxWaveM: Float,
    val riskLevel: String, // "SIAGA TINGGI", "WASPADA", "SEDANG"
    val riskFactor: String, // "Siklon Tropis", "Angin Muson Tenggara", "Alur Laut Dalam"
    val affectedVesselSizes: String, // "< 30 GT", "Semua Kapal"
    val historicalRiskScore: Int, // 1 - 100
    val latitude: Double,
    val longitude: Double
)

data class EmergencySafetyGuide(
    val id: String,
    val category: String,
    val title: String,
    val priority: String, // "KRITIS", "PENTING", "STANDAR"
    val description: String,
    val actionSteps: List<String>,
    val vhfChannel: String = "16 (156.800 MHz)",
    val internationalSignal: String
)

data class PortFacility(
    val id: String,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val capacityVessels: Int,
    val vhfChannel: String,
    val phoneOperator: String,
    val fuelAvailable: Boolean,
    val freshWaterAvailable: Boolean,
    val iceFactoryAvailable: Boolean
)

data class ZppiFishingZone(
    val id: String,
    val name: String,
    val wppZone: String,
    val targetSpecies: String,
    val latitude: Double,
    val longitude: Double,
    val sstCelsius: Float,
    val chlorophyllA: Float,
    val potentialScore: Int, // 1 - 100
    val peakHours: String,
    val recommendedDepthM: String,
    val gearType: String
)

data class SonarFishDetection(
    val depthMeters: Float,
    val fishSize: String, // "BESAR", "SEDANG", "KECIL"
    val estimatedWeightKg: Float,
    val xOffsetPercent: Float
)

data class ColregsRule(
    val id: String,
    val ruleNumber: String,
    val title: String,
    val situation: String,
    val actionRequired: String,
    val nightLights: String,
    val soundSignal: String
)

enum class NavionicsChartLayer(val title: String, val subtitle: String) {
    NAUTICAL_ENC("Navionics Nautical Chart (ENC)", "Standar Peta Laut Vektor Hidrografi"),
    SONARCHART_HD("SonarChart™ HD Fishing", "Batimetri Kontur Kerapatan 1 Meter"),
    SATELLITE_OVERLAY("Citra Satelit Maritim", "Satelit Resolusi Tinggi dengan Grid Maritim"),
    CURRENTS_WEATHER("Vektor Arus & Angin", "Aliran Arus Laut & Kecepatan Angin")
}

enum class BuoyType {
    PORT_HAND, // Merah Silinder
    STARBOARD_HAND, // Hijau Kerucut
    NORTH_CARDINAL, // Hitam-Kuning (Kerucut Atas)
    SOUTH_CARDINAL, // Kuning-Hitam (Kerucut Bawah)
    EAST_CARDINAL, // Hitam-Kuning-Hitam
    WEST_CARDINAL, // Kuning-Hitam-Kuning
    ISOLATED_DANGER, // Hitam-Merah (2 Bola Hitam)
    SAFE_WATER // Merah-Putih Vertikal
}

data class NavigationalAid(
    val id: String,
    val name: String,
    val type: BuoyType,
    val latitude: Double,
    val longitude: Double,
    val lightFlash: String
)

data class Lighthouse(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lightCharacter: String,
    val rangeNM: Float,
    val lightColor: Color
)

data class SpotSounding(
    val latitude: Double,
    val longitude: Double,
    val depthMeters: Double
)

data class CpaCalculationResult(
    val vessel: Vessel,
    val cpaDistanceNM: Double,
    val tcpaMinutes: Double,
    val isDangerous: Boolean,
    val collisionPointLat: Double,
    val collisionPointLon: Double
)

data class AnchorWatchState(
    val isActive: Boolean = false,
    val anchorLat: Double = 0.0,
    val anchorLon: Double = 0.0,
    val radiusMeters: Float = 50f,
    val currentDistanceMeters: Float = 0f,
    val isAlarmTriggered: Boolean = false,
    val setTimeString: String = ""
)

data class BreadcrumbTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

data class TideCurrentPrediction(
    val portName: String,
    val currentTideState: String,
    val tideHeightMeters: Float,
    val currentSpeedKnots: Float,
    val currentDirectionDeg: Float,
    val nextHighTideTime: String,
    val nextLowTideTime: String
)



