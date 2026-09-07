package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HighRiskZonePrediction
import com.example.data.model.PortFacility
import com.example.ui.theme.*
import com.example.ui.viewmodel.MaritimeUiState
import com.example.ui.viewmodel.MaritimeViewModel

@Composable
fun WaveAnalyticsScreen(
    viewModel: MaritimeViewModel,
    uiState: MaritimeUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wave_analytics_screen")
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Analytics Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = OceanCyan, modifier = Modifier.size(24.dp))
                            Text("DASHBOARD ANALITIK GELOMBANG TINGGI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SeafoamGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Model AI Prediktif BMKG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SeafoamGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Analisis potensi gelombang tinggi mingguan di perairan rawan Indonesia. Sistem menghitung indeks risiko historis, pergerakan angin muson, dan peringatan dini untuk keselamatan kapal nelayan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RiskStatBadge("Zona Siaga Merah", "1 Wilayah", DangerRed)
                        RiskStatBadge("Zona Waspada", "3 Wilayah", WarningAmber)
                        RiskStatBadge("Zona Kondusif", "12 Wilayah", SuccessGreen)
                    }
                }
            }
        }

        // List of High Risk Maritime Zones
        item {
            Text(
                text = "PANTAUAN WILAYAH RAWAN GELOMBANG EKSTREM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        items(uiState.highRiskZones, key = { it.id }) { zone ->
            HighRiskZoneCard(zone)
        }

        // Port Logistics & Safe Shelter Facilities
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PELABUHAN PERIKANAN & TEMPAT BERLINDUNG (SHELTER)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Daftar pelabuhan pendaratan ikan dengan fasilitas logistik BBM solar, es balok, air tawar & radio pengawas.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(uiState.ports, key = { it.id }) { port ->
            PortFacilityCard(port)
        }
    }
}

@Composable
private fun RiskStatBadge(title: String, count: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, fontWeight = FontWeight.Black, fontSize = 14.sp, color = color)
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun HighRiskZoneCard(zone: HighRiskZonePrediction) {
    val isRed = zone.currentWaveM >= 3.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isRed) androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed) else null,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(zone.regionName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Wilayah: ${zone.wppZone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }

                Surface(
                    color = if (isRed) DangerRed.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = zone.riskLevel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isRed) DangerRed else WarningOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wave progress visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tinggi Gelombang Saat Ini", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${zone.currentWaveM} m", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isRed) DangerRed else WarningAmber)
                }
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = DangerRed)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Prediksi Puncak Maksimal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("hingga ${zone.predictedMaxWaveM} m", fontWeight = FontWeight.Black, fontSize = 18.sp, color = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Risk Factors & Affected Vessels
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Faktor Pemicu: ${zone.riskFactor}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Armada Terdampak: ${zone.affectedVesselSizes}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    Text("Skor Kerentanan Historis: ${zone.historicalRiskScore}/100", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PortFacilityCard(port: PortFacility) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Anchor, contentDescription = null, tint = OceanCyan, modifier = Modifier.size(18.dp))
                    Text(port.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(port.city, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Radio Pengawas: ${port.vhfChannel}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("Telp: ${port.phoneOperator}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Facilities tags
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FacilityBadge("BBM Solar", port.fuelAvailable)
                FacilityBadge("Air Bersih", port.freshWaterAvailable)
                FacilityBadge("Pabrik Es", port.iceFactoryAvailable)
            }
        }
    }
}

@Composable
private fun FacilityBadge(label: String, available: Boolean) {
    Surface(
        color = if (available) SeafoamGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (available) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (available) SeafoamGreen else Color.Gray,
                modifier = Modifier.size(12.dp)
            )
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (available) SeafoamGreen else Color.Gray)
        }
    }
}
