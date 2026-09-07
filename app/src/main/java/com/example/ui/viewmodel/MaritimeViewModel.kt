package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.MaritimeRepository
import com.example.data.util.MaritimeAlertHelper
import com.example.data.util.MaritimeMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import com.example.ui.theme.*

data class UserVesselState(
    val name: String = "KM. BINTANG LAUT (Milik Anda)",
    val registrationNo: String = "ID-JKT-8821",
    val grossTonnage: Int = 15,
    val captainName: String = "Capt. Budi Santoso",
    val crewCount: Int = 4,
    val latitude: Double = -5.9320,
    val longitude: Double = 106.8380,
    val currentSpeedKnots: Double = 6.2,
    val headingDeg: Float = 45f,
    val engineHours: Int = 185,
    val batteryPercent: Int = 94,
    val fuelLiter: Int = 280,
    val maxFuelLiter: Int = 400
)

data class MaritimeUiState(
    val selectedTab: Int = 0,
    val userVessel: UserVesselState = UserVesselState(),
    val vessels: List<Vessel> = emptyList(),
    val selectedVessel: Vessel? = null,
    val vesselFilter: VesselType? = null,
    val weatherAreas: List<MaritimeWeatherArea> = emptyList(),
    val selectedWeatherArea: MaritimeWeatherArea? = null,
    val highRiskZones: List<HighRiskZonePrediction> = emptyList(),
    val offlineSafetyGuides: List<EmergencySafetyGuide> = emptyList(),
    val ports: List<PortFacility> = emptyList(),
    val zppiZones: List<ZppiFishingZone> = emptyList(),
    val selectedZppi: ZppiFishingZone? = null,
    val showZppiLayerOnMap: Boolean = true,
    val sonarDetections: List<SonarFishDetection> = emptyList(),
    val sonarDepthMeters: Float = 34.8f,
    val sonarWaterTempC: Float = 28.2f,
    val colregsRules: List<ColregsRule> = emptyList(),
    val navigationalAids: List<NavigationalAid> = emptyList(),
    val lighthouses: List<Lighthouse> = emptyList(),
    val spotSoundings: List<SpotSounding> = emptyList(),
    val navionicsChartLayer: NavionicsChartLayer = NavionicsChartLayer.SATELLITE_OVERLAY,
    val safetyDepthMeters: Float = 3.0f,
    val isRulerToolActive: Boolean = false,
    val rulerStartPoint: Pair<Double, Double>? = null,
    val rulerEndPoint: Pair<Double, Double>? = null,
    val selectedAtoN: NavigationalAid? = null,
    val selectedLighthouse: Lighthouse? = null,
    val selectedNavWaypoint: FishNetWaypointEntity? = null,
    val currentSeaLocationName: String = "Teluk Jakarta & Kepulauan Seribu",
    val isUsingRealDeviceGps: Boolean = false,
    val highContrastSunMode: Boolean = false,
    val isEmergencyActive: Boolean = false,
    val lastSosMessage: String? = null,
    val isSatelliteConnected: Boolean = true,
    val bmkgAlarmTriggered: Boolean = false,
    val dangerousCpaTargets: List<CpaCalculationResult> = emptyList(),
    val anchorWatch: AnchorWatchState = AnchorWatchState(),
    val isNightVisionActive: Boolean = false,
    val isRecordingTrack: Boolean = true,
    val breadcrumbTracks: List<BreadcrumbTrackPoint> = emptyList(),
    val tidePrediction: TideCurrentPrediction? = null
)

class MaritimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MaritimeRepository
    private val _uiState = MutableStateFlow(MaritimeUiState())
    val uiState: StateFlow<MaritimeUiState> = _uiState.asStateFlow()

    // Room Flow observers
    val waypoints: StateFlow<List<FishNetWaypointEntity>>
    val catchLogs: StateFlow<List<FishCatchLogEntity>>
    val maintenances: StateFlow<List<ShipMaintenanceEntity>>
    val sosRecords: StateFlow<List<EmergencySosEntity>>
    val totalCatchWeight: StateFlow<Double?>
    val totalRevenue: StateFlow<Double?>

    init {
        val db = MaritimeDatabase.getDatabase(application)
        repository = MaritimeRepository(db)

        waypoints = repository.waypoints.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        catchLogs = repository.catchLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        maintenances = repository.maintenances.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        sosRecords = repository.sosRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        totalCatchWeight = repository.totalCatchWeight.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        totalRevenue = repository.totalRevenue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        loadInitialData()
        startLiveShipSimulation()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()

            val weatherList = repository.getIndonesianMaritimeWeatherAreas()
            val vesselsList = repository.getIndonesianVessels()
            val riskList = repository.getHighRiskZones()
            val guides = repository.getOfflineSafetyGuides()
            val portList = repository.getIndonesianPorts()
            val zppiList = repository.getZppiFishingZones()
            val sonarList = repository.getMockSonarFishDetections()
            val colregsList = repository.getColregsRules()
            val atons = repository.getNavigationalAids()
            val lights = repository.getLighthouses()
            val soundings = repository.getSpotSoundings()

            _uiState.update { current ->
                val baseState = current.copy(
                    weatherAreas = weatherList,
                    selectedWeatherArea = weatherList.firstOrNull(),
                    vessels = vesselsList,
                    highRiskZones = riskList,
                    offlineSafetyGuides = guides,
                    ports = portList,
                    zppiZones = zppiList,
                    sonarDetections = sonarList,
                    colregsRules = colregsList,
                    navigationalAids = atons,
                    lighthouses = lights,
                    spotSoundings = soundings
                )
                checkNavigationAlerts(baseState)
            }
        }
    }

    private fun startLiveShipSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(3500)
                _uiState.update { current ->
                    // Gently simulate live movement of ships in ocean
                    val updatedVessels = current.vessels.map { v ->
                        val latOffset = (Math.random() - 0.5) * 0.0015
                        val lonOffset = (Math.random() - 0.5) * 0.0015
                        val newHeading = (v.headingDeg + (Math.random() - 0.5) * 2).toFloat() % 360f
                        v.copy(
                            latitude = v.latitude + latOffset,
                            longitude = v.longitude + lonOffset,
                            headingDeg = if (newHeading < 0) newHeading + 360f else newHeading
                        )
                    }
                    val updatedState = current.copy(vessels = updatedVessels)
                    checkNavigationAlerts(updatedState)
                }
            }
        }
    }

    private fun checkNavigationAlerts(current: MaritimeUiState): MaritimeUiState {
        val user = current.userVessel
        // 1. Calculate CPA / TCPA for all vessels
        val dangerousList = current.vessels.mapNotNull { v ->
            MaritimeMath.calculateCPAandTCPA(
                userLat = user.latitude,
                userLon = user.longitude,
                userSpeedKnots = user.currentSpeedKnots,
                userHeadingDeg = user.headingDeg,
                targetLat = v.latitude,
                targetLon = v.longitude,
                targetSpeedKnots = v.speedKnots,
                targetHeadingDeg = v.headingDeg
            )?.copy(vessel = v)
        }.filter { it.isDangerous }

        // 2. Check Anchor Drag
        var updatedAnchor = current.anchorWatch
        if (current.anchorWatch.isActive) {
            val distKm = MaritimeMath.calculateDistanceKm(
                user.latitude,
                user.longitude,
                current.anchorWatch.anchorLat,
                current.anchorWatch.anchorLon
            )
            val distMeters = (distKm * 1000.0).toFloat()
            val isAlarm = distMeters > current.anchorWatch.radiusMeters
            updatedAnchor = current.anchorWatch.copy(
                currentDistanceMeters = distMeters,
                isAlarmTriggered = isAlarm
            )
        }

        // 3. Update Breadcrumb Tracks if recording
        var updatedTracks = current.breadcrumbTracks
        if (current.isRecordingTrack) {
            val lastPoint = updatedTracks.lastOrNull()
            if (lastPoint == null || MaritimeMath.calculateDistanceKm(lastPoint.latitude, lastPoint.longitude, user.latitude, user.longitude) > 0.012) {
                updatedTracks = (updatedTracks + BreadcrumbTrackPoint(user.latitude, user.longitude)).takeLast(300)
            }
        }

        // 4. Update Tide & Current prediction
        val tide = MaritimeMath.predictTideAndCurrent(user.latitude, user.longitude)

        return current.copy(
            dangerousCpaTargets = dangerousList,
            anchorWatch = updatedAnchor,
            breadcrumbTracks = updatedTracks,
            tidePrediction = tide
        )
    }

    fun toggleNightVision() {
        _uiState.update { it.copy(isNightVisionActive = !it.isNightVisionActive) }
    }

    fun startAnchorWatch(radiusMeters: Float = 50f) {
        val user = _uiState.value.userVessel
        val timeStr = SimpleDateFormat("HH:mm WIB", Locale("id", "ID")).format(Date())
        _uiState.update {
            it.copy(
                anchorWatch = AnchorWatchState(
                    isActive = true,
                    anchorLat = user.latitude,
                    anchorLon = user.longitude,
                    radiusMeters = radiusMeters,
                    currentDistanceMeters = 0f,
                    isAlarmTriggered = false,
                    setTimeString = timeStr
                )
            )
        }
    }

    fun stopAnchorWatch() {
        _uiState.update { it.copy(anchorWatch = AnchorWatchState(isActive = false)) }
    }

    fun dismissAnchorAlarm() {
        _uiState.update { it.copy(anchorWatch = it.anchorWatch.copy(isAlarmTriggered = false)) }
    }

    fun dismissCpaAlarm() {
        _uiState.update { it.copy(dangerousCpaTargets = emptyList()) }
    }

    fun toggleTrackRecording() {
        _uiState.update { it.copy(isRecordingTrack = !it.isRecordingTrack) }
    }

    fun clearTrackRecording() {
        _uiState.update { it.copy(breadcrumbTracks = emptyList()) }
    }

    fun backtrackRoute() {
        val tracks = _uiState.value.breadcrumbTracks
        if (tracks.isNotEmpty()) {
            val firstPoint = tracks.first()
            val entity = FishNetWaypointEntity(
                id = 88888,
                title = "Jalur Putar Balik (Awal Jejak)",
                type = "SPOT_IKAN",
                latitude = firstPoint.latitude,
                longitude = firstPoint.longitude,
                depthMeters = _uiState.value.sonarDepthMeters.toDouble(),
                baitOrGearNotes = "Titik balik aman navigasi",
                targetRetrievalDate = "Hari Ini",
                driftDistanceMeters = 0.0,
                tagColor = "#FFD166"
            )
            _uiState.update { it.copy(selectedNavWaypoint = entity) }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectWeatherArea(area: MaritimeWeatherArea) {
        _uiState.update { it.copy(selectedWeatherArea = area) }
    }

    fun selectVessel(vessel: Vessel?) {
        _uiState.update { it.copy(selectedVessel = vessel) }
    }

    fun setVesselFilter(filter: VesselType?) {
        _uiState.update { it.copy(vesselFilter = filter) }
    }

    fun toggleHighContrastSunMode() {
        _uiState.update { it.copy(highContrastSunMode = !it.highContrastSunMode) }
    }

    fun setNavWaypoint(waypoint: FishNetWaypointEntity?) {
        _uiState.update { it.copy(selectedNavWaypoint = waypoint) }
    }

    fun triggerBmkgAlarm(context: Context) {
        MaritimeAlertHelper.playWarningBeep(context)
        _uiState.update { it.copy(bmkgAlarmTriggered = true) }
    }

    fun dismissBmkgAlarm() {
        _uiState.update { it.copy(bmkgAlarmTriggered = false) }
    }

    // Waypoint Actions
    fun saveNewWaypoint(
        title: String,
        type: String,
        latitude: Double,
        longitude: Double,
        depthMeters: Double,
        notes: String,
        targetRetrieval: String,
        colorHex: String = "#00B4D8"
    ) {
        viewModelScope.launch {
            val waypoint = FishNetWaypointEntity(
                title = title.ifBlank { "Titik Koordinat Laut #${System.currentTimeMillis() % 1000}" },
                type = type,
                latitude = latitude,
                longitude = longitude,
                depthMeters = depthMeters,
                baitOrGearNotes = notes,
                targetRetrievalDate = targetRetrieval.ifBlank { "Besok Pagi" },
                driftDistanceMeters = if (type == "JARING_HANYUT") 450.0 else 0.0,
                tagColor = colorHex
            )
            repository.addWaypoint(waypoint)
        }
    }

    fun toggleWaypointRetrieved(waypoint: FishNetWaypointEntity) {
        viewModelScope.launch {
            repository.updateWaypointRetrieved(waypoint.id, !waypoint.isRetrieved)
        }
    }

    fun deleteWaypoint(waypoint: FishNetWaypointEntity) {
        viewModelScope.launch {
            if (_uiState.value.selectedNavWaypoint?.id == waypoint.id) {
                _uiState.update { it.copy(selectedNavWaypoint = null) }
            }
            repository.deleteWaypoint(waypoint)
        }
    }

    // Catch Log Actions
    fun recordCatchLog(
        species: String,
        weightKg: Double,
        pricePerKg: Double,
        fishingGear: String,
        wppZone: String,
        notes: String
    ) {
        viewModelScope.launch {
            val userLat = _uiState.value.userVessel.latitude
            val userLon = _uiState.value.userVessel.longitude
            val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
            val log = FishCatchLogEntity(
                dateStr = formatter.format(Date()),
                fishSpecies = species,
                weightKg = weightKg,
                pricePerKg = pricePerKg,
                estimatedRevenue = weightKg * pricePerKg,
                fishingGear = fishingGear,
                wppnriZone = wppZone,
                latitude = userLat,
                longitude = userLon,
                notes = notes
            )
            repository.addCatchLog(log)
        }
    }

    fun deleteCatchLog(log: FishCatchLogEntity) {
        viewModelScope.launch {
            repository.deleteCatchLog(log)
        }
    }

    // Maintenance Actions
    fun saveMaintenance(
        component: String,
        intervalHours: Int,
        guide: String,
        notes: String
    ) {
        viewModelScope.launch {
            val currentEngHours = _uiState.value.userVessel.engineHours
            val item = ShipMaintenanceEntity(
                componentName = component,
                currentEngineHours = currentEngHours,
                intervalHours = intervalHours,
                nextDueEngineHours = currentEngHours + intervalHours,
                lastServiceDate = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()),
                isOverdue = false,
                maintenanceGuide = guide,
                notes = notes
            )
            repository.addMaintenance(item)
        }
    }

    fun markMaintenanceDone(item: ShipMaintenanceEntity) {
        viewModelScope.launch {
            val currentEngHours = _uiState.value.userVessel.engineHours
            val updated = item.copy(
                currentEngineHours = currentEngHours,
                nextDueEngineHours = currentEngHours + item.intervalHours,
                lastServiceDate = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()),
                isOverdue = false
            )
            repository.updateMaintenance(updated)
        }
    }

    fun updateEngineHours(newHours: Int) {
        _uiState.update { current ->
            current.copy(userVessel = current.userVessel.copy(engineHours = newHours))
        }
        viewModelScope.launch {
            // Check overdue
            val currentList = maintenances.value
            currentList.forEach { m ->
                if (newHours >= m.nextDueEngineHours && !m.isOverdue) {
                    repository.updateMaintenance(m.copy(isOverdue = true, currentEngineHours = newHours))
                }
            }
        }
    }

    fun deleteMaintenance(item: ShipMaintenanceEntity) {
        viewModelScope.launch {
            repository.deleteMaintenance(item)
        }
    }

    // Emergency SOS Dispatch
    fun dispatchEmergencySos(
        context: Context,
        distressType: String,
        customNotes: String
    ) {
        val user = _uiState.value.userVessel
        val coordFormatted = MaritimeMath.formatMaritimeCoord(user.latitude, user.longitude)
        val timeFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm:ss 'WIB'", Locale("id", "ID")).format(Date())

        MaritimeAlertHelper.triggerEmergencyAudioAlarm(context)

        val distressTitle = when (distressType) {
            "MESIN_MATI" -> "KERUSAKAN MESIN INDUK (ENGINE BREAKDOWN)"
            "KEBOCORAN_LAMBUNG" -> "KEBOCORAN LAMBUNG & MASUK AIR (HULL LEAK)"
            "ORANG_JATUH_LAUT" -> "ORANG JATUH KE LAUT (MAN OVERBOARD)"
            "CUACA_EKSTREM" -> "GELOMBANG EKSTREM & BADAI LAUT"
            "KEBAKARAN" -> "KEBAKARAN DI KAPAL (FIRE ONBOARD)"
            else -> "DARURAT MEDIS KRITIS DI LAUT"
        }

        val sosPayload = "MAYDAY MAYDAY! ${user.name} (${user.registrationNo}) melaporkan $distressTitle. Posisi: $coordFormatted. Awak: ${user.crewCount} orang. Baterai: ${user.batteryPercent}%. Catatan: $customNotes"

        viewModelScope.launch {
            val sosEntity = EmergencySosEntity(
                timeFormatted = timeFormatted,
                distressType = distressTitle,
                latitude = user.latitude,
                longitude = user.longitude,
                vesselName = user.name,
                captainName = user.captainName,
                crewOnBoard = user.crewCount,
                satelliteRelayStatus = "TERKIRIM (SAT-EPIRB / BASARNAS 115 RELAY)",
                notes = customNotes.ifBlank { "Permintaan bantuan SAR mendesak!" }
            )
            repository.recordEmergencySos(sosEntity)

            _uiState.update {
                it.copy(
                    isEmergencyActive = true,
                    lastSosMessage = sosPayload
                )
            }
        }
    }

    fun dismissEmergency() {
        _uiState.update { it.copy(isEmergencyActive = false) }
    }

    fun deleteSosRecord(sos: EmergencySosEntity) {
        viewModelScope.launch {
            repository.deleteSosRecord(sos)
        }
    }

    fun syncWithRealDeviceGps(context: Context) {
        if (com.example.data.util.DeviceGpsHelper.hasLocationPermission(context)) {
            com.example.data.util.DeviceGpsHelper.requestRealDeviceLocation(context) { lat, lon, speed, heading, zoneName, wppZone ->
                updateFromDeviceGps(lat, lon, speed, heading, zoneName, wppZone)
                android.widget.Toast.makeText(
                    context,
                    "📍 GPS HP Terkunci: $zoneName ($wppZone)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun updateFromDeviceGps(
        lat: Double,
        lon: Double,
        speedKnots: Double = 0.0,
        headingDeg: Float = 0f,
        zoneName: String? = null,
        wppZone: String? = null
    ) {
        val actualZone = zoneName ?: MaritimeMath.inferIndonesianMaritimeZone(lat, lon).first
        val closestArea = _uiState.value.weatherAreas.minByOrNull { area ->
            MaritimeMath.calculateDistanceNM(lat, lon, area.latitude, area.longitude)
        }
        _uiState.update { current ->
            val updated = current.copy(
                isUsingRealDeviceGps = true,
                currentSeaLocationName = actualZone,
                selectedWeatherArea = closestArea ?: current.selectedWeatherArea,
                userVessel = current.userVessel.copy(
                    latitude = lat,
                    longitude = lon,
                    currentSpeedKnots = if (speedKnots > 0) speedKnots else current.userVessel.currentSpeedKnots,
                    headingDeg = if (headingDeg > 0) headingDeg else current.userVessel.headingDeg
                )
            )
            checkNavigationAlerts(updated)
        }
        loadMaritimeDataForRegion(lat, lon)
    }

    fun changeUserSeaLocation(lat: Double, lon: Double, name: String) {
        val closestArea = _uiState.value.weatherAreas.minByOrNull { area ->
            MaritimeMath.calculateDistanceNM(lat, lon, area.latitude, area.longitude)
        }
        _uiState.update { current ->
            val updated = current.copy(
                isUsingRealDeviceGps = false,
                currentSeaLocationName = name,
                selectedWeatherArea = closestArea ?: current.selectedWeatherArea,
                userVessel = current.userVessel.copy(
                    latitude = lat,
                    longitude = lon
                )
            )
            checkNavigationAlerts(updated)
        }
        loadMaritimeDataForRegion(lat, lon)
    }

    fun loadMaritimeDataForRegion(lat: Double, lon: Double) {
        val existingVessels = _uiState.value.vessels
        val nearbyCount = existingVessels.count { v ->
            MaritimeMath.calculateDistanceNM(lat, lon, v.latitude, v.longitude) < 20.0
        }
        val (inferredName, wpp) = MaritimeMath.inferIndonesianMaritimeZone(lat, lon)
        val shortName = inferredName.split(" ", "-", "&").firstOrNull()?.trim() ?: "BAHARI"

        if (nearbyCount < 4) {
            val seed = (abs(lat * 1000).toInt() * 31 + abs(lon * 1000).toInt())
            val dynamicVessels = listOf(
                Vessel(
                    id = "VSL-REG-${seed}-01",
                    name = "KM. $shortName REJEKI ${(seed % 99) + 1}",
                    callSign = "YDB${(seed % 800) + 100}",
                    mmsi = "525${(abs(seed.toLong() * 17)) % 899999 + 100000}",
                    type = VesselType.FISHING,
                    latitude = lat + 0.014,
                    longitude = lon + 0.012,
                    speedKnots = 6.4,
                    headingDeg = ((seed * 47) % 360).toFloat(),
                    destination = "Fishing Ground $wpp",
                    status = "Sedang Melakukan Penangkapan Ikan (Underway)",
                    lengthMeters = 24,
                    draughtMeters = 1.9,
                    grossTonnage = 28,
                    lastUpdated = "Real-time",
                    originPort = inferredName,
                    eta = "Hari ini, 17:00"
                ),
                Vessel(
                    id = "VSL-REG-${seed}-02",
                    name = "TB. $shortName PANDU 0${(seed % 9) + 1}",
                    callSign = "PKTB",
                    mmsi = "525${(abs(seed.toLong() * 23)) % 899999 + 100000}",
                    type = VesselType.TUGBOAT,
                    latitude = lat - 0.009,
                    longitude = lon + 0.016,
                    speedKnots = 5.6,
                    headingDeg = ((seed * 83) % 360).toFloat(),
                    destination = "Dermaga / Alur $inferredName",
                    status = "Memandu Tongkang Muatan",
                    lengthMeters = 34,
                    draughtMeters = 3.6,
                    grossTonnage = 340,
                    lastUpdated = "1 menit lalu",
                    originPort = inferredName,
                    eta = "Operasional Pelabuhan"
                ),
                Vessel(
                    id = "VSL-REG-${seed}-03",
                    name = "KN. SAR PERINTIS ${(seed % 90) + 10}",
                    callSign = "YDA${(seed % 700) + 200}",
                    mmsi = "52599${(seed % 8999) + 1000}",
                    type = VesselType.PATROL,
                    latitude = lat + 0.022,
                    longitude = lon - 0.015,
                    speedKnots = 18.5,
                    headingDeg = ((seed * 113) % 360).toFloat(),
                    destination = "Patroli Siaga Laut $inferredName",
                    status = "Patroli Siaga Laut (Underway)",
                    lengthMeters = 40,
                    draughtMeters = 2.1,
                    grossTonnage = 180,
                    lastUpdated = "Real-time",
                    originPort = "Pangkalan Maritim",
                    eta = "Patroli Aktif"
                ),
                Vessel(
                    id = "VSL-REG-${seed}-04",
                    name = "MV. NUSANTARA MAJU ${(seed % 50) + 1}",
                    callSign = "PKNM",
                    mmsi = "52533${(seed % 8999) + 1000}",
                    type = VesselType.CARGO,
                    latitude = lat + 0.032,
                    longitude = lon + 0.028,
                    speedKnots = 13.4,
                    headingDeg = ((seed * 157) % 360).toFloat(),
                    destination = "Pelabuhan Antar Pulau Indonesia",
                    status = "Berlayar Normal (Underway)",
                    lengthMeters = 145,
                    draughtMeters = 6.8,
                    grossTonnage = 8800,
                    lastUpdated = "Real-time",
                    originPort = "Pelabuhan Wilayah",
                    eta = "Besok Pagi"
                ),
                Vessel(
                    id = "VSL-REG-${seed}-05",
                    name = "MT. SAMUDERA ENERGI 0${(seed % 5) + 1}",
                    callSign = "PKSE",
                    mmsi = "52577${(seed % 8999) + 1000}",
                    type = VesselType.TANKER,
                    latitude = lat + 0.040,
                    longitude = lon - 0.025,
                    speedKnots = 11.2,
                    headingDeg = ((seed * 199) % 360).toFloat(),
                    destination = "Terminal BBM / SPM Pesisir",
                    status = "Muatan BBM Penuh (Underway)",
                    lengthMeters = 165,
                    draughtMeters = 8.5,
                    grossTonnage = 16500,
                    lastUpdated = "2 menit lalu",
                    originPort = "Kilang Pengolahan",
                    eta = "Hari ini, 20:00"
                )
            )
            _uiState.update { current ->
                current.copy(vessels = (current.vessels + dynamicVessels).distinctBy { it.id })
            }
        }

        val existingAtoNs = _uiState.value.navigationalAids
        val nearbyAtoNCount = existingAtoNs.count { a ->
            MaritimeMath.calculateDistanceNM(lat, lon, a.latitude, a.longitude) < 18.0
        }
        if (nearbyAtoNCount < 3) {
            val seed = (abs(lat * 1000).toInt() * 31 + abs(lon * 1000).toInt())
            val dynamicAtoNs = listOf(
                NavigationalAid(
                    id = "ATON-REG-${seed}-01",
                    name = "Pelampung Suar Merah Alur $shortName No. 1",
                    type = BuoyType.PORT_HAND,
                    latitude = lat + 0.009,
                    longitude = lon - 0.007,
                    lightFlash = "Fl R 4s (Merah Silinder)"
                ),
                NavigationalAid(
                    id = "ATON-REG-${seed}-02",
                    name = "Pelampung Suar Hijau Alur $shortName No. 2",
                    type = BuoyType.STARBOARD_HAND,
                    latitude = lat + 0.009,
                    longitude = lon + 0.007,
                    lightFlash = "Fl G 4s (Hijau Kerucut)"
                ),
                NavigationalAid(
                    id = "ATON-REG-${seed}-03",
                    name = "Tanda Kardinal Utara Karang Gosong $shortName",
                    type = BuoyType.NORTH_CARDINAL,
                    latitude = lat + 0.022,
                    longitude = lon + 0.015,
                    lightFlash = "Q W (Putih Cepat)"
                ),
                NavigationalAid(
                    id = "ATON-REG-${seed}-04",
                    name = "Pelampung Suar Alur Aman (Fairway Buoy) $shortName",
                    type = BuoyType.SAFE_WATER,
                    latitude = lat + 0.030,
                    longitude = lon,
                    lightFlash = "Iso W 4s (Merah-Putih)"
                )
            )
            _uiState.update { current ->
                current.copy(navigationalAids = (current.navigationalAids + dynamicAtoNs).distinctBy { it.id })
            }
        }

        val existingLights = _uiState.value.lighthouses
        val nearbyLights = existingLights.count { l ->
            MaritimeMath.calculateDistanceNM(lat, lon, l.latitude, l.longitude) < 25.0
        }
        if (nearbyLights == 0) {
            val seed = (abs(lat * 1000).toInt() * 31 + abs(lon * 1000).toInt())
            val dynamicLight = Lighthouse(
                id = "LIGHT-REG-${seed}",
                name = "Menara Suar Navigasi $shortName",
                latitude = lat - 0.005,
                longitude = lon + 0.002,
                lightCharacter = "Fl W 5s 28m 16M",
                rangeNM = 16.0f,
                lightColor = WarningAmber
            )
            _uiState.update { current ->
                current.copy(lighthouses = (current.lighthouses + dynamicLight).distinctBy { it.id })
            }
        }
    }

    fun toggleZppiMapLayer() {
        _uiState.update { it.copy(showZppiLayerOnMap = !it.showZppiLayerOnMap) }
    }

    fun selectZppi(zppi: ZppiFishingZone?) {
        _uiState.update { it.copy(selectedZppi = zppi) }
    }

    fun setZppiAsNavTarget(zppi: ZppiFishingZone) {
        val wp = FishNetWaypointEntity(
            title = zppi.name,
            type = "SPOT_IKAN",
            latitude = zppi.latitude,
            longitude = zppi.longitude,
            depthMeters = 30.0,
            baitOrGearNotes = "Zona ZPPI Satelit: ${zppi.targetSpecies} (SST ${zppi.sstCelsius}°C, Klorofil ${zppi.chlorophyllA} mg/m³). Jam makan: ${zppi.peakHours}",
            targetRetrievalDate = zppi.peakHours,
            driftDistanceMeters = 0.0,
            currentHeadingDeg = 0f,
            tagColor = "#22D3EE"
        )
        _uiState.update {
            it.copy(
                selectedNavWaypoint = wp,
                selectedZppi = null
            )
        }
    }

    fun saveSonarSpotAsWaypoint(context: Context) {
        val user = _uiState.value.userVessel
        val depth = _uiState.value.sonarDepthMeters.toDouble()
        saveNewWaypoint(
            title = "Spot Sonar Echo (${String.format(Locale.US, "%.1f", depth)}m)",
            type = "SPOT_IKAN",
            latitude = user.latitude,
            longitude = user.longitude,
            depthMeters = depth,
            notes = "Terdeteksi gerombolan ikan di kedalaman 14-38m. Suhu air: ${_uiState.value.sonarWaterTempC}°C.",
            targetRetrieval = "Segera Tebar Jaring/Pancing"
        )
        MaritimeAlertHelper.playWarningBeep(context)
    }

    fun setNavionicsChartLayer(layer: NavionicsChartLayer) {
        _uiState.update { it.copy(navionicsChartLayer = layer) }
    }

    fun setSafetyDepthMeters(depth: Float) {
        _uiState.update { it.copy(safetyDepthMeters = depth) }
    }

    fun toggleRulerTool() {
        _uiState.update {
            it.copy(
                isRulerToolActive = !it.isRulerToolActive,
                rulerStartPoint = null,
                rulerEndPoint = null
            )
        }
    }

    fun setRulerPoints(start: Pair<Double, Double>?, end: Pair<Double, Double>?) {
        _uiState.update {
            it.copy(
                rulerStartPoint = start,
                rulerEndPoint = end
            )
        }
    }

    fun selectAtoN(aton: NavigationalAid?) {
        _uiState.update { it.copy(selectedAtoN = aton, selectedLighthouse = null, selectedVessel = null, selectedZppi = null) }
    }

    fun selectLighthouse(light: Lighthouse?) {
        _uiState.update { it.copy(selectedLighthouse = light, selectedAtoN = null, selectedVessel = null, selectedZppi = null) }
    }
}

