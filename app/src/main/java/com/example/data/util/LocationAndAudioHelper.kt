package com.example.data.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import kotlin.math.*

object MaritimeMath {

    const val EARTH_RADIUS_KM = 6371.0
    const val KM_TO_NAUTICAL_MILE = 0.539957

    /**
     * Calculates distance between 2 GPS coordinates in Kilometers
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculates distance in Nautical Miles (NM)
     */
    fun calculateDistanceNM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return calculateDistanceKm(lat1, lon1, lat2, lon2) * KM_TO_NAUTICAL_MILE
    }

    /**
     * Calculates bearing in degrees (0 to 360) from point 1 to point 2
     */
    fun calculateBearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360.0) % 360.0
        return bearing.toFloat()
    }

    fun getCardinalDirection(bearingDeg: Float): String {
        val directions = arrayOf("U (Utara)", "TL (Timur Laut)", "T (Timur)", "TG (Tenggara)", "S (Selatan)", "BD (Barat Daya)", "B (Barat)", "BL (Barat Laut)")
        val index = ((bearingDeg + 22.5f) / 45.0f).toInt() % 8
        return directions[index]
    }

    /**
     * Estimate current drift coordinates of unattended nets/rumpon based on ocean current
     */
    fun estimateDriftCoordinates(
        startLat: Double,
        startLon: Double,
        currentSpeedKnots: Double,
        currentDirectionDeg: Float,
        elapsedHours: Double
    ): Pair<Double, Double> {
        val distanceNM = currentSpeedKnots * elapsedHours
        val distanceKm = distanceNM / KM_TO_NAUTICAL_MILE
        val angularDist = distanceKm / EARTH_RADIUS_KM

        val radLat1 = Math.toRadians(startLat)
        val radLon1 = Math.toRadians(startLon)
        val radBearing = Math.toRadians(currentDirectionDeg.toDouble())

        val radLat2 = asin(
            sin(radLat1) * cos(angularDist) +
                    cos(radLat1) * sin(angularDist) * cos(radBearing)
        )
        val radLon2 = radLon1 + atan2(
            sin(radBearing) * sin(angularDist) * cos(radLat1),
            cos(angularDist) - sin(radLat1) * sin(radLat2)
        )

        return Pair(Math.toDegrees(radLat2), Math.toDegrees(radLon2))
    }

    /**
     * Formats GPS to standard maritime Lat/Lon (e.g., 05°54.50' S, 106°49.30' E)
     */
    fun formatMaritimeCoord(lat: Double, lon: Double): String {
        val latDirection = if (lat >= 0) "N" else "S"
        val absLat = abs(lat)
        val latDeg = absLat.toInt()
        val latMin = (absLat - latDeg) * 60.0

        val lonDirection = if (lon >= 0) "E" else "W"
        val absLon = abs(lon)
        val lonDeg = absLon.toInt()
        val lonMin = (absLon - lonDeg) * 60.0

        return String.format(java.util.Locale.US, "%02d°%05.2f' %s, %03d°%05.2f' %s", latDeg, latMin, latDirection, lonDeg, lonMin, lonDirection)
    }

    /**
     * Determines Indonesian Marine Water Name & WPPNRI Zone from GPS coordinates
     */
    fun inferIndonesianMaritimeZone(lat: Double, lon: Double): Pair<String, String> {
        return when {
            // Selat Malaka / Aceh / Sumut / Riau
            lat > 1.5 && lon < 105.0 -> Pair("Selat Malaka & Perairan Riau", "WPPNRI 571")
            // Laut Natuna Utara & Kep. Riau
            lat > 0.8 && lon in 105.0..109.5 -> Pair("Laut Natuna & Natuna Utara", "WPPNRI 711")
            // Teluk Jakarta & Kepulauan Seribu
            lat in -6.3..-5.4 && lon in 106.3..107.2 -> Pair("Teluk Jakarta & Kepulauan Seribu", "WPPNRI 712")
            // Selat Sunda / Merak / Bakauheni / Teluk Lampung
            lat in -6.5..-5.2 && lon in 104.5..106.3 -> Pair("Selat Sunda & Pesisir Lampung/Banten", "WPPNRI 572")
            // Laut Jawa (Pantura: Cirebon, Tegal, Pekalongan, Semarang, Tuban, Surabaya, Madura)
            lat in -7.5..-4.0 && lon in 107.2..115.0 -> Pair("Laut Jawa & Perairan Pantura", "WPPNRI 712")
            // Samudera Hindia Selatan Jawa, Bali & Nusa Tenggara
            lat < -7.5 && lon in 105.0..116.0 -> Pair("Samudera Hindia Selatan Jawa & Bali", "WPPNRI 573")
            // Selat Bali & Selat Lombok
            lat in -9.0..-7.5 && lon in 114.0..116.5 -> Pair("Selat Bali & Selat Lombok", "WPPNRI 573")
            // Selat Makassar & Perairan Balikpapan / Donggala
            lat in -5.0..2.5 && lon in 116.5..120.5 -> Pair("Selat Makassar & Perairan Balikpapan", "WPPNRI 713")
            // Laut Flores & Teluk Bone
            lat in -9.0..-4.5 && lon in 119.5..124.0 -> Pair("Laut Flores & Teluk Bone", "WPPNRI 713")
            // Laut Banda & Kepulauan Maluku
            lat in -7.0..-2.0 && lon in 125.0..132.0 -> Pair("Laut Banda & Perairan Maluku", "WPPNRI 714")
            // Laut Arafuru & Merauke
            lat in -10.0..-4.0 && lon in 133.0..141.0 -> Pair("Laut Arafuru & Perairan Merauke", "WPPNRI 718")
            // Default Indonesian waters fallback
            else -> Pair("Perairan Maritim Indonesia (GPS Live)", "WPPNRI 712 / 572")
        }
    }
    /**
     * Calculates Closest Point of Approach (CPA in NM) and Time to CPA (TCPA in minutes)
     */
    fun calculateCPAandTCPA(
        userLat: Double,
        userLon: Double,
        userSpeedKnots: Double,
        userHeadingDeg: Float,
        targetLat: Double,
        targetLon: Double,
        targetSpeedKnots: Double,
        targetHeadingDeg: Float
    ): com.example.data.model.CpaCalculationResult? {
        val distNM = calculateDistanceNM(userLat, userLon, targetLat, targetLon)
        val bearingDeg = calculateBearingDeg(userLat, userLon, targetLat, targetLon)

        // Target relative Cartesian coordinates in NM (X: East, Y: North)
        val radBearing = Math.toRadians(bearingDeg.toDouble())
        val rx = distNM * sin(radBearing)
        val ry = distNM * cos(radBearing)

        // User velocity vector in knots (NM/h)
        val radUserHdg = Math.toRadians(userHeadingDeg.toDouble())
        val uvx = userSpeedKnots * sin(radUserHdg)
        val uvy = userSpeedKnots * cos(radUserHdg)

        // Target velocity vector in knots (NM/h)
        val radTgtHdg = Math.toRadians(targetHeadingDeg.toDouble())
        val tvx = targetSpeedKnots * sin(radTgtHdg)
        val tvy = targetSpeedKnots * cos(radTgtHdg)

        // Relative velocity vector (Target rel to User)
        val relVx = tvx - uvx
        val relVy = tvy - uvy
        val relSpeedSq = relVx * relVx + relVy * relVy

        val tcpaHours: Double
        val cpaNM: Double

        if (relSpeedSq < 0.001) {
            // Ships moving at identical velocity and heading
            tcpaHours = 0.0
            cpaNM = distNM
        } else {
            // tcpa = - (R . Vrel) / |Vrel|^2
            val dotProduct = rx * relVx + ry * relVy
            val rawTcpa = -dotProduct / relSpeedSq

            if (rawTcpa < 0) {
                // Vessels are diverging (opening)
                tcpaHours = 0.0
                cpaNM = distNM
            } else {
                tcpaHours = rawTcpa
                val cpaX = rx + relVx * tcpaHours
                val cpaY = ry + relVy * tcpaHours
                cpaNM = sqrt(cpaX * cpaX + cpaY * cpaY)
            }
        }

        val tcpaMinutes = tcpaHours * 60.0

        // Collision point coordinates estimated
        val colLat = userLat + (uvy * tcpaHours / 60.0)
        val colLon = userLon + (uvx * tcpaHours / (60.0 * cos(Math.toRadians(userLat))))

        val isDangerous = cpaNM < 0.6 && tcpaMinutes in 0.1..15.0 && (userSpeedKnots > 0.8 || targetSpeedKnots > 0.8)

        val dummyVessel = com.example.data.model.Vessel(
            id = "target",
            name = "AIS Target",
            callSign = "N/A",
            mmsi = "000000000",
            type = com.example.data.model.VesselType.CARGO,
            latitude = targetLat,
            longitude = targetLon,
            speedKnots = targetSpeedKnots,
            headingDeg = targetHeadingDeg,
            destination = "",
            status = "Underway",
            lengthMeters = 80,
            draughtMeters = 4.5,
            grossTonnage = 1200,
            lastUpdated = "Live",
            originPort = "",
            eta = ""
        )

        return com.example.data.model.CpaCalculationResult(
            vessel = dummyVessel,
            cpaDistanceNM = cpaNM,
            tcpaMinutes = tcpaMinutes,
            isDangerous = isDangerous,
            collisionPointLat = colLat,
            collisionPointLon = colLon
        )
    }

    /**
     * Estimates real-time Tidal Height (m) and Tidal Stream Current for local Indonesian Waters
     */
    fun predictTideAndCurrent(lat: Double, lon: Double, hourOfDay: Double = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) + java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) / 60.0): com.example.data.model.TideCurrentPrediction {
        val (zoneName, _) = inferIndonesianMaritimeZone(lat, lon)
        // Semi-diurnal M2 tide wave simulation (period ~12.42 hours)
        val tidePhase = (hourOfDay % 12.42) / 12.42 * 2.0 * PI
        val tidalRange = if (lat in -6.5..-5.5 && lon in 107.0..109.0) 1.6f else 2.2f // North Java vs Sunda/Bali
        val tideHeight = (sin(tidePhase) * (tidalRange / 2.0) + (tidalRange / 2.0)).toFloat()

        val tideState = when {
            sin(tidePhase) > 0.85 -> "PUNCAK PASANG TINGGI (HW)"
            sin(tidePhase) < -0.85 -> "SURUT TERENDAH (LW)"
            cos(tidePhase) > 0 -> "PASANG NAIK (FLOOD)"
            else -> "SURUT MENUJU RENDAH (EBB)"
        }

        val currentSpeed = (abs(cos(tidePhase)) * 1.8f).toFloat() // current strongest during mid flood/ebb
        val currentDir = if (cos(tidePhase) > 0) 110f else 290f // Eastward on flood, Westward on ebb in Java Sea

        val nextHw = String.format(java.util.Locale.US, "%02d:00 WIB", ((hourOfDay + (if (cos(tidePhase) > 0) (6.0 - (hourOfDay % 6.0)) else (12.0 - (hourOfDay % 6.0)))).toInt() % 24))
        val nextLw = String.format(java.util.Locale.US, "%02d:30 WIB", ((hourOfDay + (if (cos(tidePhase) <= 0) (6.0 - (hourOfDay % 6.0)) else (12.0 - (hourOfDay % 6.0)))).toInt() % 24))

        return com.example.data.model.TideCurrentPrediction(
            portName = zoneName.substringBefore(" &"),
            currentTideState = tideState,
            tideHeightMeters = tideHeight,
            currentSpeedKnots = currentSpeed,
            currentDirectionDeg = currentDir,
            nextHighTideTime = nextHw,
            nextLowTideTime = nextLw
        )
    }
}

object DeviceGpsHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    fun requestRealDeviceLocation(
        context: Context,
        onLocationFound: (lat: Double, lon: Double, speedKnots: Double, headingDeg: Float, zoneName: String, wppZone: String) -> Unit
    ) {
        if (!hasLocationPermission(context)) return

        try {
            val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            fusedClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val (zoneName, wpp) = MaritimeMath.inferIndonesianMaritimeZone(location.latitude, location.longitude)
                    val speedKnots = (location.speed * 1.94384).toDouble() // m/s to knots
                    val heading = if (location.hasBearing()) location.bearing else 0f
                    onLocationFound(location.latitude, location.longitude, speedKnots, heading, zoneName, wpp)
                } else {
                    // Fallback to last known location
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            val (zoneName, wpp) = MaritimeMath.inferIndonesianMaritimeZone(lastLoc.latitude, lastLoc.longitude)
                            val speedKnots = (lastLoc.speed * 1.94384).toDouble()
                            val heading = if (lastLoc.hasBearing()) lastLoc.bearing else 0f
                            onLocationFound(lastLoc.latitude, lastLoc.longitude, speedKnots, heading, zoneName, wpp)
                        }
                    }
                }
            }
        } catch (_: SecurityException) {}
    }
}

object MaritimeAlertHelper {

    fun triggerEmergencyAudioAlarm(context: Context) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(longArrayOf(0, 300, 200, 300, 200, 600), -1)
            }
        } catch (_: Exception) {}
    }

    fun triggerCollisionCpaAlarm(context: Context) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 800)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
        } catch (_: Exception) {}
    }

    fun triggerAnchorDragAlarm(context: Context) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1200)
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
        } catch (_: Exception) {}
    }

    fun playWarningBeep(context: Context) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (_: Exception) {}
    }
}

