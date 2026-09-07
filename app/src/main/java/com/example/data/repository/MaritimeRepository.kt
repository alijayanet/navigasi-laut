package com.example.data.repository

import androidx.compose.ui.graphics.Color
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class MaritimeRepository(private val database: MaritimeDatabase) {

    val waypoints: Flow<List<FishNetWaypointEntity>> = database.waypointDao().getAllWaypoints()
    val catchLogs: Flow<List<FishCatchLogEntity>> = database.catchLogDao().getAllCatchLogs()
    val maintenances: Flow<List<ShipMaintenanceEntity>> = database.maintenanceDao().getAllMaintenances()
    val sosRecords: Flow<List<EmergencySosEntity>> = database.emergencySosDao().getAllSosRecords()
    val totalCatchWeight: Flow<Double?> = database.catchLogDao().getTotalCatchWeightKg()
    val totalRevenue: Flow<Double?> = database.catchLogDao().getTotalRevenue()

    suspend fun addWaypoint(waypoint: FishNetWaypointEntity): Long =
        database.waypointDao().insertWaypoint(waypoint)

    suspend fun updateWaypointRetrieved(id: Long, retrieved: Boolean) =
        database.waypointDao().updateRetrievedStatus(id, retrieved)

    suspend fun deleteWaypoint(waypoint: FishNetWaypointEntity) =
        database.waypointDao().deleteWaypoint(waypoint)

    suspend fun addCatchLog(log: FishCatchLogEntity): Long =
        database.catchLogDao().insertCatchLog(log)

    suspend fun deleteCatchLog(log: FishCatchLogEntity) =
        database.catchLogDao().deleteCatchLog(log)

    suspend fun addMaintenance(item: ShipMaintenanceEntity): Long =
        database.maintenanceDao().insertMaintenance(item)

    suspend fun updateMaintenance(item: ShipMaintenanceEntity) =
        database.maintenanceDao().updateMaintenance(item)

    suspend fun deleteMaintenance(item: ShipMaintenanceEntity) =
        database.maintenanceDao().deleteMaintenance(item)

    suspend fun recordEmergencySos(sos: EmergencySosEntity): Long =
        database.emergencySosDao().insertSosRecord(sos)

    suspend fun deleteSosRecord(sos: EmergencySosEntity) =
        database.emergencySosDao().deleteSosRecord(sos)

    suspend fun initializeDefaultDataIfEmpty() {
        if (database.maintenanceDao().getMaintenanceCount() == 0) {
            val defaultMaintenances = listOf(
                ShipMaintenanceEntity(
                    componentName = "Penggantian Oli Mesin Induk & Filter Oli",
                    currentEngineHours = 185,
                    intervalHours = 100,
                    nextDueEngineHours = 200,
                    lastServiceDate = "10 Agustus 2026",
                    isOverdue = false,
                    maintenanceGuide = "Kuras oli saat mesin masih hangat, ganti filter oli mesin, bersihkan filter pernafasan karter. Gunakan oli standar marin diesel 15W-40."
                ),
                ShipMaintenanceEntity(
                    componentName = "Penggantian Filter Solar & Water Separator",
                    currentEngineHours = 185,
                    intervalHours = 250,
                    nextDueEngineHours = 250,
                    lastServiceDate = "15 Juli 2026",
                    isOverdue = false,
                    maintenanceGuide = "Periksa endapan air pada separator setiap hari sebelum melaut. Ganti elemen filter jika solar terkontaminasi atau setiap 250 jam."
                ),
                ShipMaintenanceEntity(
                    componentName = "Pembersihan Teritip & Baling-baling (Propeller)",
                    currentEngineHours = 185,
                    intervalHours = 150,
                    nextDueEngineHours = 150,
                    lastServiceDate = "28 Mei 2026",
                    isOverdue = true,
                    maintenanceGuide = "Keruk lumut & teritip yang menempel pada lambung dan daun propeller. Bersihkan sacrificial anode seng (zinc anode) dari korosi air laut."
                ),
                ShipMaintenanceEntity(
                    componentName = "Pengecekan Aki / Baterai & Terminal Kelistrikan",
                    currentEngineHours = 185,
                    intervalHours = 100,
                    nextDueEngineHours = 200,
                    lastServiceDate = "05 Agustus 2026",
                    isOverdue = false,
                    maintenanceGuide = "Cek voltase aki (min 12.6V), bersihkan kerak putih pada kutub terminal aki dengan air hangat, periksa kekencangan v-belt alternator."
                ),
                ShipMaintenanceEntity(
                    componentName = "Pemeriksaan Impeller Pompa Pendingin Air Laut",
                    currentEngineHours = 185,
                    intervalHours = 300,
                    nextDueEngineHours = 300,
                    lastServiceDate = "01 Juni 2026",
                    isOverdue = false,
                    maintenanceGuide = "Buka cover impeller, cek sirip karet dari keretakan atau aus akibat pasir. Lumasi dengan gliserin saat pemasangan kembali."
                ),
                ShipMaintenanceEntity(
                    componentName = "Inspeksi Jaket Pelampung (Life Jacket) & Suar Darurat",
                    currentEngineHours = 185,
                    intervalHours = 100,
                    nextDueEngineHours = 250,
                    lastServiceDate = "01 Agustus 2026",
                    isOverdue = false,
                    maintenanceGuide = "Pastikan peluit dan lampu jaket pelampung berfungsi. Cek tanggal kedaluwarsa roket suar darurat parasut & red hand flare IMO."
                )
            )
            database.maintenanceDao().insertAll(defaultMaintenances)

            // Add sample waypoint & catch log
            database.waypointDao().insertWaypoint(
                FishNetWaypointEntity(
                    title = "Jaring Gillnet Tongkol (Pelampung Merah)",
                    type = "JARING_HANYUT",
                    latitude = -5.9120,
                    longitude = 106.8450,
                    depthMeters = 24.5,
                    baitOrGearNotes = "Jaring hanyut monofilamen 3.5 inch, 6 piece disambung, arah hanyut arus timur.",
                    targetRetrievalDate = "Besok Pagi 05:30 WIB",
                    driftDistanceMeters = 850.0,
                    currentHeadingDeg = 78f,
                    tagColor = "#FFB703"
                )
            )

            database.waypointDao().insertWaypoint(
                FishNetWaypointEntity(
                    title = "Spot Rumpon Tuna & Cakalang",
                    type = "RUMPON",
                    latitude = -6.0420,
                    longitude = 107.1200,
                    depthMeters = 48.0,
                    baitOrGearNotes = "Rumpon daun kelapa dalam, banyak ikan umpan laying & kembung.",
                    targetRetrievalDate = "Setiap saat",
                    driftDistanceMeters = 0.0,
                    currentHeadingDeg = 0f,
                    tagColor = "#2A9D8F"
                )
            )

            database.catchLogDao().insertCatchLog(
                FishCatchLogEntity(
                    dateStr = "24 Agustus 2026",
                    fishSpecies = "Tongkol Lisong",
                    weightKg = 340.0,
                    pricePerKg = 25000.0,
                    estimatedRevenue = 8500000.0,
                    fishingGear = "Jaring Insang (Gillnet)",
                    wppnriZone = "WPP 712 (Laut Jawa)",
                    latitude = -5.9500,
                    longitude = 106.8800,
                    notes = "Tangkapan melimpah saat fajar, ukuran 25-30 cm, segar kualitas ekspor."
                )
            )
            database.catchLogDao().insertCatchLog(
                FishCatchLogEntity(
                    dateStr = "22 Agustus 2026",
                    fishSpecies = "Cakalang & Kembung",
                    weightKg = 210.0,
                    pricePerKg = 30000.0,
                    estimatedRevenue = 6300000.0,
                    fishingGear = "Pancing Ulur (Handline)",
                    wppnriZone = "WPP 712 (Laut Jawa)",
                    latitude = -6.0100,
                    longitude = 107.0500,
                    notes = "Umpan cumi hidup sangat efektif di dekat rumpon."
                )
            )
        }
    }

    fun getIndonesianVessels(): List<Vessel> {
        return listOf(
            Vessel(
                id = "VSL-01",
                name = "KM. BARUNA JAYA IV",
                callSign = "YDB402",
                mmsi = "525001240",
                type = VesselType.FISHING,
                latitude = -5.9250,
                longitude = 106.8200,
                speedKnots = 7.4,
                headingDeg = 65f,
                destination = "WPP 712 Laut Jawa",
                status = "Sedang Melakukan Penangkapan (Underway Fishing)",
                lengthMeters = 28,
                draughtMeters = 2.4,
                grossTonnage = 29,
                lastUpdated = "1 menit lalu",
                originPort = "Pelabuhan Muara Baru Jakarta",
                eta = "26 Agu 2026, 17:00"
            ),
            Vessel(
                id = "VSL-02",
                name = "KN. SAR BHISMA 239",
                callSign = "YDA991",
                mmsi = "525999011",
                type = VesselType.PATROL,
                latitude = -5.8700,
                longitude = 106.7600,
                speedKnots = 18.2,
                headingDeg = 310f,
                destination = "Patroli Siaga Selat Sunda",
                status = "Patroli Siaga SAR (Underway)",
                lengthMeters = 40,
                draughtMeters = 1.9,
                grossTonnage = 150,
                lastUpdated = "Real-time",
                originPort = "Dermaga Basarnas Tanjung Priok",
                eta = "Patroli Aktif"
            ),
            Vessel(
                id = "VSL-03",
                name = "KM. NUSANTARA MAKMUR",
                callSign = "YDB118",
                mmsi = "525008922",
                type = VesselType.FISHING,
                latitude = -5.9600,
                longitude = 106.9100,
                speedKnots = 4.2,
                headingDeg = 120f,
                destination = "Spot Jaring Karang",
                status = "Menarik Jaring (Hauling Nets)",
                lengthMeters = 22,
                draughtMeters = 1.8,
                grossTonnage = 18,
                lastUpdated = "3 menit lalu",
                originPort = "Muara Angke",
                eta = "25 Agu 2026, 21:00"
            ),
            Vessel(
                id = "VSL-04",
                name = "MV. MERATUS SAMUDERA",
                callSign = "PKLM",
                mmsi = "525112004",
                type = VesselType.CARGO,
                latitude = -5.8100,
                longitude = 106.9500,
                speedKnots = 14.6,
                headingDeg = 75f,
                destination = "Tanjung Perak, Surabaya",
                status = "Berlayar Normal (Underway using Engine)",
                lengthMeters = 142,
                draughtMeters = 6.8,
                grossTonnage = 8500,
                lastUpdated = "2 menit lalu",
                originPort = "Tanjung Priok",
                eta = "26 Agu 2026, 06:00"
            ),
            Vessel(
                id = "VSL-05",
                name = "MT. PERTAMINA GAS 2",
                callSign = "PKGA",
                mmsi = "525778891",
                type = VesselType.TANKER,
                latitude = -5.7500,
                longitude = 106.6800,
                speedKnots = 11.8,
                headingDeg = 245f,
                destination = "Balongan, Indramayu",
                status = "Muatan Penuh Berlayar (Underway)",
                lengthMeters = 180,
                draughtMeters = 9.2,
                grossTonnage = 18000,
                lastUpdated = "1 menit lalu",
                originPort = "Merak Cilegon",
                eta = "26 Agu 2026, 12:30"
            ),
            Vessel(
                id = "VSL-06",
                name = "KMP. BATU MANDI",
                callSign = "PKFE",
                mmsi = "525334419",
                type = VesselType.PASSENGER,
                latitude = -5.8900,
                longitude = 106.6200,
                speedKnots = 13.1,
                headingDeg = 280f,
                destination = "Bakauheni, Lampung",
                status = "Penyeberangan Rutin (Underway)",
                lengthMeters = 95,
                draughtMeters = 4.1,
                grossTonnage = 5400,
                lastUpdated = "Real-time",
                originPort = "Merak Banten",
                eta = "25 Agu 2026, 15:45"
            ),
            Vessel(
                id = "VSL-07",
                name = "TB. JAYAKARTA PANDU III",
                callSign = "YDB901",
                mmsi = "525441908",
                type = VesselType.TUGBOAT,
                latitude = -5.9900,
                longitude = 106.8700,
                speedKnots = 5.8,
                headingDeg = 180f,
                destination = "Alur Pelabuhan Tanjung Priok",
                status = "Memandu Tongkang (Towing)",
                lengthMeters = 32,
                draughtMeters = 3.2,
                grossTonnage = 280,
                lastUpdated = "Real-time",
                originPort = "Tanjung Priok",
                eta = "25 Agu 2026, 14:00"
            ),
            // ================= PELABUHAN PATIMBAN (SUBANG) =================
            Vessel(
                id = "VSL-PTB-01",
                name = "MV. BELITUNG HIGHWAY",
                callSign = "PKBH",
                mmsi = "525992014",
                type = VesselType.CARGO,
                latitude = -6.2180,
                longitude = 107.9060,
                speedKnots = 12.4,
                headingDeg = 15f,
                destination = "Terminal Mobil Patimban (Car Terminal)",
                status = "Mendekati Alur Masuk Patimban (Underway)",
                lengthMeters = 180,
                draughtMeters = 7.5,
                grossTonnage = 14500,
                lastUpdated = "Real-time",
                originPort = "Toyohashi, Jepang",
                eta = "Hari ini, 10:30"
            ),
            Vessel(
                id = "VSL-PTB-02",
                name = "TB. PATIMBAN PANDU 01",
                callSign = "YDC331",
                mmsi = "525119044",
                type = VesselType.TUGBOAT,
                latitude = -6.2280,
                longitude = 107.9040,
                speedKnots = 6.8,
                headingDeg = 345f,
                destination = "Kolam Putar Patimban",
                status = "Siaga Pemanduan Kapal Ekspor (Standby Towing)",
                lengthMeters = 34,
                draughtMeters = 3.8,
                grossTonnage = 320,
                lastUpdated = "1 menit lalu",
                originPort = "Dermaga Patimban",
                eta = "Operasional Pelabuhan"
            ),
            Vessel(
                id = "VSL-PTB-03",
                name = "KN. CELURIT 201 (KPLP)",
                callSign = "YDB881",
                mmsi = "525771092",
                type = VesselType.PATROL,
                latitude = -6.2120,
                longitude = 107.8920,
                speedKnots = 16.5,
                headingDeg = 80f,
                destination = "Patroli Alur Pelayaran Patimban",
                status = "Patroli Kesyahbandaran KSOP Patimban",
                lengthMeters = 42,
                draughtMeters = 2.1,
                grossTonnage = 210,
                lastUpdated = "Real-time",
                originPort = "Pangkalan KSOP Patimban",
                eta = "Patroli Aktif"
            ),
            Vessel(
                id = "VSL-PTB-04",
                name = "KM. SUBANG BAHARI VII",
                callSign = "YDB449",
                mmsi = "525004122",
                type = VesselType.FISHING,
                latitude = -6.2050,
                longitude = 107.9250,
                speedKnots = 5.2,
                headingDeg = 110f,
                destination = "Fishing Ground Teluk Ciasem - Patimban",
                status = "Operasi Penangkapan Jaring Insang Hanyut",
                lengthMeters = 19,
                draughtMeters = 1.6,
                grossTonnage = 14,
                lastUpdated = "2 menit lalu",
                originPort = "Muara Genteng Subang",
                eta = "Hari ini, 18:00"
            ),
            // ================= PLTU INDRAMAYU & ERETAN - PATROL =================
            Vessel(
                id = "VSL-PLTU-01",
                name = "TB. INDRAMAYU POWER 08",
                callSign = "PKIP",
                mmsi = "525660219",
                type = VesselType.TUGBOAT,
                latitude = -6.2840,
                longitude = 108.0080,
                speedKnots = 4.8,
                headingDeg = 160f,
                destination = "Jetty Khusus Batubara PLTU Indramayu",
                status = "Towing Tongkang Batubara 10.000 Ton",
                lengthMeters = 36,
                draughtMeters = 4.2,
                grossTonnage = 450,
                lastUpdated = "Real-time",
                originPort = "Muara Pantai, Kaltim",
                eta = "Hari ini, 11:15"
            ),
            Vessel(
                id = "VSL-PLTU-02",
                name = "TK. COAL CARRIER SUMURADEM",
                callSign = "TKSA",
                mmsi = "525660220",
                type = VesselType.CARGO,
                latitude = -6.2880,
                longitude = 108.0120,
                speedKnots = 4.5,
                headingDeg = 160f,
                destination = "Unloading Coal Jetty PLTU Sumuradem",
                status = "Sandar Pembongkaran Batubara PLTU",
                lengthMeters = 105,
                draughtMeters = 5.6,
                grossTonnage = 6200,
                lastUpdated = "3 menit lalu",
                originPort = "Banjarmasin",
                eta = "Sedang Sandar"
            ),
            Vessel(
                id = "VSL-PLTU-03",
                name = "KM. SRI REJEKI ERETAN",
                callSign = "YDB551",
                mmsi = "525008772",
                type = VesselType.FISHING,
                latitude = -6.2750,
                longitude = 108.0380,
                speedKnots = 6.4,
                headingDeg = 290f,
                destination = "Pangkalan Nelayan Eretan Kulon",
                status = "Kembali Melaut Membawa Ikan Tongkol & Cumi",
                lengthMeters = 23,
                draughtMeters = 1.9,
                grossTonnage = 24,
                lastUpdated = "1 menit lalu",
                originPort = "TPI Eretan Wetan",
                eta = "Hari ini, 12:00"
            ),
            Vessel(
                id = "VSL-PLTU-04",
                name = "KM. PATROL MAKMUR INDAH",
                callSign = "YDB559",
                mmsi = "525008778",
                type = VesselType.FISHING,
                latitude = -6.2620,
                longitude = 107.9850,
                speedKnots = 5.0,
                headingDeg = 50f,
                destination = "Spot Rumpon Lepas Pantai Patrol",
                status = "Pancing Cumi & Rawai Dasar",
                lengthMeters = 17,
                draughtMeters = 1.5,
                grossTonnage = 12,
                lastUpdated = "Real-time",
                originPort = "Muara Patrol Indramayu",
                eta = "Hari ini, 19:30"
            ),
            Vessel(
                id = "VSL-PLTU-05",
                name = "KN. KEPODANG 5001 (POLAIRUD)",
                callSign = "YDA301",
                mmsi = "525881903",
                type = VesselType.PATROL,
                latitude = -6.2950,
                longitude = 108.0280,
                speedKnots = 14.8,
                headingDeg = 320f,
                destination = "Pengamanan Obvitnas PLTU Indramayu",
                status = "Patroli Kamtibmas Perairan Sukra - Eretan",
                lengthMeters = 28,
                draughtMeters = 1.6,
                grossTonnage = 95,
                lastUpdated = "Real-time",
                originPort = "Pos Polairud Eretan Indramayu",
                eta = "Patroli Aktif"
            ),
            Vessel(
                id = "VSL-PLTU-06",
                name = "MT. PERTAMINA BALONGAN IX",
                callSign = "PKBL",
                mmsi = "525779012",
                type = VesselType.TANKER,
                latitude = -6.3250,
                longitude = 108.3850,
                speedKnots = 9.8,
                headingDeg = 210f,
                destination = "SPM Loading Buoy Kilang Balongan",
                status = "Manuver Sandar Single Point Mooring",
                lengthMeters = 195,
                draughtMeters = 10.4,
                grossTonnage = 28000,
                lastUpdated = "Real-time",
                originPort = "Dumai, Riau",
                eta = "Hari ini, 14:00"
            )
        )
    }

    fun getIndonesianMaritimeWeatherAreas(): List<MaritimeWeatherArea> {
        return listOf(
            MaritimeWeatherArea(
                id = "W-00",
                name = "Teluk Jakarta & Kepulauan Seribu",
                wppnriZone = "WPPNRI 712",
                latitude = -5.9320,
                longitude = 106.8380,
                waveHeight = 0.6f,
                maxWaveHeight = 1.0f,
                waveCategory = SeaWaveCategory.TENANG,
                windSpeedKnots = 9,
                windDirection = "Timur Laut",
                windBeaufort = 3,
                currentSpeedKnots = 0.5,
                currentDirection = "Barat Daya",
                highTideTime = "08:30 WIB",
                highTideHeightM = 1.10f,
                lowTideTime = "16:15 WIB",
                lowTideHeightM = 0.25f,
                seaSurfaceTempC = 29,
                visibilityKm = 12,
                rainChance = 10,
                statusMelaut = MelautStatus.AMAN,
                bmkgWarning = null,
                moonPhase = "Bulan Sabit Awal (Waxing Crescent)",
                lunarFishingRating = "Sangat Baik (Indeks 94%) ★★★★★",
                forecast7Days = generate7DayForecast(0.6f, 9, "Cerah Berawan")
            ),
            MaritimeWeatherArea(
                id = "W-01",
                name = "Laut Jawa Bagian Tengah & Timur",
                wppnriZone = "WPPNRI 712",
                latitude = -5.9000,
                longitude = 107.5000,
                waveHeight = 1.1f,
                maxWaveHeight = 1.8f,
                waveCategory = SeaWaveCategory.RENDAH,
                windSpeedKnots = 14,
                windDirection = "Timur - Tenggara",
                windBeaufort = 4,
                currentSpeedKnots = 0.8,
                currentDirection = "Barat Daya",
                highTideTime = "09:45 WIB",
                highTideHeightM = 1.35f,
                lowTideTime = "17:30 WIB",
                lowTideHeightM = 0.35f,
                seaSurfaceTempC = 29,
                visibilityKm = 10,
                rainChance = 25,
                statusMelaut = MelautStatus.AMAN,
                bmkgWarning = null,
                moonPhase = "Bulan Sabit Awal (Waxing Crescent)",
                lunarFishingRating = "Sangat Baik ★★★★★",
                forecast7Days = generate7DayForecast(1.1f, 14, "Cerah Berawan")
            ),
            MaritimeWeatherArea(
                id = "W-02",
                name = "Selat Sunda Bagian Selatan",
                wppnriZone = "WPPNRI 572",
                latitude = -6.2500,
                longitude = 105.7500,
                waveHeight = 2.8f,
                maxWaveHeight = 3.8f,
                waveCategory = SeaWaveCategory.TINGGI,
                windSpeedKnots = 24,
                windDirection = "Selatan - Barat Daya",
                windBeaufort = 6,
                currentSpeedKnots = 2.4,
                currentDirection = "Utara",
                highTideTime = "07:15 WIB",
                highTideHeightM = 1.9f,
                lowTideTime = "15:00 WIB",
                lowTideHeightM = 0.4f,
                seaSurfaceTempC = 27,
                visibilityKm = 6,
                rainChance = 70,
                statusMelaut = MelautStatus.BAHAYA,
                bmkgWarning = "PERINGATAN DINI GELOMBANG TINGGI BMKG: Gelombang mencapai 2.5 - 4.0 m di Selat Sunda Selatan & Samudera Hindia. Kapal Nelayan < 30 GT dilarang melaut!",
                moonPhase = "Bulan Sabit Awal",
                lunarFishingRating = "Bahaya Arus Kuat ★★☆☆☆",
                forecast7Days = generate7DayForecast(2.8f, 24, "Hujan Ringan & Gelombang Tinggi")
            ),
            MaritimeWeatherArea(
                id = "W-03",
                name = "Laut Natuna Utara",
                wppnriZone = "WPPNRI 711",
                latitude = 4.0000,
                longitude = 108.5000,
                waveHeight = 2.2f,
                maxWaveHeight = 2.9f,
                waveCategory = SeaWaveCategory.SEDANG,
                windSpeedKnots = 19,
                windDirection = "Barat Daya",
                windBeaufort = 5,
                currentSpeedKnots = 1.2,
                currentDirection = "Timur Laut",
                highTideTime = "11:20 WIB",
                highTideHeightM = 1.6f,
                lowTideTime = "19:40 WIB",
                lowTideHeightM = 0.5f,
                seaSurfaceTempC = 29,
                visibilityKm = 8,
                rainChance = 40,
                statusMelaut = MelautStatus.WASPADA,
                bmkgWarning = "WASPADA BMKG: Potensi awan Cumulonimbus menyebabkan peningkatan tinggi gelombang sesaat dan angin kencang.",
                moonPhase = "Bulan Sabit Awal",
                lunarFishingRating = "Baik ★★★☆☆",
                forecast7Days = generate7DayForecast(2.2f, 19, "Berawan Berpetir")
            ),
            MaritimeWeatherArea(
                id = "W-04",
                name = "Selat Malaka Bagian Utara",
                wppnriZone = "WPPNRI 571",
                latitude = 3.5000,
                longitude = 100.2000,
                waveHeight = 0.8f,
                maxWaveHeight = 1.2f,
                waveCategory = SeaWaveCategory.RENDAH,
                windSpeedKnots = 10,
                windDirection = "Tenggara",
                windBeaufort = 3,
                currentSpeedKnots = 0.9,
                currentDirection = "Barat Laut",
                highTideTime = "12:00 WIB",
                highTideHeightM = 2.1f,
                lowTideTime = "18:15 WIB",
                lowTideHeightM = 0.6f,
                seaSurfaceTempC = 30,
                visibilityKm = 10,
                rainChance = 15,
                statusMelaut = MelautStatus.AMAN,
                bmkgWarning = null,
                moonPhase = "Bulan Sabit Awal",
                lunarFishingRating = "Sangat Baik ★★★★★",
                forecast7Days = generate7DayForecast(0.8f, 10, "Cerah")
            ),
            MaritimeWeatherArea(
                id = "W-05",
                name = "Samudera Hindia Selatan Jawa",
                wppnriZone = "WPPNRI 573",
                latitude = -8.5000,
                longitude = 110.5000,
                waveHeight = 3.6f,
                maxWaveHeight = 4.8f,
                waveCategory = SeaWaveCategory.TINGGI,
                windSpeedKnots = 26,
                windDirection = "Timur - Tenggara",
                windBeaufort = 6,
                currentSpeedKnots = 2.1,
                currentDirection = "Barat",
                highTideTime = "08:30 WIB",
                highTideHeightM = 2.4f,
                lowTideTime = "16:10 WIB",
                lowTideHeightM = 0.3f,
                seaSurfaceTempC = 26,
                visibilityKm = 5,
                rainChance = 65,
                statusMelaut = MelautStatus.BAHAYA,
                bmkgWarning = "SIAGA BMKG: Gelombang Sangat Tinggi 3.5 - 5.0 m. Berbahaya bagi seluruh armada nelayan dan tongkang di perairan selatan Jawa hingga Bali!",
                moonPhase = "Bulan Sabit Awal",
                lunarFishingRating = "Sangat Berbahaya ★☆☆☆☆",
                forecast7Days = generate7DayForecast(3.6f, 26, "Gelombang Ekstrem")
            ),
            MaritimeWeatherArea(
                id = "W-06",
                name = "Selat Makassar Bagian Selatan",
                wppnriZone = "WPPNRI 713",
                latitude = -3.8000,
                longitude = 118.2000,
                waveHeight = 1.4f,
                maxWaveHeight = 2.1f,
                waveCategory = SeaWaveCategory.SEDANG,
                windSpeedKnots = 15,
                windDirection = "Selatan",
                windBeaufort = 4,
                currentSpeedKnots = 1.5,
                currentDirection = "Selatan (Arlindo)",
                highTideTime = "10:10 WITA",
                highTideHeightM = 1.5f,
                lowTideTime = "17:45 WITA",
                lowTideHeightM = 0.4f,
                seaSurfaceTempC = 29,
                visibilityKm = 9,
                rainChance = 30,
                statusMelaut = MelautStatus.WASPADA,
                bmkgWarning = "WASPADA: Arus lintas Indonesia (Arlindo) cukup deras di alur tengah selat.",
                moonPhase = "Bulan Sabit Awal",
                lunarFishingRating = "Baik ★★★★☆",
                forecast7Days = generate7DayForecast(1.4f, 15, "Berawan")
            ),
            MaritimeWeatherArea(
                id = "W-07",
                name = "Laut Arafura & Kep. Aru",
                wppnriZone = "WPPNRI 718",
                latitude = -6.8000,
                longitude = 135.2000,
                waveHeight = 2.4f,
                maxWaveHeight = 3.2f,
                waveCategory = SeaWaveCategory.SEDANG,
                windSpeedKnots = 20,
                windDirection = "Tenggara",
                windBeaufort = 5,
                currentSpeedKnots = 1.8,
                currentDirection = "Barat Laut",
                highTideTime = "13:40 WIT",
                highTideHeightM = 2.2f,
                lowTideTime = "21:00 WIT",
                lowTideHeightM = 0.5f,
                seaSurfaceTempC = 28,
                visibilityKm = 8,
                rainChance = 35,
                statusMelaut = MelautStatus.WASPADA,
                bmkgWarning = "WASPADA BMKG: Angin muson tenggara kencang meningkatkan gelombang di perairan dangkal Arafura.",
                moonPhase = "Bulan Sabit Awal",
                lunarFishingRating = "Baik ★★★☆☆",
                forecast7Days = generate7DayForecast(2.4f, 20, "Angin Kencang")
            )
        )
    }

    private fun generate7DayForecast(baseWave: Float, baseWind: Int, mainCond: String): List<DayForecast> {
        val days = listOf("Hari Ini", "Besok", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val dateFormater = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        val cal = Calendar.getInstance()
        
        return days.mapIndexed { index, dayName ->
            val date = cal.time
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val waveVariation = ((index * 7) % 5 - 2) * 0.15f
            val wave = (baseWave + waveVariation).coerceAtLeast(0.4f)
            val maxWave = wave * 1.4f
            val wind = (baseWind + (index % 3) * 2).coerceAtLeast(8)
            val safe = wave < 2.0f

            DayForecast(
                dayName = dayName,
                dateStr = dateFormater.format(date),
                waveHeight = String.format(Locale.US, "%.1f", wave).toFloat(),
                maxWaveHeight = String.format(Locale.US, "%.1f", maxWave).toFloat(),
                windSpeedKnots = wind,
                windDirection = if (index % 2 == 0) "Tenggara" else "Timur",
                condition = if (wave > 2.5f) "Gelombang Tinggi" else if (index % 3 == 0) "Hujan Ringan" else "Cerah Berawan",
                rainChance = if (wave > 2.5f) 70 else 20 + (index * 8) % 40,
                isSafe = safe
            )
        }
    }

    fun getHighRiskZones(): List<HighRiskZonePrediction> {
        return listOf(
            HighRiskZonePrediction(
                id = "RZ-01",
                regionName = "Samudera Hindia Selatan Banten - Jawa Timur",
                wppZone = "WPPNRI 572 & 573",
                currentWaveM = 3.8f,
                predictedMaxWaveM = 4.9f,
                riskLevel = "SIAGA MERAH TINGGI",
                riskFactor = "Pola Tekanan Rendah & Swell Samudera Hindia",
                affectedVesselSizes = "Semua Kapal < 100 GT & Perahu Nelayan",
                historicalRiskScore = 92,
                latitude = -8.50,
                longitude = 109.00
            ),
            HighRiskZonePrediction(
                id = "RZ-02",
                regionName = "Selat Sunda Bagian Selatan",
                wppZone = "WPPNRI 572",
                currentWaveM = 2.9f,
                predictedMaxWaveM = 3.7f,
                riskLevel = "WASPADA TINGGI",
                riskFactor = "Penyempitan Arus Selat & Gelombang Pasang",
                affectedVesselSizes = "Kapal Nelayan < 30 GT & Kapal Tongkang",
                historicalRiskScore = 84,
                latitude = -6.20,
                longitude = 105.80
            ),
            HighRiskZonePrediction(
                id = "RZ-03",
                regionName = "Laut Natuna Utara & Kep. Anambas",
                wppZone = "WPPNRI 711",
                currentWaveM = 2.3f,
                predictedMaxWaveM = 3.1f,
                riskLevel = "WASPADA SEDANG",
                riskFactor = "Awan Konvektif & Angin Barat Daya",
                affectedVesselSizes = "Perahu Nelayan Jukung & Small Boats",
                historicalRiskScore = 76,
                latitude = 4.20,
                longitude = 108.20
            ),
            HighRiskZonePrediction(
                id = "RZ-04",
                regionName = "Laut Arafura Bagian Tengah",
                wppZone = "WPPNRI 718",
                currentWaveM = 2.5f,
                predictedMaxWaveM = 3.3f,
                riskLevel = "WASPADA",
                riskFactor = "Angin Muson Tenggara Kencang (>22 Knot)",
                affectedVesselSizes = "Kapal Nelayan < 20 GT",
                historicalRiskScore = 79,
                latitude = -7.10,
                longitude = 136.00
            ),
            HighRiskZonePrediction(
                id = "RZ-05",
                regionName = "Laut Banda & Perairan Kep. Tanimbar",
                wppZone = "WPPNRI 714",
                currentWaveM = 2.1f,
                predictedMaxWaveM = 2.8f,
                riskLevel = "WASPADA SEDANG",
                riskFactor = "Kedalaman Palung Laut & Turbulensi Arus",
                affectedVesselSizes = "Kapal Ikan Tradisional",
                historicalRiskScore = 68,
                latitude = -5.50,
                longitude = 130.00
            )
        )
    }

    fun getOfflineSafetyGuides(): List<EmergencySafetyGuide> {
        return listOf(
            EmergencySafetyGuide(
                id = "SG-01",
                category = "DARURAT UTAMA",
                title = "Orang Jatuh ke Laut (Man Overboard / MOB)",
                priority = "KRITIS",
                description = "Tindakan segera detik-detik pertama saat awak kapal atau nelayan terjatuh ke laut.",
                actionSteps = listOf(
                    "1. Teriakkan 'ORANG JATUH KE LAUT SISI KANAN/KIRI' sekeras mungkin untuk membangunkan seluruh awak.",
                    "2. Segera lemparkan Pelampung Penolong (Lifebuoy) dengan tali/lampu suar ke arah korban.",
                    "3. Tekan tombol MOB pada GPS kapal untuk mengunci koordinat titik jatuh seketika.",
                    "4. Putar kemudi kapal ke arah jatuhnya orang untuk menjauhkan baling-baling (Propeller) dari korban.",
                    "5. Lakukan manuver Williamson Turn atau Anderson Turn untuk berputar 180° kembali ke lintasan semula.",
                    "6. Tugaskan satu pengamat khusus (Lookout) yang terus menunjuk ke arah korban tanpa mengalihkan pandangan.",
                    "7. Siarkan panggilan MAYDAY di Radio VHF Channel 16 jika korban belum ditemukan dalam 3 menit."
                ),
                internationalSignal = "Kibarkan Bendera OSCAR (Kuning-Merah Diagonal) / 3 Tiupan Panjang Peluit Kapal (Morse 'O')"
            ),
            EmergencySafetyGuide(
                id = "SG-02",
                category = "DARURAT MESIN",
                title = "Mesin Induk Mati Total di Tengah Laut (Engine Breakdown)",
                priority = "PENTING",
                description = "Prosedur keselamatan saat perahu terombang-ambing tanpa tenaga penggerak.",
                actionSteps = listOf(
                    "1. Segera pasang Jangkar Apung (Sea Anchor / Dreg) di haluan kapal agar haluan tetap menghadap ombak dan perahu tidak terbalik menyamping.",
                    "2. Periksa tangki solar: cek apakah ada sumbatan solar, filter solar kotor, atau udara masuk (lakukan bleeding pompa injeksi).",
                    "3. Periksa ketersediaan oli mesin dan pastikan mesin tidak mengalami overheating atau piston macet.",
                    "4. Cek v-belt alternator dan sambungan aki starter mesin.",
                    "5. Jika di jalur lalu lintas kapal besar (TSS / Alur Pelayaran), nyalakan lampu navigasi 'Kapal Tidak Dapat Diolah Gerak' (2 Lampu Merah Vertikal di malam hari).",
                    "6. Hubungi kapal nelayan rekanan terdekat atau broadcast PAN-PAN di VHF Ch 16 untuk meminta bantuan tunda (towing)."
                ),
                internationalSignal = "Simbol 2 Bola Hitam Vertikal di siang hari atau 2 Lampu Merah Vertikal di malam hari"
            ),
            EmergencySafetyGuide(
                id = "SG-03",
                category = "DARURAT LAMBUNG",
                title = "Kebocoran Lambung & Masuk Air (Hull Breach)",
                priority = "KRITIS",
                description = "Penanganan kebocoran kapal akibat menabrak karang, kayu hanyut, atau retak lambung.",
                actionSteps = listOf(
                    "1. Seluruh awak kapal wajib langsung mengenakan Jaket Pelampung (Life Jacket).",
                    "2. Hidupkan seluruh Pompa Bilga (Bilge Pump) elektrik dan siapkan ember/pompa manual untuk membuang air keluar secepatnya.",
                    "3. Lokalisir titik kebocoran di lambung bawah atau ruang palka.",
                    "4. Tutup lubang kebocoran dari luar menggunakan terpal tebal/matras, atau dari dalam dengan pasak kayu kerucut & semen cepat kering/epoxy kapal.",
                    "5. Arahkan perahu ke perairan dangkal atau pantai terdekat (beaching) jika laju air melebihi kapasitas pompa.",
                    "6. Siapkan EPIRB, roket suar darurat, dan rakit penolong (Liferaft)."
                ),
                internationalSignal = "MAYDAY MAYDAY MAYDAY di VHF Channel 16 atau Satelit 115"
            ),
            EmergencySafetyGuide(
                id = "SG-04",
                category = "MEDIS & RESUSITASI",
                title = "Pertolongan Pertama Tenggelam & Hipotermia di Laut",
                priority = "KRITIS",
                description = "Tata laksana medis darurat pada korban hampir tenggelam dan kedinginan ekstrem.",
                actionSteps = listOf(
                    "1. Angkat korban dari air dalam posisi horizontal untuk mencegah syok penurunan tekanan darah.",
                    "2. Periksa kesadaran dan pernapasan. Jika tidak bernapas, segera lakukan RJP (Resusitasi Jantung Paru): 30 kompresi dada + 2 napas buatan secara berulang.",
                    "3. Miringkan posisi kepala korban (Recovery Position) jika korban batuk mengeluarkan air laut agar tidak tersedak ke paru.",
                    "4. Tanggalkan pakaian basah, keringkan tubuh, dan bungkus dengan selimut hangat / aluminium thermal foil.",
                    "5. Jangan menggosok kulit korban yang hipotermia dengan keras, berikan minuman manis hangat jika sadar penuh."
                ),
                internationalSignal = "Panggilan MEDICO / Pan-Pan Medis di VHF Channel 16"
            ),
            EmergencySafetyGuide(
                id = "SG-05",
                category = "SINYAL VISUAL",
                title = "Panduan Sinyal Visual & Isyarat Bahaya Internasional",
                priority = "STANDAR",
                description = "Kode isyarat darurat saat komunikasi radio/satelit terputus.",
                actionSteps = listOf(
                    "1. Roket Suar Parasut Merah (Parachute Flare): Tembakkan tegak lurus ke atas saat melihat kapal/pesawat SAR melintas di kejauhan.",
                    "2. Hand Flare Merah: Nyalakan di sisi bawah angin (lee side) agar asap dan percikan api tidak mengenai tubuh/kapal.",
                    "3. Sinyal Asap Oranye (Orange Smoke): Gunakan di siang hari untuk memandu helikopter atau kapal penyelamat mendekat.",
                    "4. Cermin Sinyal Heliograf: Pantulkan cahaya matahari ke arah kapal penyelamat atau pesawat pencari.",
                    "5. Kode Morse S.O.S dengan senter/peluit: Tiga pendek (...), Tiga panjang (---), Tiga pendek (...) diulang berkala."
                ),
                internationalSignal = "SOS: ... --- ... (Tiga Pendek - Tiga Panjang - Tiga Pendek)"
            )
        )
    }

    fun getIndonesianPorts(): List<PortFacility> {
        return listOf(
            PortFacility(
                id = "P-01",
                name = "PPS Nizam Zachman (Muara Baru)",
                city = "DKI Jakarta",
                latitude = -6.1150,
                longitude = 106.8050,
                capacityVessels = 1200,
                vhfChannel = "Ch 12 & 16",
                phoneOperator = "(021) 6620137",
                fuelAvailable = true,
                freshWaterAvailable = true,
                iceFactoryAvailable = true
            ),
            PortFacility(
                id = "P-02",
                name = "PPS Cilacap",
                city = "Cilacap, Jawa Tengah",
                latitude = -7.7450,
                longitude = 109.0150,
                capacityVessels = 850,
                vhfChannel = "Ch 14 & 16",
                phoneOperator = "(0282) 534120",
                fuelAvailable = true,
                freshWaterAvailable = true,
                iceFactoryAvailable = true
            ),
            PortFacility(
                id = "P-03",
                name = "PPS Bitung",
                city = "Bitung, Sulawesi Utara",
                latitude = 1.4420,
                longitude = 125.1950,
                capacityVessels = 950,
                vhfChannel = "Ch 16",
                phoneOperator = "(0438) 31050",
                fuelAvailable = true,
                freshWaterAvailable = true,
                iceFactoryAvailable = true
            ),
            PortFacility(
                id = "P-04",
                name = "PPS Belawan",
                city = "Medan, Sumatera Utara",
                latitude = 3.7850,
                longitude = 98.6920,
                capacityVessels = 700,
                vhfChannel = "Ch 16 & 08",
                phoneOperator = "(061) 6941420",
                fuelAvailable = true,
                freshWaterAvailable = true,
                iceFactoryAvailable = true
            ),
            PortFacility(
                id = "P-05",
                name = "PPN Banyuwangi (Muncar)",
                city = "Banyuwangi, Jawa Timur",
                latitude = -8.4350,
                longitude = 114.3350,
                capacityVessels = 1100,
                vhfChannel = "Ch 16",
                phoneOperator = "(0333) 593214",
                fuelAvailable = true,
                freshWaterAvailable = true,
                iceFactoryAvailable = true
            )
        )
    }

    fun getZppiFishingZones(): List<ZppiFishingZone> {
        return listOf(
            ZppiFishingZone(
                id = "ZPPI-01",
                name = "Zona Pelagis P. Pari & P. Damar (Thermal Front)",
                wppZone = "WPP 712 (Laut Jawa)",
                targetSpecies = "Tongkol, Kembung & Selar",
                latitude = -5.8600,
                longitude = 106.7200,
                sstCelsius = 28.6f,
                chlorophyllA = 0.92f,
                potentialScore = 94,
                peakHours = "04:30 - 08:30 WIB",
                recommendedDepthM = "15 - 35 meter",
                gearType = "Jaring Insang Hanyut / Handline"
            ),
            ZppiFishingZone(
                id = "ZPPI-02",
                name = "Spot Cakalang & Madidihang Selat Sunda",
                wppZone = "WPP 572 (Selat Sunda)",
                targetSpecies = "Cakalang & Tuna Sirip Kuning",
                latitude = -5.9800,
                longitude = 105.9500,
                sstCelsius = 27.8f,
                chlorophyllA = 0.88f,
                potentialScore = 89,
                peakHours = "05:00 - 09:00 & 16:30 - 19:30 WIB",
                recommendedDepthM = "25 - 60 meter",
                gearType = "Rawai Hanyut (Longline) / Tonda"
            ),
            ZppiFishingZone(
                id = "ZPPI-03",
                name = "Zona Upwelling Cirebon - Indramayu",
                wppZone = "WPP 712 (Laut Jawa)",
                targetSpecies = "Tenggiri & Bawal Putih",
                latitude = -6.1500,
                longitude = 108.7500,
                sstCelsius = 29.1f,
                chlorophyllA = 0.76f,
                potentialScore = 86,
                peakHours = "05:30 - 10:00 WIB",
                recommendedDepthM = "18 - 40 meter",
                gearType = "Jaring Insang Monofilamen"
            ),
            ZppiFishingZone(
                id = "ZPPI-04",
                name = "Drop-off Tubiran Karang Teluk Jakarta",
                wppZone = "WPP 712 (Teluk Jakarta)",
                targetSpecies = "Kakap Merah, Kerapu & Kurisi",
                latitude = -5.7900,
                longitude = 106.8800,
                sstCelsius = 29.4f,
                chlorophyllA = 0.65f,
                potentialScore = 82,
                peakHours = "Fajar & Senang Hari (Pasang)",
                recommendedDepthM = "35 - 55 meter",
                gearType = "Pancing Dasar (Bottom Handline) / Bubu"
            ),
            ZppiFishingZone(
                id = "ZPPI-05",
                name = "Zona Cumi-cumi & Pelagis Natuna Selatan",
                wppZone = "WPP 711 (Laut Natuna)",
                targetSpecies = "Cumi-cumi, Teri & Kembung",
                latitude = 3.8500,
                longitude = 108.4000,
                sstCelsius = 29.2f,
                chlorophyllA = 0.81f,
                potentialScore = 88,
                peakHours = "Malam Hari (Bulan Gelap / Light Fishing)",
                recommendedDepthM = "20 - 45 meter",
                gearType = "Bagan Perahu / Bouke Ami"
            )
        )
    }

    fun getMockSonarFishDetections(): List<SonarFishDetection> {
        return listOf(
            SonarFishDetection(depthMeters = 14.5f, fishSize = "SEDANG", estimatedWeightKg = 2.4f, xOffsetPercent = 0.25f),
            SonarFishDetection(depthMeters = 22.8f, fishSize = "BESAR", estimatedWeightKg = 8.5f, xOffsetPercent = 0.55f),
            SonarFishDetection(depthMeters = 31.2f, fishSize = "SEDANG", estimatedWeightKg = 3.1f, xOffsetPercent = 0.78f),
            SonarFishDetection(depthMeters = 38.0f, fishSize = "KECIL", estimatedWeightKg = 0.8f, xOffsetPercent = 0.40f)
        )
    }

    fun getColregsRules(): List<ColregsRule> {
        return listOf(
            ColregsRule(
                id = "COL-14",
                ruleNumber = "Aturan 14 (Rule 14)",
                title = "Situasi Berhadapan Langsung (Head-on Situation)",
                situation = "Dua kapal bertenaga mesin berlayar dengan haluan berlawanan yang berisiko tabrakan.",
                actionRequired = "KEDUA KAPAL WAJIB MERUBAH HALUAN KE KANAN (STARBOARD) dan saling berpapasan di lambung kiri (port to port).",
                nightLights = "Melihat kedua lampu lambung (Merah & Hijau) dan lampu tiang putih kapal lawan tepat di depan.",
                soundSignal = "1 Tiupan Pendek (1 detik) = 'Saya merubah haluan ke kanan'."
            ),
            ColregsRule(
                id = "COL-15",
                ruleNumber = "Aturan 15 (Rule 15)",
                title = "Situasi Bersilangan (Crossing Situation)",
                situation = "Dua kapal bertenaga mesin saling memotong lintasan satu sama lain.",
                actionRequired = "Kapal yang melihat kapal lain di sisi LAMBUNG KANANNYA (Starboard) adalah Kapal yang Wajib Menghindar (Give-way). Hindari memotong di depan haluan kapal lawan.",
                nightLights = "Jika melihat lampu Merah kapal lawan di sisi kanan Anda: WAJIB MENGHINDAR. Jika melihat lampu Hijau: Anda berhak jalan (Stand-on), tetap waspada.",
                soundSignal = "5 Tiupan Pendek atau Cepat = Isyarat keraguan/bahaya jika kapal lawan tidak menghindar."
            ),
            ColregsRule(
                id = "COL-13",
                ruleNumber = "Aturan 13 (Rule 13)",
                title = "Situasi Menyusul / Mendahului (Overtaking)",
                situation = "Sebuah kapal mendekati kapal lain dari arah lebih dari 22.5° di belakang lambung (sektor lampu buritan).",
                actionRequired = "Kapal yang menyusul WAJIB MEMBERI JALAN dan menjaga jarak aman sampai benar-benar bebas dari kapal yang disusul.",
                nightLights = "Hanya melihat lampu buritan putih kapal di depan (tanpa melihat lampu merah/hijau).",
                soundSignal = "2 Tiupan Panjang + 1 Tiupan Pendek = 'Saya hendak menyusul dari lambung kanan Anda'."
            ),
            ColregsRule(
                id = "COL-19",
                ruleNumber = "Aturan 19 & 35",
                title = "Pelayaran dalam Kabut / Jarak Pandang Terbatas",
                situation = "Kondisi kabut tebal, hujan badai lebat, atau asap laut tebal.",
                actionRequired = "Kurangi kecepatan ke batas aman (Safe Speed). Nyalakan radar & pendengaran siaga. Bunyikan isyarat suling kabut secara periodik.",
                nightLights = "Nyalakan seluruh lampu navigasi wajib (Lampu Tiang, Lambung Kanan-Kiri, dan Buritan).",
                soundSignal = "1 Tiupan Panjang setiap 2 menit (Kapal bertenaga mesin berlayar)."
            )
        )
    }

    fun getNavigationalAids(): List<NavigationalAid> {
        return listOf(
            NavigationalAid(
                id = "ATON-01",
                name = "Pelampung Suar Barat Alur Priok No. 1",
                type = BuoyType.PORT_HAND,
                latitude = -5.9820,
                longitude = 106.8750,
                lightFlash = "Fl R 4s (Merah)"
            ),
            NavigationalAid(
                id = "ATON-02",
                name = "Pelampung Suar Timur Alur Priok No. 2",
                type = BuoyType.STARBOARD_HAND,
                latitude = -5.9815,
                longitude = 106.8850,
                lightFlash = "Fl G 4s (Hijau)"
            ),
            NavigationalAid(
                id = "ATON-03",
                name = "Tanda Kardinal Utara Karang Gosong P. Ayer",
                type = BuoyType.NORTH_CARDINAL,
                latitude = -5.9200,
                longitude = 106.8200,
                lightFlash = "Q W (Putih Cepat Kontinu)"
            ),
            NavigationalAid(
                id = "ATON-04",
                name = "Tanda Kardinal Selatan Karang Berang",
                type = BuoyType.SOUTH_CARDINAL,
                latitude = -5.8700,
                longitude = 106.8600,
                lightFlash = "Q(6) + LFl W 15s"
            ),
            NavigationalAid(
                id = "ATON-05",
                name = "Tanda Bahaya Terpencil Karang Pasir Teluk",
                type = BuoyType.ISOLATED_DANGER,
                latitude = -5.9450,
                longitude = 106.7700,
                lightFlash = "Fl(2) W 5s (Putih 2 Kilatan)"
            ),
            NavionicsAidSafeWaterJakarta(),
            // ================= PELABUHAN PATIMBAN BUOYS =================
            NavigationalAid(
                id = "ATON-PTB-01",
                name = "Pelampung Suar Merah Alur Patimban No. 1",
                type = BuoyType.PORT_HAND,
                latitude = -6.2100,
                longitude = 107.9000,
                lightFlash = "Fl R 4s (Merah Silinder)"
            ),
            NavigationalAid(
                id = "ATON-PTB-02",
                name = "Pelampung Suar Hijau Alur Patimban No. 2",
                type = BuoyType.STARBOARD_HAND,
                latitude = -6.2100,
                longitude = 107.9100,
                lightFlash = "Fl G 4s (Hijau Kerucut)"
            ),
            NavigationalAid(
                id = "ATON-PTB-03",
                name = "Pelampung Suar Merah Kolam Putar Patimban No. 3",
                type = BuoyType.PORT_HAND,
                latitude = -6.2250,
                longitude = 107.9020,
                lightFlash = "Fl R 2s"
            ),
            NavigationalAid(
                id = "ATON-PTB-04",
                name = "Pelampung Suar Hijau Kolam Putar Patimban No. 4",
                type = BuoyType.STARBOARD_HAND,
                latitude = -6.2250,
                longitude = 107.9120,
                lightFlash = "Fl G 2s"
            ),
            NavigationalAid(
                id = "ATON-PTB-SAFE",
                name = "Pelampung Suar Alur Aman Luar Patimban (Fairway Buoy)",
                type = BuoyType.SAFE_WATER,
                latitude = -6.1850,
                longitude = 107.9050,
                lightFlash = "Iso W 4s (Merah-Putih)"
            ),
            // ================= PLTU INDRAMAYU & ERETAN / BALONGAN BUOYS =================
            NavigationalAid(
                id = "ATON-PLTU-01",
                name = "Pelampung Khusus Dermaga Batubara PLTU Indramayu No. 1",
                type = BuoyType.NORTH_CARDINAL,
                latitude = -6.2850,
                longitude = 108.0050,
                lightFlash = "Fl Y 3s (Kuning Khusus PLTU)"
            ),
            NavigationalAid(
                id = "ATON-PLTU-02",
                name = "Pelampung Khusus Kolam Jetty PLTU Indramayu No. 2",
                type = BuoyType.SOUTH_CARDINAL,
                latitude = -6.2850,
                longitude = 108.0200,
                lightFlash = "Fl Y 3s (Kuning Khusus PLTU)"
            ),
            NavigationalAid(
                id = "ATON-PLTU-SAFE",
                name = "Pelampung Alur Aman Tongkang Batubara PLTU Sumuradem",
                type = BuoyType.SAFE_WATER,
                latitude = -6.2650,
                longitude = 108.0120,
                lightFlash = "Iso W 4s (Aman Alur Dalam)"
            ),
            NavigationalAid(
                id = "ATON-ERETAN-01",
                name = "Pelampung Suar Masuk Muara Nelayan Eretan Kulon",
                type = BuoyType.STARBOARD_HAND,
                latitude = -6.3100,
                longitude = 108.0350,
                lightFlash = "Fl G 3s (Hijau Pintu Muara)"
            ),
            NavigationalAid(
                id = "ATON-BALONGAN-SPM",
                name = "Pelampung Mooring Single Point (SPM Pertamina Balongan)",
                type = BuoyType.ISOLATED_DANGER,
                latitude = -6.3400,
                longitude = 108.3900,
                lightFlash = "Fl(2) Y 10s (Area Terlarang Manuver Tanker)"
            )
        )
    }

    private fun NavionicsAidSafeWaterJakarta(): NavigationalAid {
        return NavigationalAid(
            id = "ATON-06",
            name = "Pelampung Alur Aman Luar Teluk Jakarta",
            type = BuoyType.SAFE_WATER,
            latitude = -5.9100,
            longitude = 106.9000,
            lightFlash = "Iso W 4s (Merah-Putih)"
        )
    }

    fun getLighthouses(): List<Lighthouse> {
        return listOf(
            Lighthouse(
                id = "LIGHT-01",
                name = "Mercusuar P. Damar Besar (Edam)",
                latitude = -5.9550,
                longitude = 106.8450,
                lightCharacter = "Fl(4) W 20s 56m 22M",
                rangeNM = 22.0f,
                lightColor = WarningAmber
            ),
            Lighthouse(
                id = "LIGHT-02",
                name = "Menara Suar Tanjung Priok",
                latitude = -6.0900,
                longitude = 106.8800,
                lightCharacter = "Fl G 5s 18m 12M",
                rangeNM = 12.0f,
                lightColor = SeafoamGreen
            ),
            Lighthouse(
                id = "LIGHT-03",
                name = "Mercusuar P. Bokor (P. Rambut)",
                latitude = -5.9750,
                longitude = 106.7150,
                lightCharacter = "Fl W 10s 25m 15M",
                rangeNM = 15.0f,
                lightColor = Color.White
            ),
            Lighthouse(
                id = "LIGHT-PTB-01",
                name = "Menara Suar Pelabuhan Patimban (Subang)",
                latitude = -6.2350,
                longitude = 107.9050,
                lightCharacter = "Fl W 5s 30m 18M",
                rangeNM = 18.0f,
                lightColor = WarningAmber
            ),
            Lighthouse(
                id = "LIGHT-IND-01",
                name = "Menara Suar Eretan - Sukra (Indramayu)",
                latitude = -6.3100,
                longitude = 108.0200,
                lightCharacter = "Fl W 6s 22m 14M",
                rangeNM = 14.0f,
                lightColor = Color.White
            ),
            Lighthouse(
                id = "LIGHT-BLG-01",
                name = "Mercusuar Pelabuhan Balongan Indramayu",
                latitude = -6.3600,
                longitude = 108.3850,
                lightCharacter = "Fl(3) W 15s 35m 18M",
                rangeNM = 18.0f,
                lightColor = WarningAmber
            )
        )
    }

    fun getSpotSoundings(): List<SpotSounding> {
        return listOf(
            // Jakarta & Kepulauan Seribu
            SpotSounding(-6.0650, 106.8350, 2.8),
            SpotSounding(-6.0500, 106.8500, 3.4),
            SpotSounding(-6.0350, 106.8200, 4.2),
            SpotSounding(-5.9900, 106.8400, 7.8),
            SpotSounding(-5.9750, 106.8650, 9.6),
            SpotSounding(-5.9600, 106.8300, 11.2),
            SpotSounding(-5.9300, 106.8450, 18.4),
            SpotSounding(-5.9100, 106.8200, 22.6),
            SpotSounding(-5.8850, 106.8500, 28.2),
            SpotSounding(-5.8400, 106.8400, 42.0),

            // ================= PELABUHAN PATIMBAN SPOT SOUNDINGS =================
            SpotSounding(-6.2380, 107.9060, 4.5), // Dermaga Pesisir Patimban
            SpotSounding(-6.2310, 107.9040, 9.2), // Kolam Putar Patimban
            SpotSounding(-6.2230, 107.9050, 12.8), // Alur Tengah Patimban
            SpotSounding(-6.2120, 107.9060, 14.5), // Alur Luar Masuk Patimban
            SpotSounding(-6.1950, 107.9050, 18.0), // Pintu Masuk Alur Luar
            SpotSounding(-6.1800, 107.9050, 24.5), // Lepas Pantai Patimban

            // ================= PLTU INDRAMAYU & ERETAN SPOT SOUNDINGS =================
            SpotSounding(-6.3050, 108.0100, 3.2), // Pantai Sukra PLTU
            SpotSounding(-6.2920, 108.0120, 7.8), // Kolam Jetty Batubara PLTU
            SpotSounding(-6.2840, 108.0100, 11.4), // Alur Masuk Tongkang Batubara
            SpotSounding(-6.2700, 108.0150, 16.2), // Lepas Pantai Sumuradem PLTU
            SpotSounding(-6.3150, 108.0350, 2.5), // Muara Eretan Kulon
            SpotSounding(-6.2900, 108.0450, 13.6), // Fishing Ground Eretan
            SpotSounding(-6.3400, 108.3880, 22.0), // Area SPM Balongan
            SpotSounding(-6.3000, 108.3800, 28.5) // Laut Terbuka Balongan
        )
    }
}

