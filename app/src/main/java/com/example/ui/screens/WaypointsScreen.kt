package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FishNetWaypointEntity
import com.example.data.util.MaritimeMath
import com.example.ui.theme.*
import com.example.ui.viewmodel.MaritimeUiState
import com.example.ui.viewmodel.MaritimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointsScreen(
    viewModel: MaritimeViewModel,
    uiState: MaritimeUiState,
    waypoints: List<FishNetWaypointEntity>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(waypoints, filterType) {
        if (filterType == null) waypoints
        else waypoints.filter { it.type == filterType }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_waypoint"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = "Tandai Titik")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tandai Titik Jaring / Spot", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("waypoints_screen")
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Navigation HUD if a Waypoint is Selected as Target
            uiState.selectedNavWaypoint?.let { navWp ->
                item {
                    val distNM = MaritimeMath.calculateDistanceNM(
                        uiState.userVessel.latitude, uiState.userVessel.longitude,
                        navWp.latitude, navWp.longitude
                    )
                    val distKm = distNM / 0.539957
                    val bearing = MaritimeMath.calculateBearingDeg(
                        uiState.userVessel.latitude, uiState.userVessel.longitude,
                        navWp.latitude, navWp.longitude
                    )
                    val etaMinutes = if (uiState.userVessel.currentSpeedKnots > 0) {
                        ((distNM / uiState.userVessel.currentSpeedKnots) * 60).toInt()
                    } else 0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = OceanSurface),
                        border = androidx.compose.foundation.BorderStroke(2.dp, OceanCyan),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = DangerRed, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text("NAVIGASI MENUJU TARGET", fontWeight = FontWeight.Black, fontSize = 11.sp, color = OceanCyan, letterSpacing = 1.sp)
                                        Text(navWp.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    }
                                }
                                IconButton(onClick = { viewModel.setNavWaypoint(null) }) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Batal Target", tint = Color.White)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = OceanCyan.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("JARAK REAL-TIME", fontSize = 10.sp, color = OceanTeal)
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.2f NM", distNM),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "≈ ${String.format(java.util.Locale.US, "%.1f", distKm)} km (ETA: ~$etaMinutes mnt)",
                                        fontSize = 11.sp,
                                        color = OceanLight
                                    )
                                }

                                // Compass Needle pointed to target
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(OceanDeep)
                                            .border(1.5.dp, OceanCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = "Bearing Arrow",
                                            tint = DangerRed,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .rotate(bearing)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${bearing.toInt()}° ${MaritimeMath.getCardinalDirection(bearing)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = OceanLight
                                    )
                                }
                            }

                            if (navWp.type == "JARING_HANYUT") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = WarningAmber.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Water, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Estimasi pergeseran arus: ±${navWp.driftDistanceMeters.toInt()}m ke arah Timur Laut",
                                            fontSize = 11.sp,
                                            color = WarningAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Header & Filter Tags
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR TITIK KOORDINAT (${filteredList.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { filterType = null },
                        label = { Text("Semua", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == "JARING_HANYUT",
                        onClick = { filterType = if (filterType == "JARING_HANYUT") null else "JARING_HANYUT" },
                        label = { Text("Jaring Hanyut", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == "RUMPON",
                        onClick = { filterType = if (filterType == "RUMPON") null else "RUMPON" },
                        label = { Text("Rumpon", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == "SPOT_IKAN",
                        onClick = { filterType = if (filterType == "SPOT_IKAN") null else "SPOT_IKAN" },
                        label = { Text("Spot Ikan", fontSize = 11.sp) }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Belum Ada Titik Koordinat Tersimpan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tekan tombol di bawah untuk menandai lokasi jaring atau spot ikan yang ditinggal.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    WaypointItemCard(
                        item = item,
                        userLat = uiState.userVessel.latitude,
                        userLon = uiState.userVessel.longitude,
                        isSelectedForNav = uiState.selectedNavWaypoint?.id == item.id,
                        onSelectForNav = { viewModel.setNavWaypoint(item) },
                        onToggleRetrieved = { viewModel.toggleWaypointRetrieved(item) },
                        onDelete = { viewModel.deleteWaypoint(item) }
                    )
                }
            }
        }
    }

    // Add Waypoint Modal Dialog
    if (showAddDialog) {
        AddWaypointDialog(
            userLat = uiState.userVessel.latitude,
            userLon = uiState.userVessel.longitude,
            onDismiss = { showAddDialog = false },
            onSave = { title, type, lat, lon, depth, notes, retrievalDate ->
                viewModel.saveNewWaypoint(title, type, lat, lon, depth, notes, retrievalDate)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun WaypointItemCard(
    item: FishNetWaypointEntity,
    userLat: Double,
    userLon: Double,
    isSelectedForNav: Boolean,
    onSelectForNav: () -> Unit,
    onToggleRetrieved: () -> Unit,
    onDelete: () -> Unit
) {
    val distNM = MaritimeMath.calculateDistanceNM(userLat, userLon, item.latitude, item.longitude)
    val bearing = MaritimeMath.calculateBearingDeg(userLat, userLon, item.latitude, item.longitude)

    val typeLabel = when (item.type) {
        "JARING_HANYUT" -> "Jaring Hanyut / Gillnet"
        "RUMPON" -> "Rumpon Ikan"
        "SPOT_IKAN" -> "Spot Ikan Melimpah"
        "BUBU_RAWAI" -> "Bubu / Rawai Dasar"
        else -> "Karang Bahaya"
    }

    val typeColor = when (item.type) {
        "JARING_HANYUT" -> WarningAmber
        "RUMPON" -> SeafoamGreen
        "SPOT_IKAN" -> OceanCyan
        else -> DangerRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRetrieved) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(typeColor)
                    )
                    Column {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Checkbox status retrieved
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isRetrieved) "Sudah Diambil" else "Masih di Laut",
                        fontSize = 11.sp,
                        color = if (item.isRetrieved) SuccessGreen else WarningAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Checkbox(
                        checked = item.isRetrieved,
                        onCheckedChange = { onToggleRetrieved() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coordinates & Distance
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("KOORDINAT GPS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = MaritimeMath.formatMaritimeCoord(item.latitude, item.longitude),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("JARAK DARI KAPAL", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f NM (%s)", distNM, MaritimeMath.getCardinalDirection(bearing)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OceanCyan
                        )
                    }
                }
            }

            if (item.baitOrGearNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${item.baitOrGearNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.depthMeters > 0) {
                Text(
                    text = "Kedalaman: ${item.depthMeters} meter • Target Ambil: ${item.targetRetrievalDate}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Navigate to this waypoint / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onSelectForNav,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedForNav) SuccessGreen else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isSelectedForNav) Color.White else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isSelectedForNav) Icons.Default.Check else Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isSelectedForNav) "Sedang Dituju" else "Arahkan Navigasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddWaypointDialog(
    userLat: Double,
    userLon: Double,
    onDismiss: () -> Unit,
    onSave: (title: String, type: String, lat: Double, lon: Double, depth: Double, notes: String, retrieval: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("JARING_HANYUT") }
    var latStr by remember { mutableStateOf(userLat.toString()) }
    var lonStr by remember { mutableStateOf(userLon.toString()) }
    var depthStr by remember { mutableStateOf("25") }
    var notes by remember { mutableStateOf("") }
    var retrievalStr by remember { mutableStateOf("Besok Pagi (05:30 WIB)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tandai Titik Jaring / Spot Ikan", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Titik / Pelampung") },
                        placeholder = { Text("cth: Jaring Gillnet Pelampung Kuning") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Jenis Titik Koordinat:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "JARING_HANYUT" to "Jaring",
                            "RUMPON" to "Rumpon",
                            "SPOT_IKAN" to "Spot Ikan",
                            "BUBU_RAWAI" to "Bubu"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Koordinat GPS:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                latStr = userLat.toString()
                                lonStr = userLon.toString()
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Posisi Saat Ini", fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latStr,
                            onValueChange = { latStr = it },
                            label = { Text("Lintang (Lat)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lonStr,
                            onValueChange = { lonStr = it },
                            label = { Text("Bujur (Lon)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = depthStr,
                            onValueChange = { depthStr = it },
                            label = { Text("Kedalaman (meter)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = retrievalStr,
                            onValueChange = { retrievalStr = it },
                            label = { Text("Rencana Ambil") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Umpan / Jaring / Arus") },
                        placeholder = { Text("cth: 4 peace jaring mata 3 inch, tali pelampung merah") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull() ?: userLat
                    val lon = lonStr.toDoubleOrNull() ?: userLon
                    val depth = depthStr.toDoubleOrNull() ?: 0.0
                    onSave(title, selectedType, lat, lon, depth, notes, retrievalStr)
                }
            ) {
                Text("Simpan Titik")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
