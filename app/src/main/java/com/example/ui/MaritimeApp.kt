package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MaritimeBottomNavBar
import com.example.ui.components.MaritimeTopBar
import com.example.ui.screens.*
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MaritimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaritimeApp(viewModel: MaritimeViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val waypoints by viewModel.waypoints.collectAsStateWithLifecycle()
    val catchLogs by viewModel.catchLogs.collectAsStateWithLifecycle()
    val maintenances by viewModel.maintenances.collectAsStateWithLifecycle()
    val sosRecords by viewModel.sosRecords.collectAsStateWithLifecycle()
    val totalCatchWeight by viewModel.totalCatchWeight.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()

    var showEmergencyModal by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    MyApplicationTheme(
        highContrastSunMode = uiState.highContrastSunMode
    ) {
        Scaffold(
            topBar = {
                // In landscape mode when viewing navigation map (Tab 0), hide the outer top bar to give 100% height to map
                if (!isLandscape || uiState.selectedTab != 0) {
                    MaritimeTopBar(
                        userLat = uiState.userVessel.latitude,
                        userLon = uiState.userVessel.longitude,
                        locationName = uiState.currentSeaLocationName,
                        isHighContrast = uiState.highContrastSunMode,
                        isSatelliteOnline = uiState.isSatelliteConnected,
                        onToggleContrast = { viewModel.toggleHighContrastSunMode() },
                        onTriggerSos = {
                            viewModel.selectTab(5) // Navigate to emergency screen
                        }
                    )
                }
            },
            bottomBar = {
                MaritimeBottomNavBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    isCompact = isLandscape && uiState.selectedTab == 0
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main Tab Screen Routing
                when (uiState.selectedTab) {
                    0 -> MapVesselScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        waypoints = waypoints
                    )
                    1 -> WeatherForecastScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    2 -> WaypointsScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        waypoints = waypoints
                    )
                    3 -> WaveAnalyticsScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    4 -> CatchLogScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        catchLogs = catchLogs,
                        totalWeight = totalCatchWeight,
                        totalRevenue = totalRevenue
                    )
                    5 -> EmergencyAndMaintenanceScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        maintenances = maintenances,
                        sosRecords = sosRecords
                    )
                }

                // Active Emergency SOS Persistent Alert Banner
                AnimatedVisibility(
                    visible = uiState.isEmergencyActive,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                    Text("SIARAN DARURAT SOS AKTIF!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                                }
                                IconButton(onClick = { viewModel.dismissEmergency() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                                }
                            }
                            Text(
                                text = uiState.lastSosMessage ?: "Sinyal marabahaya telah dipancarkan via satelit ke BASARNAS 115 dan kapal di sekitar.",
                                fontSize = 11.sp,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:115"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DangerRed)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hubungi BASARNAS 115", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // BMKG Siren Audio Alert Dialog
                if (uiState.bmkgAlarmTriggered) {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissBmkgAlarm() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = DangerRed)
                                Text("SIRINE PERINGATAN MARITIM", fontWeight = FontWeight.Black, color = DangerRed)
                            }
                        },
                        text = {
                            Text(
                                "Sirine peringatan cuaca buruk dan kanal darurat VHF 16 telah diaktifkan untuk keselamatan navigasi kapal.",
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.dismissBmkgAlarm() }) {
                                Text("Matikan Sirine")
                            }
                        }
                    )
                }
            }
        }
    }
}
