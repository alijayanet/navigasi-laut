package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MelautStatus
import com.example.data.model.SeaWaveCategory
import com.example.data.util.MaritimeMath
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaritimeTopBar(
    userLat: Double,
    userLon: Double,
    locationName: String,
    isHighContrast: Boolean,
    isSatelliteOnline: Boolean,
    onToggleContrast: () -> Unit,
    onTriggerSos: () -> Unit
) {
    Surface(
        color = if (isHighContrast) MaterialTheme.colorScheme.surface else ElegantDarkHeader,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHighContrast) Color.Black else ElegantDarkBorder),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Title & Maritime Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isHighContrast) MaterialTheme.colorScheme.primaryContainer else ElegantDarkCard)
                            .border(1.dp, if (isHighContrast) Color.Black else ElegantDarkBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBoat,
                            contentDescription = "App Logo",
                            tint = OceanCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "MARITIMA INDONESIA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanCyan,
                            letterSpacing = 1.8.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Nusantara Bahari",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isHighContrast) Color.Black else Color.White
                            )
                            // ID Flag Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElegantDarkCard)
                                    .border(1.dp, ElegantDarkBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("🇮🇩 BMKG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                            }
                        }
                    }
                }

                // Quick Status & Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sunlight Contrast Toggle
                    IconButton(
                        onClick = onToggleContrast,
                        modifier = Modifier
                            .testTag("btn_sun_contrast_toggle")
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isHighContrast) WarningAmber else ElegantDarkCard)
                            .border(1.dp, if (isHighContrast) Color.Black else ElegantDarkBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isHighContrast) Icons.Default.WbSunny else Icons.Outlined.LightMode,
                            contentDescription = "Mode Terik Matahari",
                            tint = if (isHighContrast) Color.Black else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Red SOS Emergency Quick Button
                    Button(
                        onClick = onTriggerSos,
                        modifier = Modifier
                            .testTag("btn_top_bar_sos")
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SOS",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-bar: GPS Live Coordinates & Satellite Link Status (Elegant Dark Styled)
            Surface(
                color = if (isHighContrast) MaterialTheme.colorScheme.surfaceVariant else ElegantDarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isHighContrast) Color.Black else ElegantDarkBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "GPS",
                            tint = OceanCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = locationName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighContrast) Color.Black else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = MaritimeMath.formatMaritimeCoord(userLat, userLon),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isHighContrast) Color.Black else OceanCyan
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSatelliteOnline) SeafoamGreen else DangerRed)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isSatelliteOnline) "GPS 3D FIX" else "SEARCHING",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isSatelliteOnline) SeafoamGreen else DangerRed
                            )
                            Text(
                                text = if (isSatelliteOnline) "±2.5m • 9 SAT" else "SAT-OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isHighContrast) Color.Black else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MaritimeBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isCompact: Boolean = false
) {
    val navItems = listOf(
        NavTabItem("Peta & Kapal", Icons.Filled.Map, Icons.Outlined.Map, "nav_tab_map"),
        NavTabItem("Cuaca BMKG", Icons.Filled.Cloud, Icons.Outlined.Cloud, "nav_tab_weather"),
        NavTabItem("Spot & Jaring", Icons.Filled.Place, Icons.Outlined.Place, "nav_tab_waypoints"),
        NavTabItem("Analitik Rawan", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_tab_analytics"),
        NavTabItem("Tangkapan", Icons.Filled.PhonelinkRing, Icons.Outlined.PhonelinkRing, "nav_tab_catch"),
        NavTabItem("Darurat & Servis", Icons.Filled.Build, Icons.Outlined.Build, "nav_tab_safety")
    )

    Surface(
        color = ElegantDarkHeader,
        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkBorder),
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            modifier = Modifier
                .then(if (isCompact) Modifier.height(48.dp) else Modifier.navigationBarsPadding())
                .testTag("maritime_bottom_nav"),
            containerColor = ElegantDarkHeader,
            tonalElevation = 0.dp
        ) {
            navItems.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                NavigationBarItem(
                    modifier = Modifier.testTag(item.testTag),
                    selected = isSelected,
                    alwaysShowLabel = !isCompact,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = if (isSelected) OceanCyan else Color(0xFF64748B),
                            modifier = if (isCompact) Modifier.size(20.dp) else Modifier.size(24.dp)
                        )
                    },
                    label = if (!isCompact) {
                        {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OceanCyan else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else null,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = OceanCyan.copy(alpha = 0.15f),
                        selectedIconColor = OceanCyan,
                        unselectedIconColor = Color(0xFF64748B),
                        selectedTextColor = OceanCyan,
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    }
}

@Composable
fun WaveStatusBadge(
    category: SeaWaveCategory,
    statusMelaut: MelautStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = statusMelaut.badgeColor.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusMelaut.badgeColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(statusMelaut.badgeColor)
            )
            Column {
                Text(
                    text = statusMelaut.label,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = statusMelaut.badgeColor
                )
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun CompassRoseWidget(
    headingDeg: Float,
    speedKnots: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ElegantDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(ElegantDarkBackground)
                    .border(1.5.dp, OceanCyan.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Radar sweep circle decoration
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.dp, OceanCyan.copy(alpha = 0.25f), CircleShape)
                )

                // Cardinal markers
                Text("U", modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = DangerRed)
                Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Text("B", modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Text("T", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))

                // Rotating Needle
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Compass Needle",
                    tint = DangerRed,
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(headingDeg)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${headingDeg.toInt()}° ${MaritimeMath.getCardinalDirection(headingDeg)}",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "Kecepatan: $speedKnots Knot",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OceanCyan
            )
        }
    }
}
