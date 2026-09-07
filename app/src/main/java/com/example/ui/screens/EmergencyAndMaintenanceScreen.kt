package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EmergencySosEntity
import com.example.data.local.ShipMaintenanceEntity
import com.example.data.model.EmergencySafetyGuide
import com.example.data.util.MaritimeMath
import com.example.ui.theme.*
import com.example.ui.viewmodel.MaritimeUiState
import com.example.ui.viewmodel.MaritimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAndMaintenanceScreen(
    viewModel: MaritimeViewModel,
    uiState: MaritimeUiState,
    maintenances: List<ShipMaintenanceEntity>,
    sosRecords: List<EmergencySosEntity>
) {
    val context = LocalContext.current
    var selectedTabSection by remember { mutableIntStateOf(0) } // 0: SOS Darurat, 1: Panduan Offline, 2: Servis Mesin
    var showSosDialog by remember { mutableStateOf(false) }
    var showAddMaintenanceDialog by remember { mutableStateOf(false) }
    var showEngineHoursDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emergency_maintenance_screen")
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Navigation Tabs
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SectionTabButton(
                        modifier = Modifier.weight(1f),
                        title = "SOS Darurat",
                        icon = Icons.Default.Warning,
                        isSelected = selectedTabSection == 0,
                        color = DangerRed,
                        onClick = { selectedTabSection = 0 }
                    )
                    SectionTabButton(
                        modifier = Modifier.weight(1.1f),
                        title = "Panduan SAR",
                        icon = Icons.Default.MenuBook,
                        isSelected = selectedTabSection == 1,
                        color = OceanCyan,
                        onClick = { selectedTabSection = 1 }
                    )
                    SectionTabButton(
                        modifier = Modifier.weight(1.1f),
                        title = "Servis Kapal",
                        icon = Icons.Default.Build,
                        isSelected = selectedTabSection == 2,
                        color = WarningAmber,
                        onClick = { selectedTabSection = 2 }
                    )
                }
            }
        }

        // ================= SECTION 0: SOS DARURAT & SATELIT =================
        if (selectedTabSection == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, DangerRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DangerRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(42.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "TOMBOL DARURAT SOS PELAUT",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = DangerRed
                        )
                        Text(
                            text = "Kirim koordinat GPS instan via Satelit & Radio VHF ke BASARNAS 115, Stasiun Pantai SROP, & Kapal Terdekat",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Coordinates Box
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Posisi GPS Anda: ${MaritimeMath.formatMaritimeCoord(uiState.userVessel.latitude, uiState.userVessel.longitude)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showSosDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_trigger_emergency_sos")
                        ) {
                            Icon(Icons.Default.Sos, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PILIH JENIS DARURAT & SIARKAN SOS", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Quick Call Channels
            item {
                Text(
                    text = "SALURAN KOMUNIKASI DARURAT RESMI",
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
                    EmergencyCallButton(
                        modifier = Modifier.weight(1f),
                        title = "BASARNAS (115)",
                        subtitle = "Telepon Bebas Pulsa",
                        icon = Icons.Default.Call,
                        color = WarningOrange,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:115"))
                            context.startActivity(intent)
                        }
                    )

                    EmergencyCallButton(
                        modifier = Modifier.weight(1f),
                        title = "Radio VHF Ch 16",
                        subtitle = "156.800 MHz Mayday",
                        icon = Icons.Default.Radio,
                        color = OceanCyan,
                        onClick = {
                            viewModel.triggerBmkgAlarm(context)
                        }
                    )
                }
            }

            // SOS History Records
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "LOG SIARAN DARURAT TERSIMPAN (${sosRecords.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            if (sosRecords.isEmpty()) {
                item {
                    Text(
                        text = "Tidak ada laporan marabahaya aktif. Seluruh sistem beroperasi normal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(sosRecords, key = { it.id }) { record ->
                    SosRecordCard(record, onDelete = { viewModel.deleteSosRecord(record) })
                }
            }
        }

        // ================= SECTION 1: PANDUAN KESELAMATAN DARURAT OFFLINE =================
        if (selectedTabSection == 1) {
            // Fuel & Voyage Range Estimator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OceanCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = WarningAmber)
                            Column {
                                Text("ESTIMASI BAHAN BAKAR & JANGKAUAN JELAJAH", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Kapal: ${uiState.userVessel.name}", fontSize = 10.sp, color = OceanCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val fuelLiter = uiState.userVessel.fuelLiter
                        val maxFuel = uiState.userVessel.maxFuelLiter
                        val speedKnots = uiState.userVessel.currentSpeedKnots
                        val litersPerHour = 12.0 // average fuel consumption for 15-30GT vessel
                        val hoursRemaining = if (litersPerHour > 0) fuelLiter / litersPerHour else 0.0
                        val safeCruisingRadiusNM = (hoursRemaining * speedKnots * 0.8) // with 20% safe reserve

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("SISA SOLAR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$fuelLiter / $maxFuel L", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                            }
                            Column {
                                Text("SISA WAKTU", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("~${String.format(java.util.Locale.US, "%.1f", hoursRemaining)} Jam", fontWeight = FontWeight.Black, fontSize = 15.sp, color = OceanCyan)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("JANGKAUAN AMAN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("~${String.format(java.util.Locale.US, "%.0f", safeCruisingRadiusNM)} NM", fontWeight = FontWeight.Black, fontSize = 15.sp, color = SeafoamGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dihitung dengan kecepatan jelajah $speedKnots Knot dan cadangan solar darurat 20% untuk kondisi arus/ombak.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.OfflinePin, contentDescription = null, tint = SuccessGreen)
                            Text("AKSES OFFLINE 100% DI TENGAH LAUT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "Panduan SOP darurat keselamatan laut standar IMO (International Maritime Organization) & BASARNAS yang dapat diakses tanpa kuota data/internet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text("PANDUAN TANGGAP DARURAT SAR (5):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            items(uiState.offlineSafetyGuides, key = { it.id }) { guide ->
                SafetyGuideExpandableCard(guide)
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text("ATURAN PENCEGAHAN TABRAKAN LAUT (COLREGs 1972):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = WarningAmber)
            }

            items(uiState.colregsRules, key = { it.id }) { rule ->
                ColregsRuleExpandableCard(rule)
            }
        }

        // ================= SECTION 2: SERVIS & PERAWATAN KAPAL =================
        if (selectedTabSection == 2) {
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PENGINGAT PERAWATAN MESIN KAPAL", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Kapal: ${uiState.userVessel.name} (${uiState.userVessel.registrationNo})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showEngineHoursDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Jam Mesin", tint = OceanCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Engine Hours Progress Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TOTAL JAM KERJA MESIN (ENGINE HOURS)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("${uiState.userVessel.engineHours} Jam", fontWeight = FontWeight.Black, fontSize = 24.sp, color = OceanCyan)
                                }
                                Button(
                                    onClick = { viewModel.updateEngineHours(uiState.userVessel.engineHours + 5) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                                ) {
                                    Text("+5 Jam Melaut", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Engine & Electrical Health Matrix
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("TEGANGAN AKI", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("13.4 V", fontSize = 13.sp, fontWeight = FontWeight.Black, color = SeafoamGreen)
                                    Text("Status: Normal", fontSize = 9.sp, color = SeafoamGreen)
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("SUHU PENDINGIN", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("78°C", fontSize = 13.sp, fontWeight = FontWeight.Black, color = OceanCyan)
                                    Text("Status: Optimal", fontSize = 9.sp, color = OceanCyan)
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("TEKANAN OLI", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Text("4.2 Bar", fontSize = 13.sp, fontWeight = FontWeight.Black, color = SeafoamGreen)
                                    Text("Status: Aman", fontSize = 9.sp, color = SeafoamGreen)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JADWAL SERVIS KOMPONEN KAPAL (${maintenances.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    TextButton(onClick = { showAddMaintenanceDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Servis", fontSize = 11.sp)
                    }
                }
            }

            items(maintenances, key = { it.id }) { item ->
                MaintenanceItemCard(
                    item = item,
                    currentEngineHours = uiState.userVessel.engineHours,
                    onMarkDone = { viewModel.markMaintenanceDone(item) },
                    onDelete = { viewModel.deleteMaintenance(item) }
                )
            }
        }
    }

    // Modal Dialog: Emergency SOS Dispatch
    if (showSosDialog) {
        EmergencySosDialog(
            userLat = uiState.userVessel.latitude,
            userLon = uiState.userVessel.longitude,
            onDismiss = { showSosDialog = false },
            onSendSos = { distressType, notes ->
                viewModel.dispatchEmergencySos(context, distressType, notes)
                showSosDialog = false
            }
        )
    }

    // Modal Dialog: Add Maintenance
    if (showAddMaintenanceDialog) {
        AddMaintenanceDialog(
            onDismiss = { showAddMaintenanceDialog = false },
            onSave = { component, interval, guide, notes ->
                viewModel.saveMaintenance(component, interval, guide, notes)
                showAddMaintenanceDialog = false
            }
        )
    }

    // Modal Dialog: Edit Engine Hours
    if (showEngineHoursDialog) {
        var hoursStr by remember { mutableStateOf(uiState.userVessel.engineHours.toString()) }
        AlertDialog(
            onDismissRequest = { showEngineHoursDialog = false },
            title = { Text("Atur Jam Operasional Mesin (Hours Meter)") },
            text = {
                OutlinedTextField(
                    value = hoursStr,
                    onValueChange = { hoursStr = it },
                    label = { Text("Total Jam Kerja Mesin (Hours)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = hoursStr.toIntOrNull() ?: uiState.userVessel.engineHours
                        viewModel.updateEngineHours(h)
                        showEngineHoursDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEngineHoursDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SectionTabButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmergencyCallButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SosRecordCard(record: EmergencySosEntity, onDelete: () -> Unit) {
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
                Text(record.distressType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DangerRed)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Waktu: ${record.timeFormatted}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Posisi: ${MaritimeMath.formatMaritimeCoord(record.latitude, record.longitude)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OceanCyan)
            Text("Status Satelit: ${record.satelliteRelayStatus}", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            if (record.notes.isNotBlank()) {
                Text("Catatan: ${record.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SafetyGuideExpandableCard(guide: EmergencySafetyGuide) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (guide.priority == "KRITIS") DangerRed else WarningAmber)
                    )
                    Column {
                        Text(guide.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${guide.category} • ${guide.priority}", fontSize = 10.sp, color = if (guide.priority == "KRITIS") DangerRed else WarningOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Buka",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                Text(guide.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text("LANGKAH TINDAKAN TANGGAP DARURAT:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                guide.actionSteps.forEach { step ->
                    Text(step, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Sinyal / Isyarat: ${guide.internationalSignal}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanCyan,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColregsRuleExpandableCard(rule: com.example.data.model.ColregsRule) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(WarningAmber)
                    )
                    Column {
                        Text(rule.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(rule.ruleNumber, fontSize = 10.sp, color = OceanCyan, fontWeight = FontWeight.SemiBold)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Buka",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                
                Text("SITUASI:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                Text(rule.situation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("TINDAKAN WAJIB NAKHODA:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DangerRed)
                Text(rule.actionRequired, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("💡 Lampu Navigasi: ${rule.nightLights}", fontSize = 11.sp, color = Color.White)
                        Text("📢 Isyarat Bunyi Suling: ${rule.soundSignal}", fontSize = 11.sp, color = OceanCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceItemCard(
    item: ShipMaintenanceEntity,
    currentEngineHours: Int,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingHours = item.nextDueEngineHours - currentEngineHours
    val isOverdue = remainingHours <= 0
    val progress = if (item.intervalHours > 0) {
        ((item.intervalHours - remainingHours.coerceAtLeast(0)).toFloat() / item.intervalHours).coerceIn(0f, 1f)
    } else 0f

    val statusColor = when {
        isOverdue -> DangerRed
        remainingHours <= 20 -> WarningAmber
        else -> SeafoamGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isOverdue) androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Column {
                        Text(item.componentName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Interval Servis: Setiap ${item.intervalHours} Jam Mesin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Service Interval Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOverdue) "JATUH TEMPO SEKARANG! (Lewat ${-remainingHours} Jam)" else "Tersisa $remainingHours Jam Operasional",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jadwal berikutnya: Pada ${item.nextDueEngineHours} Jam Mesin", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Terakhir Servis: ${item.lastServiceDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = onMarkDone,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isOverdue) DangerRed else SeafoamGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Selesai Servis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (item.maintenanceGuide.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Petunjuk: ${item.maintenanceGuide}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencySosDialog(
    userLat: Double,
    userLon: Double,
    onDismiss: () -> Unit,
    onSendSos: (type: String, notes: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("MESIN_MATI") }
    var notes by remember { mutableStateOf("") }

    val distressOptions = listOf(
        "MESIN_MATI" to "Kerusakan Mesin Induk",
        "KEBOCORAN_LAMBUNG" to "Kebocoran Lambung / Masuk Air",
        "ORANG_JATUH_LAUT" to "Orang Jatuh ke Laut (MOB)",
        "CUACA_EKSTREM" to "Gelombang Ekstrem / Badai",
        "KEBAKARAN" to "Kebakaran Kapal",
        "MEDIS" to "Darurat Medis Kritis"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                Text("Kirim Sinyal Marabahaya SOS", fontWeight = FontWeight.Black, color = DangerRed)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Pilih Jenis Kondisi Darurat:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    distressOptions.forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                colors = RadioButtonDefaults.colors(selectedColor = DangerRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, fontSize = 12.sp, fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                item {
                    Text("Koordinat Terkunci:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = MaritimeMath.formatMaritimeCoord(userLat, userLon),
                            modifier = Modifier.padding(8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanCyan
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Detail Tambahan (Jumlah Korban/Kondisi)") },
                        placeholder = { Text("cth: Butuh bantuan tunda atau evakuasi medis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendSos(selectedType, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("SIARKAN DARURAT SEKARANG", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun AddMaintenanceDialog(
    onDismiss: () -> Unit,
    onSave: (component: String, interval: Int, guide: String, notes: String) -> Unit
) {
    var component by remember { mutableStateOf("") }
    var intervalStr by remember { mutableStateOf("100") }
    var guide by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jadwal Servis Kapal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = component,
                    onValueChange = { component = it },
                    label = { Text("Nama Komponen / Bagian Mesin") },
                    placeholder = { Text("cth: Pelumasan As Kemudi & Baut Kapal") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it },
                    label = { Text("Interval Servis (Jam Kerja)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = guide,
                    onValueChange = { guide = it },
                    label = { Text("Petunjuk Perawatan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val interval = intervalStr.toIntOrNull() ?: 100
                    onSave(component, interval, guide, notes)
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
