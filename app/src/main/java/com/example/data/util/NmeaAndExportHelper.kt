package com.example.data.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.FishCatchLogEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object CatchLogExportHelper {

    fun generateCatchLogCsv(catchLogs: List<FishCatchLogEntity>, vesselName: String): String {
        val sb = StringBuilder()
        sb.append("No,Tanggal,Nama Kapal,Spesies Ikan,Berat (Kg),Harga Per Kg (Rp),Total Pendapatan (Rp),Alat Tangkap,Wilayah WPPNRI,Lintang (Lat),Bujur (Lon),Catatan\n")
        
        catchLogs.forEachIndexed { index, log ->
            val row = listOf(
                (index + 1).toString(),
                "\"${log.dateStr}\"",
                "\"$vesselName\"",
                "\"${log.fishSpecies}\"",
                String.format(Locale.US, "%.2f", log.weightKg),
                String.format(Locale.US, "%.0f", log.pricePerKg),
                String.format(Locale.US, "%.0f", log.estimatedRevenue),
                "\"${log.fishingGear}\"",
                "\"${log.wppnriZone}\"",
                String.format(Locale.US, "%.6f", log.latitude),
                String.format(Locale.US, "%.6f", log.longitude),
                "\"${log.notes.replace("\"", "\"\"")}\""
            ).joinToString(",")
            sb.append(row).append("\n")
        }
        return sb.toString()
    }

    fun generateCatchLogSummaryText(
        catchLogs: List<FishCatchLogEntity>,
        vesselName: String,
        totalWeight: Double,
        totalRevenue: Double
    ): String {
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val dateNow = SimpleDateFormat("dd MMMM yyyy HH:mm 'WIB'", Locale("id", "ID")).format(Date())

        val sb = StringBuilder()
        sb.append("📋 *REKAPITULASI E-LOGBOOK PENANGKAPAN IKAN TERUKUR*\n")
        sb.append("🌊 *Aplikasi Nusantara Bahari - KKP RI*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🚢 *Nama Kapal:* $vesselName\n")
        sb.append("📅 *Waktu Ekspor:* $dateNow\n")
        sb.append("⚖️ *Total Tangkapan:* ${String.format(Locale.US, "%.1f", totalWeight)} Kg\n")
        sb.append("💰 *Estimasi Nilai:* ${rupiahFormat.format(totalRevenue).replace(",00", "")}\n")
        sb.append("🔢 *Jumlah Catatan:* ${catchLogs.size} Trip\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
        sb.append("📦 *RINCIAN SPESIES TANGKAPAN:*\n")

        val grouped = catchLogs.groupBy { it.fishSpecies }
        grouped.forEach { (species, list) ->
            val speciesWeight = list.sumOf { it.weightKg }
            val speciesRevenue = list.sumOf { it.estimatedRevenue }
            sb.append("• *$species:* ${String.format(Locale.US, "%.1f", speciesWeight)} Kg (${rupiahFormat.format(speciesRevenue).replace(",00", "")})\n")
        }

        sb.append("\n📍 *DATA KOORDINAT PENANGKAPAN:*\n")
        catchLogs.take(5).forEachIndexed { i, log ->
            val coord = MaritimeMath.formatMaritimeCoord(log.latitude, log.longitude)
            sb.append("${i + 1}. ${log.fishSpecies} (${log.weightKg}kg) @ $coord [${log.wppnriZone}]\n")
        }
        if (catchLogs.size > 5) {
            sb.append("...dan ${catchLogs.size - 5} titik penangkapan lainnya tersimpan offline.\n")
        }

        sb.append("\n_Data disinkronkan otomatis sesuai kepatuhan kuota PIT WPPNRI._")
        return sb.toString()
    }

    fun shareCatchReport(
        context: Context,
        catchLogs: List<FishCatchLogEntity>,
        vesselName: String,
        totalWeight: Double,
        totalRevenue: Double
    ) {
        if (catchLogs.isEmpty()) {
            Toast.makeText(context, "Belum ada catatan tangkapan untuk dibagikan", Toast.LENGTH_SHORT).show()
            return
        }

        val textSummary = generateCatchLogSummaryText(catchLogs, vesselName, totalWeight, totalRevenue)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textSummary)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan E-Logbook Tangkapan - $vesselName")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Laporan E-Logbook ke:")
        context.startActivity(shareIntent)
    }
}

/**
 * NMEA 0183 Maritime Telemetry Receiver & Parser Helper
 */
object Nmea0183Helper {

    /**
     * Generates simulated live NMEA 0183 sentences for external GPS/AIS transponders
     */
    fun generateMockNmeaStream(lat: Double, lon: Double, speedKnots: Double, headingDeg: Float): List<String> {
        val timeStr = SimpleDateFormat("HHmmss.ss", Locale.US).format(Date())
        val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date())

        val absLat = Math.abs(lat)
        val latDeg = absLat.toInt()
        val latMin = (absLat - latDeg) * 60.0
        val latNmea = String.format(Locale.US, "%02d%07.4f,%s", latDeg, latMin, if (lat >= 0) "N" else "S")

        val absLon = Math.abs(lon)
        val lonDeg = absLon.toInt()
        val lonMin = (absLon - lonDeg) * 60.0
        val lonNmea = String.format(Locale.US, "%03d%07.4f,%s", lonDeg, lonMin, if (lon >= 0) "E" else "W")

        // $GPRMC
        val rmcContent = "GPRMC,$timeStr,A,$latNmea,$lonNmea,${String.format(Locale.US, "%.1f", speedKnots)},${String.format(Locale.US, "%.1f", headingDeg)},$dateStr,,,A"
        val rmcChecksum = calculateChecksum(rmcContent)
        val rmc = "$$rmcContent*$rmcChecksum"

        // $GPGGA
        val ggaContent = "GPGGA,$timeStr,$latNmea,$lonNmea,1,09,1.1,12.4,M,0.0,M,,"
        val ggaChecksum = calculateChecksum(ggaContent)
        val gga = "$$ggaContent*$ggaChecksum"

        // $AIVDM (AIS Transponder sentence example)
        val aivdm = "!AIVDM,1,1,,B,15N43P001rO7b9bM>W6r>wTP0000,0*1E"

        return listOf(rmc, gga, aivdm)
    }

    private fun calculateChecksum(sentence: String): String {
        var checksum = 0
        for (char in sentence) {
            checksum = checksum xor char.code
        }
        return String.format(Locale.US, "%02X", checksum)
    }
}
