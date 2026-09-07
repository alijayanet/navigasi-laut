package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.local.FishCatchLogEntity
import com.example.data.util.MaritimeMath
import com.example.ui.theme.*
import com.example.ui.viewmodel.MaritimeUiState
import com.example.ui.viewmodel.MaritimeViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchLogScreen(
    viewModel: MaritimeViewModel,
    uiState: MaritimeUiState,
    catchLogs: List<FishCatchLogEntity>,
    totalWeight: Double?,
    totalRevenue: Double?
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    val rupiahFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SeafoamGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_catch_log"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Tangkapan")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Catat Hasil Tangkapan", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("catch_log_screen")
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fishery Catch Statistics Overview Card
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
                            Text(
                                text = "LOGBOOK TANGKAPAN IKAN TERUKUR",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = SeafoamGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "E-Logbook KKP",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SeafoamGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TOTAL TANGKAPAN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${String.format("%.1f", totalWeight ?: 0.0)} Kg",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OceanCyan
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("ESTIMASI PENDAPATAN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = rupiahFormatter.format(totalRevenue ?: 0.0).replace(",00", ""),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SeafoamGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Eco, contentDescription = null, tint = SeafoamGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Keberlanjutan: Sesuai kuota penangkapan terukur WPPNRI. Data tersimpan offline & siap disinkronisasi.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (catchLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        com.example.data.util.CatchLogExportHelper.shareCatchReport(
                                            context = context,
                                            catchLogs = catchLogs,
                                            vesselName = uiState.userVessel.name,
                                            totalWeight = totalWeight ?: 0.0,
                                            totalRevenue = totalRevenue ?: 0.0
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SeafoamGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bagikan Laporan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val csv = com.example.data.util.CatchLogExportHelper.generateCatchLogCsv(
                                            catchLogs = catchLogs,
                                            vesselName = uiState.userVessel.name
                                        )
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("E-Logbook CSV", csv)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Data format CSV disalin ke clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.9f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = OceanCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salin CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OceanCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Catch Records List Header
            item {
                Text(
                    text = "RIWAYAT PENANGKAPAN IKAN (${catchLogs.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            if (catchLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhonelinkRing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Belum Ada Catatan Tangkapan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Catat jenis ikan, berat, dan harga penjualan Anda secara mudah dan akurat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(catchLogs, key = { it.id }) { log ->
                    CatchLogCard(
                        log = log,
                        rupiahFormatter = rupiahFormatter,
                        onDelete = { viewModel.deleteCatchLog(log) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCatchLogDialog(
            onDismiss = { showAddDialog = false },
            onSave = { species, weight, price, gear, wpp, notes ->
                viewModel.recordCatchLog(species, weight, price, gear, wpp, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CatchLogCard(
    log: FishCatchLogEntity,
    rupiahFormatter: NumberFormat,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SeafoamGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Water, contentDescription = null, tint = SeafoamGreen, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(log.fishSpecies, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${log.dateStr} • ${log.wppnriZone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Berat Tangkapan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${log.weightKg} Kg", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Harga Satuan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rupiahFormatter.format(log.pricePerKg).replace(",00", "") + "/kg", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Estimasi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rupiahFormatter.format(log.estimatedRevenue).replace(",00", ""), fontWeight = FontWeight.Black, fontSize = 14.sp, color = SeafoamGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Alat: ${log.fishingGear}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("GPS: ${MaritimeMath.formatMaritimeCoord(log.latitude, log.longitude)}", fontSize = 10.sp, color = OceanCyan)
                }
            }

            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Catatan: ${log.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCatchLogDialog(
    onDismiss: () -> Unit,
    onSave: (species: String, weight: Double, price: Double, gear: String, wpp: String, notes: String) -> Unit
) {
    var species by remember { mutableStateOf("Tongkol") }
    var weightStr by remember { mutableStateOf("150") }
    var priceStr by remember { mutableStateOf("25000") }
    var selectedGear by remember { mutableStateOf("Jaring Insang (Gillnet)") }
    var selectedWpp by remember { mutableStateOf("WPP 712 (Laut Jawa)") }
    var notes by remember { mutableStateOf("") }

    val commonSpecies = listOf("Tongkol", "Cakalang", "Tuna", "Kembung", "Teri", "Kakap Merah", "Tenggiri", "Cumi-cumi", "Lobster")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Hasil Tangkapan Ikan", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Pilih Spesies Ikan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        commonSpecies.chunked(3).forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowItems.forEach { item ->
                                    FilterChip(
                                        selected = species == item,
                                        onClick = { species = item },
                                        label = { Text(item, fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = species,
                        onValueChange = { species = it },
                        label = { Text("Nama Ikan / Spesies Lain") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("Berat (Kg)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Harga/Kg (Rp)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = selectedGear,
                        onValueChange = { selectedGear = it },
                        label = { Text("Alat Tangkap") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = selectedWpp,
                        onValueChange = { selectedWpp = it },
                        label = { Text("Wilayah Perairan (WPP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Tambahan") },
                        placeholder = { Text("cth: Hasil tangkapan fajar, kondisi sangat segar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightStr.toDoubleOrNull() ?: 0.0
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    onSave(species, w, p, selectedGear, selectedWpp, notes)
                }
            ) {
                Text("Simpan Logbook")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
