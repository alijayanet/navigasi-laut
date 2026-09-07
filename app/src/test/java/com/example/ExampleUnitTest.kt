package com.example

import com.example.data.util.MaritimeMath
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDistanceCalculation() {
        // Jakarta Bay to Seribu Islands approx test
        val lat1 = -6.10
        val lon1 = 106.85
        val lat2 = -5.90
        val lon2 = 106.85

        val distanceKm = MaritimeMath.calculateDistanceKm(lat1, lon1, lat2, lon2)
        val distanceNM = MaritimeMath.calculateDistanceNM(lat1, lon1, lat2, lon2)

        assertTrue(distanceKm > 20.0 && distanceKm < 24.0)
        assertTrue(distanceNM > 10.0 && distanceNM < 14.0)
    }

    @Test
    fun testBearingCalculation() {
        val lat1 = -6.00
        val lon1 = 106.00
        val lat2 = -5.00 // Heading North
        val lon2 = 106.00

        val bearing = MaritimeMath.calculateBearingDeg(lat1, lon1, lat2, lon2)
        assertEquals(0f, bearing, 1.0f)
        assertTrue(MaritimeMath.getCardinalDirection(bearing).contains("Utara"))
    }

    @Test
    fun testMaritimeCoordinateFormatting() {
        val formatted = MaritimeMath.formatMaritimeCoord(-5.9320, 106.8380)
        assertTrue(formatted.contains("S") && formatted.contains("E"))
        assertTrue(formatted.contains("05°") && formatted.contains("106°"))
    }

    @Test
    fun testDriftEstimation() {
        val startLat = -6.0
        val startLon = 106.0
        val currentSpeed = 2.0 // knots
        val direction = 45f // Northeast
        val hours = 3.0 // 3 hours

        val (driftLat, driftLon) = MaritimeMath.estimateDriftCoordinates(
            startLat, startLon, currentSpeed, direction, hours
        )

        assertTrue(driftLat > startLat)
        assertTrue(driftLon > startLon)
    }

    @Test
    fun testCatchLogCsvExport() {
        val sampleLogs = listOf(
            com.example.data.local.FishCatchLogEntity(
                id = 1,
                dateStr = "25/08/2026",
                fishSpecies = "Tongkol",
                weightKg = 150.0,
                pricePerKg = 25000.0,
                estimatedRevenue = 3750000.0,
                fishingGear = "Jaring Insang",
                wppnriZone = "WPP 712",
                latitude = -5.92,
                longitude = 106.84,
                notes = "Hasil fajar"
            )
        )

        val csv = com.example.data.util.CatchLogExportHelper.generateCatchLogCsv(sampleLogs, "KM Bahari Sentosa")
        assertTrue(csv.contains("Tongkol"))
        assertTrue(csv.contains("150.00"))
        assertTrue(csv.contains("KM Bahari Sentosa"))

        val summary = com.example.data.util.CatchLogExportHelper.generateCatchLogSummaryText(sampleLogs, "KM Bahari Sentosa", 150.0, 3750000.0)
        assertTrue(summary.contains("KM Bahari Sentosa"))
        assertTrue(summary.contains("Tongkol"))
    }

    @Test
    fun testNmeaMockStream() {
        val stream = com.example.data.util.Nmea0183Helper.generateMockNmeaStream(
            lat = -5.9320,
            lon = 106.8380,
            speedKnots = 8.5,
            headingDeg = 142.0f
        )

        assertEquals(3, stream.size)
        assertTrue(stream[0].startsWith("\$GPRMC"))
        assertTrue(stream[1].startsWith("\$GPGGA"))
        assertTrue(stream[2].startsWith("!AIVDM"))
    }

    @Test
    fun testZppiZonesCalculation() {
        val zone = com.example.data.model.ZppiFishingZone(
            id = "ZPPI-01",
            name = "Zona Pelagis P. Pari",
            wppZone = "WPP 712",
            targetSpecies = "Tongkol",
            latitude = -5.86,
            longitude = 106.72,
            sstCelsius = 28.6f,
            chlorophyllA = 0.92f,
            potentialScore = 94,
            peakHours = "04:30 - 08:30 WIB",
            recommendedDepthM = "15 - 35m",
            gearType = "Gillnet"
        )

        assertTrue(zone.potentialScore in 1..100)
        assertTrue(zone.sstCelsius in 25.0f..32.0f)
        assertTrue(zone.chlorophyllA > 0.5f)
    }

    @Test
    fun testColregsRules() {
        val rule = com.example.data.model.ColregsRule(
            id = "COL-14",
            ruleNumber = "Aturan 14",
            title = "Head-on",
            situation = "Berhadapan",
            actionRequired = "KEDUA KAPAL MERUBAH HALUAN KE KANAN",
            nightLights = "Merah & Hijau",
            soundSignal = "1 Tiupan Pendek"
        )

        assertTrue(rule.actionRequired.contains("KANAN"))
    }

    @Test
    fun testInferIndonesianMaritimeZone() {
        val (jakartaName, jakartaWpp) = MaritimeMath.inferIndonesianMaritimeZone(-5.9320, 106.8380)
        assertEquals("Teluk Jakarta & Kepulauan Seribu", jakartaName)
        assertEquals("WPPNRI 712", jakartaWpp)

        val (natunaName, natunaWpp) = MaritimeMath.inferIndonesianMaritimeZone(3.9000, 108.3000)
        assertTrue(natunaName.contains("Natuna"))
        assertEquals("WPPNRI 711", natunaWpp)

        val (sundaName, sundaWpp) = MaritimeMath.inferIndonesianMaritimeZone(-5.8800, 105.8500)
        assertTrue(sundaName.contains("Sunda"))
        assertEquals("WPPNRI 572", sundaWpp)
    }
}

