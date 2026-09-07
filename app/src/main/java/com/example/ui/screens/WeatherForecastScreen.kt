package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayForecast
import com.example.data.model.MaritimeWeatherArea
import com.example.data.model.MelautStatus
import com.example.data.model.SeaWaveCategory
import com.example.data.util.MaritimeMath
import com.example.ui.components.WaveStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MaritimeUiState
import com.example.ui.viewmodel.MaritimeViewModel
import java.util.Locale

@Composable
fun WeatherForecastScreen(
    viewModel: MaritimeViewModel,
    uiState: MaritimeUiState
) {
    val context = LocalContext.current
    val currentArea = uiState.selectedWeatherArea ?: uiState.weatherAreas.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_forecast_screen")
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Vessel GPS Zone Header Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OceanCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = OceanCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text("POSISI KAPAL AKTIF:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = OceanCyan, letterSpacing = 1.sp)
                            Text(uiState.currentSeaLocationName, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(
                                text = "GPS: ${MaritimeMath.formatMaritimeCoord(uiState.userVessel.latitude, uiState.userVessel.longitude)}",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentArea != null) {
                        val closestArea = uiState.weatherAreas.minByOrNull { area ->
                            MaritimeMath.calculateDistanceNM(uiState.userVessel.latitude, uiState.userVessel.longitude, area.latitude, area.longitude)
                        }
                        if (closestArea != null && closestArea.id != currentArea.id) {
                            Button(
                                onClick = { viewModel.selectWeatherArea(closestArea) },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanCyan, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Pilih Terdekat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Area Selector Bar (Horizontal Scroll of Indonesian Marine Zones)
        item {
            Text(
                text = "PILIH WILAYAH PERAIRAN BMKG (${uiState.weatherAreas.size} ZONA)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.weatherAreas) { area ->
                    val isSelected = currentArea?.id == area.id
                    val distFromShip = MaritimeMath.calculateDistanceNM(
                        uiState.userVessel.latitude, uiState.userVessel.longitude,
                        area.latitude, area.longitude
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectWeatherArea(area) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, OceanCyan) else null,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = if (isSelected) 6.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(area.statusMelaut.badgeColor)
                            )
                            Column {
                                Text(
                                    text = area.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${area.wppnriZone} • ${String.format(Locale.US, "%.0f", distFromShip)} NM • Gel. ${area.waveHeight}m",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (currentArea != null) {
            val distNM = MaritimeMath.calculateDistanceNM(
                uiState.userVessel.latitude, uiState.userVessel.longitude,
                currentArea.latitude, currentArea.longitude
            )

            // BMKG Warning Banner (if exists)
            currentArea.bmkgWarning?.let { warning ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Peringatan", tint = DangerRed)
                                    Text(
                                        text = "PERINGATAN DINI GELOMBANG BMKG",
                                        fontWeight = FontWeight.Black,
                                        color = DangerRed,
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.triggerBmkgAlarm(context) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Bunyikan Sirine", tint = DangerRed)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Main Wave & Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentArea.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentArea.wppnriZone} • Jarak: ${String.format(Locale.US, "%.1f", distNM)} NM • ${MaritimeMath.formatMaritimeCoord(currentArea.latitude, currentArea.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            WaveStatusBadge(currentArea.waveCategory, currentArea.statusMelaut)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Wave Height Visual Meter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TINGGI GELOMBANG SIGNIFIKAN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${currentArea.waveHeight}",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = currentArea.waveCategory.color
                                    )
                                    Text(
                                        text = " meter",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Maksimum: hingga ${currentArea.maxWaveHeight} meter",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Moon Phase & Lunar Fishing Index
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.NightsStay, contentDescription = "Fase Bulan", tint = WarningAmber)
                                    Text(currentArea.moonPhase, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Potensi Tangkapan:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(currentArea.lunarFishingRating, fontSize = 10.sp, fontWeight = FontWeight.Black, color = SeafoamGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Safety Advice Note
                        Surface(
                            color = currentArea.statusMelaut.badgeColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = currentArea.statusMelaut.badgeColor, modifier = Modifier.size(18.dp))
                                Text(
                                    text = currentArea.waveCategory.advice,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Marine Conditions Matrix: Wind, Tides, SST, Visibility, Current
            item {
                Text(
                    text = "PARAMETER KEMARITIMAN LAUT HARI INI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarineParamCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Air,
                        iconTint = OceanTeal,
                        title = "Angin Laut",
                        value = "${currentArea.windSpeedKnots} Knot",
                        subtitle = "Arah: ${currentArea.windDirection} (Skala ${currentArea.windBeaufort} Bft)"
                    )
                    MarineParamCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        iconTint = OceanBlue,
                        title = "Arus Permukaan",
                        value = "${currentArea.currentSpeedKnots} Knot",
                        subtitle = "Menuju: ${currentArea.currentDirection}"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarineParamCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Waves,
                        iconTint = OceanCyan,
                        title = "Pasang Tertinggi",
                        value = "${currentArea.highTideHeightM} m",
                        subtitle = "Waktu: ${currentArea.highTideTime}"
                    )
                    MarineParamCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.South,
                        iconTint = WarningAmber,
                        title = "Surut Terendah",
                        value = "${currentArea.lowTideHeightM} m",
                        subtitle = "Waktu: ${currentArea.lowTideTime}"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarineParamCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Thermostat,
                        iconTint = WarningOrange,
                        title = "Suhu Permukaan Laut (SST)",
                        value = "${currentArea.seaSurfaceTempC}°C",
                        subtitle = "Sangat Ideal untuk Ikan Pelagis"
                    )
                    MarineParamCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Visibility,
                        iconTint = SeafoamGreen,
                        title = "Jarak Pandang (Visibility)",
                        value = "${currentArea.visibilityKm} KM",
                        subtitle = "Hujan: ${currentArea.rainChance}%"
                    )
                }
            }

            // 7-Day Maritime Forecast
            item {
                Text(
                    text = "PRAKIRAAN CUACA MARITIM 7 HARI KE DEPAN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentArea.forecast7Days.forEach { day ->
                            DayForecastRow(day)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarineParamCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayForecastRow(day: DayForecast) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.width(90.dp)) {
            Text(day.dayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(day.dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = if (day.rainChance > 50) Icons.Default.Thunderstorm else if (day.rainChance > 25) Icons.Default.CloudQueue else Icons.Default.WbSunny,
                contentDescription = null,
                tint = if (day.rainChance > 50) DangerRed else if (day.rainChance > 25) OceanCyan else WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Text(day.condition, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Waves, contentDescription = null, tint = if (day.waveHeight > 2.5f) DangerRed else OceanBlue, modifier = Modifier.size(14.dp))
                Text(
                    text = "${day.waveHeight} - ${day.maxWaveHeight} m",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (day.waveHeight > 2.5f) DangerRed else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${day.windSpeedKnots} Knot ${day.windDirection}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
