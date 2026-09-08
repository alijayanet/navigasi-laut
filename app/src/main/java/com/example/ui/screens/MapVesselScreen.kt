package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FishNetWaypointEntity
import com.example.data.model.*
import com.example.data.util.MaritimeMath
import com.example.data.util.NauticalTileManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.MaritimeUiState
import com.example.ui.viewmodel.MaritimeViewModel
import com.example.ui.viewmodel.UserVesselState
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapVesselScreen(
    viewModel: MaritimeViewModel,
    uiState: MaritimeUiState,
    waypoints: List<FishNetWaypointEntity>
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Keep screen awake while on navigation map
    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    val tileManager = remember { NauticalTileManager(context) }
    val tileRevision by tileManager.tileRevision

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showLayerPicker by remember { mutableStateOf(false) }
    var showSafetyDepthDialog by remember { mutableStateOf(false) }
    var showNmeaDialog by remember { mutableStateOf(false) }
    var showFishFinderDialog by remember { mutableStateOf(false) }
    var showAnchorDialog by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showTideDialog by remember { mutableStateOf(false) }
    var showEditVesselDialog by remember { mutableStateOf(false) }
    var isHudExpanded by remember(isLandscape) { mutableStateOf(!isLandscape) }

    val filteredVessels = remember(uiState.vessels, uiState.vesselFilter) {
        if (uiState.vesselFilter == null) uiState.vessels
        else uiState.vessels.filter { it.type == uiState.vesselFilter }
    }

    // Map geographic reference center (follows user vessel or selected area)
    val refLat = uiState.userVessel.latitude
    val refLon = uiState.userVessel.longitude

    // Automatically load local vessels, buoys & lighthouses when map viewport or GPS position moves
    LaunchedEffect(refLat, refLon, offsetX, offsetY, scale) {
        val zoomContinuous = 11.0 + ln(scale.toDouble()) / ln(2.0)
        val intZoom = zoomContinuous.toInt().coerceIn(4, 16)
        val zoomSubFactor = 2.0.pow(zoomContinuous - intZoom).toFloat()
        val renderedTileSize = NauticalTileManager.TILE_SIZE * zoomSubFactor
        val numTiles = 1 shl intZoom

        val centerNormX = NauticalTileManager.lonToNormalizedX(refLon)
        val centerNormY = NauticalTileManager.latToNormalizedY(refLat)
        val centerTileX = centerNormX * numTiles - (offsetX / renderedTileSize)
        val centerTileY = centerNormY * numTiles - (offsetY / renderedTileSize)

        val currentVisibleLon = NauticalTileManager.normalizedXToLon(centerTileX / numTiles)
        val currentVisibleLat = NauticalTileManager.normalizedYToLat(centerTileY / numTiles)

        viewModel.loadMaritimeDataForRegion(currentVisibleLat, currentVisibleLon)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_vessel_screen")
    ) {
        // ================= 1. HIGH-RESOLUTION SLIPPY TILE & NAUTICAL CANVAS =================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF03101E)) // Navionics deep marine background
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 6.0f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(
                    filteredVessels,
                    uiState.zppiZones,
                    uiState.navigationalAids,
                    uiState.lighthouses,
                    uiState.isRulerToolActive,
                    scale,
                    offsetX,
                    offsetY
                ) {
                    detectTapGestures { tapOffset ->
                        val canvasW = size.width
                        val canvasH = size.height
                        val centerX = canvasW / 2f + offsetX
                        val centerY = canvasH / 2f + offsetY

                        val zoomContinuous = 11.0 + ln(scale.toDouble()) / ln(2.0)
                        val intZoom = zoomContinuous.toInt().coerceIn(4, 16)
                        val zoomSubFactor = 2.0.pow(zoomContinuous - intZoom).toFloat()
                        val renderedTileSize = NauticalTileManager.TILE_SIZE * zoomSubFactor
                        val numTiles = 1 shl intZoom

                        val centerNormX = NauticalTileManager.lonToNormalizedX(refLon)
                        val centerNormY = NauticalTileManager.latToNormalizedY(refLat)
                        val centerTileX = centerNormX * numTiles
                        val centerTileY = centerNormY * numTiles

                        // Inverse projection: Screen tap to GPS (Lat/Lon)
                        val tileX = centerTileX + (tapOffset.x - centerX) / renderedTileSize
                        val tileY = centerTileY + (tapOffset.y - centerY) / renderedTileSize
                        val normX = tileX / numTiles
                        val normY = tileY / numTiles
                        val tappedLon = NauticalTileManager.normalizedXToLon(normX)
                        val tappedLat = NauticalTileManager.normalizedYToLat(normY)

                        fun geoToScreen(lat: Double, lon: Double): Offset {
                            val gx = NauticalTileManager.lonToNormalizedX(lon) * numTiles
                            val gy = NauticalTileManager.latToNormalizedY(lat) * numTiles
                            val sx = centerX + ((gx - centerTileX) * renderedTileSize).toFloat()
                            val sy = centerY + ((gy - centerTileY) * renderedTileSize).toFloat()
                            return Offset(sx, sy)
                        }

                        val touchThresholdPx = 36f * density
                        val touchThresholdSq = touchThresholdPx * touchThresholdPx

                        // 1. Handle Ruler Tool Tap
                        if (uiState.isRulerToolActive) {
                            if (uiState.rulerStartPoint == null) {
                                viewModel.setRulerPoints(Pair(tappedLat, tappedLon), null)
                            } else if (uiState.rulerEndPoint == null) {
                                viewModel.setRulerPoints(uiState.rulerStartPoint, Pair(tappedLat, tappedLon))
                            } else {
                                viewModel.setRulerPoints(Pair(tappedLat, tappedLon), null)
                            }
                            return@detectTapGestures
                        }

                        // 2. Check Tap on Lighthouses
                        val tappedLight = uiState.lighthouses.firstOrNull { light ->
                            val sPos = geoToScreen(light.latitude, light.longitude)
                            val dx = tapOffset.x - sPos.x
                            val dy = tapOffset.y - sPos.y
                            (dx * dx + dy * dy) < touchThresholdSq * 1.5f
                        }
                        if (tappedLight != null) {
                            viewModel.selectLighthouse(tappedLight)
                            return@detectTapGestures
                        }

                        // 3. Check Tap on Navigational Aids (Buoys / Buih)
                        val tappedAtoN = uiState.navigationalAids.firstOrNull { aton ->
                            val sPos = geoToScreen(aton.latitude, aton.longitude)
                            val dx = tapOffset.x - sPos.x
                            val dy = tapOffset.y - sPos.y
                            (dx * dx + dy * dy) < touchThresholdSq * 1.5f
                        }
                        if (tappedAtoN != null) {
                            viewModel.selectAtoN(tappedAtoN)
                            return@detectTapGestures
                        }

                        // 4. Check Tap on ZPPI Fishing Hotspots
                        if (uiState.showZppiLayerOnMap) {
                            val tappedZppi = uiState.zppiZones.firstOrNull { z ->
                                val sPos = geoToScreen(z.latitude, z.longitude)
                                val dx = tapOffset.x - sPos.x
                                val dy = tapOffset.y - sPos.y
                                (dx * dx + dy * dy) < touchThresholdSq * 1.5f
                            }
                            if (tappedZppi != null) {
                                viewModel.selectZppi(tappedZppi)
                                return@detectTapGestures
                            }
                        }

                        // 5. Check Tap on Own Boat (Kapal Sendiri)
                        val ownPos = geoToScreen(uiState.userVessel.latitude, uiState.userVessel.longitude)
                        val odx = tapOffset.x - ownPos.x
                        val ody = tapOffset.y - ownPos.y
                        if ((odx * odx + ody * ody) < touchThresholdSq * 2.2f) {
                            showEditVesselDialog = true
                            return@detectTapGestures
                        }

                        // 6. Check Tap on AIS Vessels
                        val tappedVessel = filteredVessels.firstOrNull { v ->
                            val sPos = geoToScreen(v.latitude, v.longitude)
                            val dx = tapOffset.x - sPos.x
                            val dy = tapOffset.y - sPos.y
                            (dx * dx + dy * dy) < touchThresholdSq * 1.5f
                        }
                        if (tappedVessel != null) {
                            viewModel.selectVessel(tappedVessel)
                            return@detectTapGestures
                        }

                        // Tap on open sea dismisses cards
                        viewModel.selectVessel(null)
                        viewModel.selectAtoN(null)
                        viewModel.selectLighthouse(null)
                        viewModel.selectZppi(null)
                    }
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val rev = tileRevision

            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f + offsetX
            val centerY = canvasH / 2f + offsetY

            val zoomContinuous = 11.0 + ln(scale.toDouble()) / ln(2.0)
            val intZoom = zoomContinuous.toInt().coerceIn(4, 16)
            val zoomSubFactor = 2.0.pow(zoomContinuous - intZoom).toFloat()
            val renderedTileSize = NauticalTileManager.TILE_SIZE * zoomSubFactor
            val numTiles = 1 shl intZoom

            val centerNormX = NauticalTileManager.lonToNormalizedX(refLon)
            val centerNormY = NauticalTileManager.latToNormalizedY(refLat)
            val centerTileX = centerNormX * numTiles
            val centerTileY = centerNormY * numTiles

            fun geoToCanvas(lat: Double, lon: Double): Offset {
                val gx = NauticalTileManager.lonToNormalizedX(lon) * numTiles
                val gy = NauticalTileManager.latToNormalizedY(lat) * numTiles
                val sx = centerX + ((gx - centerTileX) * renderedTileSize).toFloat()
                val sy = centerY + ((gy - centerTileY) * renderedTileSize).toFloat()
                return Offset(sx, sy)
            }

            // 1. Draw High-Resolution Slippy Map Tiles (Real Satellite HD / OSM Marine / SonarChart)
            drawSlippyMapTiles(
                tileManager = tileManager,
                centerX = centerX,
                centerY = centerY,
                centerTileX = centerTileX,
                centerTileY = centerTileY,
                intZoom = intZoom,
                renderedTileSize = renderedTileSize,
                canvasW = canvasW,
                canvasH = canvasH,
                layer = uiState.navionicsChartLayer
            )

            // 2. Draw Nautical Graticules & Geographic Coordinates Grid
            drawNavionicsGridMercator(
                centerX = centerX,
                centerY = centerY,
                centerTileX = centerTileX,
                centerTileY = centerTileY,
                intZoom = intZoom,
                renderedTileSize = renderedTileSize,
                canvasW = canvasW,
                canvasH = canvasH
            )

            // 3. Draw Traffic Separation Scheme (TSS Lanes) & Marine Hazards (Shipwrecks, Subsea Cables)
            drawNavionicsHazardsAndTSS(::geoToCanvas, scale)

            // 4. Draw Official Spot Soundings (Angka Kedalaman Laut IHO ENC / Pushidrosal)
            drawNavionicsSpotSoundings(::geoToCanvas, uiState.spotSoundings, scale)

            // 5. Draw Navigational Aids (AtoN - IALA Region A Buoys / Pelampung Suar)
            drawNavionicsAtoN(::geoToCanvas, uiState.navigationalAids, uiState.selectedAtoN, scale)

            // 6. Draw Lighthouses with Light Beam Arcs & Name Labels
            drawNavionicsLighthouses(::geoToCanvas, uiState.lighthouses, uiState.selectedLighthouse, scale)

            // 7. Draw ZPPI Satellite Fishing Hotspots
            if (uiState.showZppiLayerOnMap) {
                drawZppiHotspots(::geoToCanvas, uiState.zppiZones, scale)
            }

            // 8. Draw Fishing Waypoints (Nets, Rumpons, Hotspots)
            drawWaypoints(::geoToCanvas, waypoints, scale)

            // 9. Draw Active Breadcrumb Tracks (Perekam Jejak Rute)
            drawBreadcrumbTracks(::geoToCanvas, uiState.breadcrumbTracks, scale, uiState.isNightVisionActive)

            // 10. Draw Anchor Watch Zone (Lingkaran Batas Aman Jangkar)
            if (uiState.anchorWatch.isActive) {
                drawAnchorWatchZone(
                    geoToCanvas = ::geoToCanvas,
                    anchor = uiState.anchorWatch,
                    numTiles = numTiles,
                    renderedTileSize = renderedTileSize,
                    scale = scale
                )
            }

            // 11. Draw Active Navigation Highway Route Line (Glowing Route Corridor)
            drawNavigationHighway(::geoToCanvas, uiState)

            // 12. Draw AIS Vessels with Vector Speed Lines & Name Labels
            drawAisVessels(::geoToCanvas, filteredVessels, uiState.selectedVessel, scale)

            // 13. Draw CPA Collision Danger Vectors & Pulsing Halos
            drawCpaCollisionRisks(::geoToCanvas, uiState.dangerousCpaTargets, scale)

            // 14. Draw User's Vessel (Own Boat with Heading Cone & Course Projection)
            drawUserVessel(::geoToCanvas, uiState.userVessel, scale)

            // 15. Draw Ruler Measurement Tool (if active)
            drawRulerTool(::geoToCanvas, uiState.rulerStartPoint, uiState.rulerEndPoint)

            // 16. Draw Floating Navionics Compass Rose
            drawCompassRose(canvasW, canvasH, uiState.userVessel.headingDeg)

            // 17. Night Vision Tint Overlay (Mode Navigasi Anjungan Malam)
            if (uiState.isNightVisionActive) {
                drawRect(
                    color = Color(0x35FF0000),
                    size = size,
                    blendMode = BlendMode.SrcOver
                )
            }
        }

        // ================= 2. TOP RESPONSIVE NAVIONICS HUD (GPS BADGE & TELEMETRY) =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .then(if (isLandscape) Modifier.padding(top = 8.dp) else Modifier.statusBarsPadding())
        ) {
            // Collision CPA Danger Alert Banner
            if (uiState.dangerousCpaTargets.isNotEmpty()) {
                val mostDangerous = uiState.dangerousCpaTargets.minByOrNull { it.cpaDistanceNM }
                if (mostDangerous != null) {
                    CpaCollisionWarningBanner(
                        danger = mostDangerous,
                        onDismiss = { viewModel.dismissCpaAlarm() }
                    )
                }
            }

            // Anchor Drag Alarm Banner
            if (uiState.anchorWatch.isAlarmTriggered) {
                AnchorDragAlertBanner(
                    anchor = uiState.anchorWatch,
                    onDismiss = { viewModel.dismissAnchorAlarm() }
                )
            }

            // Unified Compact GPS & Telemetry HUD Ribbon
            NavionicsUnifiedGpsHud(
                user = uiState.userVessel,
                sonarDepthMeters = uiState.sonarDepthMeters,
                locationName = uiState.currentSeaLocationName,
                isUsingRealGps = uiState.isUsingRealDeviceGps,
                navWaypoint = uiState.selectedNavWaypoint,
                navZppi = uiState.selectedZppi,
                selectedVessel = uiState.selectedVessel,
                selectedAtoN = uiState.selectedAtoN,
                selectedLighthouse = uiState.selectedLighthouse,
                isExpanded = isHudExpanded,
                isLandscape = isLandscape,
                onToggleExpand = { isHudExpanded = !isHudExpanded },
                onOpenLocationPicker = { showLocationPicker = true },
                onOpenLayerPicker = { showLayerPicker = true }
            )

            // Expanded Mode Extra Toolbar & Filter Chips (in Portrait or when expanded)
            AnimatedVisibility(
                visible = isHudExpanded && !isLandscape,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Toolbar Row (Location, Layer, Ruler, Safety Depth, Tools)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Selector Chip
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.94f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 6.dp,
                            modifier = Modifier.clickable { showLocationPicker = true }.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = "Lokasi",
                                    tint = if (uiState.isUsingRealDeviceGps) SeafoamGreen else OceanCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = uiState.currentSeaLocationName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Navionics Floating Action Tools
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showLayerPicker = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, OceanCyan.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = "Layer Peta", tint = OceanCyan, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.toggleRulerTool() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isRulerToolActive) WarningAmber else Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, if (uiState.isRulerToolActive) WarningAmber else Color(0xFF334155), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Straighten,
                                    contentDescription = "Penggaris Navigasi",
                                    tint = if (uiState.isRulerToolActive) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showSafetyDepthDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Safety Depth", tint = SeafoamGreen, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.syncWithRealDeviceGps(context) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isUsingRealDeviceGps) SeafoamGreen else Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, if (uiState.isUsingRealDeviceGps) SeafoamGreen else Color(0xFF334155), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.GpsFixed,
                                    contentDescription = "Kunci GPS HP",
                                    tint = if (uiState.isUsingRealDeviceGps) Color.Black else OceanCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showFishFinderDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Radar, contentDescription = "Fish Finder Sonar", tint = OceanCyan, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { showAnchorDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.anchorWatch.isActive) (if (uiState.anchorWatch.isAlarmTriggered) DangerRed else SeafoamGreen) else Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, if (uiState.anchorWatch.isActive) (if (uiState.anchorWatch.isAlarmTriggered) DangerRed else SeafoamGreen) else Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Anchor, contentDescription = "Lego Jangkar", tint = if (uiState.anchorWatch.isActive) (if (uiState.anchorWatch.isAlarmTriggered) Color.White else Color.Black) else SeafoamGreen, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { showTrackDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isRecordingTrack) DangerRed.copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, if (uiState.isRecordingTrack) DangerRed else Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Route, contentDescription = "Rekam Jejak", tint = if (uiState.isRecordingTrack) DangerRed else OceanCyan, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { showTideDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Water, contentDescription = "Pasang Surut & Arus", tint = OceanCyan, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.toggleNightVision() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isNightVisionActive) DangerRed else Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, if (uiState.isNightVisionActive) DangerRed else Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Nightlight, contentDescription = "Mode Malam", tint = if (uiState.isNightVisionActive) Color.White else Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { showEditVesselDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, WarningAmber, CircleShape)
                            ) {
                                Icon(Icons.Default.DirectionsBoat, contentDescription = "Profil & Nama Kapal", tint = WarningAmber, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { showNmeaDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                    .border(1.dp, Color(0xFF334155), CircleShape)
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = "NMEA 0183", tint = SeafoamGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Map Layer Selector Row (Satelit HD, OSM Laut, SonarChart, Arus)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                    ) {
                        item {
                            val isSat = uiState.navionicsChartLayer == NavionicsChartLayer.SATELLITE_OVERLAY
                            FilterChip(
                                selected = isSat,
                                onClick = { viewModel.setNavionicsChartLayer(NavionicsChartLayer.SATELLITE_OVERLAY) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (isSat) Color.Black else OceanCyan)
                                        Text("🛰️ Satelit HD", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OceanCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        item {
                            val isEnc = uiState.navionicsChartLayer == NavionicsChartLayer.NAUTICAL_ENC
                            FilterChip(
                                selected = isEnc,
                                onClick = { viewModel.setNavionicsChartLayer(NavionicsChartLayer.NAUTICAL_ENC) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (isEnc) Color.Black else SeafoamGreen)
                                        Text("🗺️ Peta Laut OSM", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SeafoamGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        item {
                            val isSonar = uiState.navionicsChartLayer == NavionicsChartLayer.SONARCHART_HD
                            FilterChip(
                                selected = isSonar,
                                onClick = { viewModel.setNavionicsChartLayer(NavionicsChartLayer.SONARCHART_HD) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Waves, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (isSonar) Color.Black else WarningAmber)
                                        Text("🌊 SonarChart™", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningAmber,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.showZppiLayerOnMap,
                                onClick = { viewModel.toggleZppiMapLayer() },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SeafoamGreen))
                                        Text("Spot Ikan (${uiState.zppiZones.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SeafoamGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        items(VesselType.values()) { type ->
                            val count = uiState.vessels.count { it.type == type }
                            FilterChip(
                                selected = uiState.vesselFilter == type,
                                onClick = { viewModel.setVesselFilter(if (uiState.vesselFilter == type) null else type) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(type.color))
                                        Text("${type.label} ($count)", fontSize = 10.5.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = type.color,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // ================= 3. FLOATING ACTION CONTROLS & ZOOM DOCK =================
        if (isLandscape) {
            // Landscape Mode: Compact Vertical Floating Dock on the Right Edge
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF334155).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { showLayerPicker = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Layers, contentDescription = "Layer Peta", tint = OceanCyan, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { viewModel.toggleRulerTool() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Straighten, contentDescription = "Ruler", tint = if (uiState.isRulerToolActive) WarningAmber else Color.White, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showSafetyDepthDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Security, contentDescription = "Safety Depth", tint = SeafoamGreen, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { viewModel.syncWithRealDeviceGps(context) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "GPS Lock", tint = if (uiState.isUsingRealDeviceGps) SeafoamGreen else OceanCyan, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showFishFinderDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Radar, contentDescription = "Sonar", tint = OceanCyan, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showAnchorDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Anchor, contentDescription = "Jangkar", tint = if (uiState.anchorWatch.isActive) (if (uiState.anchorWatch.isAlarmTriggered) DangerRed else SeafoamGreen) else SeafoamGreen, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showTrackDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Route, contentDescription = "Jejak", tint = if (uiState.isRecordingTrack) DangerRed else OceanCyan, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showTideDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Water, contentDescription = "Pasang Surut", tint = OceanCyan, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { viewModel.toggleNightVision() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Nightlight, contentDescription = "Mode Malam", tint = if (uiState.isNightVisionActive) DangerRed else Color.White, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showEditVesselDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DirectionsBoat, contentDescription = "Nama Kapal", tint = WarningAmber, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showNmeaDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Sensors, contentDescription = "NMEA", tint = SeafoamGreen, modifier = Modifier.size(17.dp))
                }

                HorizontalDivider(modifier = Modifier.width(22.dp).padding(vertical = 2.dp), color = Color(0xFF334155))

                IconButton(onClick = { scale = (scale * 1.3f).coerceAtMost(6.0f) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { scale = (scale / 1.3f).coerceAtLeast(0.4f) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { scale = 1.0f; offsetX = 0f; offsetY = 0f }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Pusatkan", tint = OceanCyan, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            // Portrait Mode: Floating Zoom and Re-center at Bottom-End
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { scale = (scale * 1.3f).coerceAtMost(6.0f) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                        .border(1.dp, Color(0xFF334155), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { scale = (scale / 1.3f).coerceAtLeast(0.4f) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                        .border(1.dp, Color(0xFF334155), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { scale = 1.0f; offsetX = 0f; offsetY = 0f },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                        .border(1.dp, OceanCyan, CircleShape)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Pusatkan Kapal", tint = OceanCyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ================= 4. RESPONSIVE DETAIL OVERLAYS (SIDE PANEL IN LANDSCAPE) =================
        Box(
            modifier = Modifier
                .then(
                    if (isLandscape) {
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 8.dp)
                            .widthIn(max = 380.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    }
                )
        ) {
            AnimatedVisibility(
                visible = uiState.selectedAtoN != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.selectedAtoN?.let { aton ->
                    NavigationalAidDetailCard(
                        aton = aton,
                        userLat = uiState.userVessel.latitude,
                        userLon = uiState.userVessel.longitude,
                        onClose = { viewModel.selectAtoN(null) }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.selectedLighthouse != null && uiState.selectedAtoN == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.selectedLighthouse?.let { light ->
                    LighthouseDetailCard(
                        light = light,
                        userLat = uiState.userVessel.latitude,
                        userLon = uiState.userVessel.longitude,
                        onClose = { viewModel.selectLighthouse(null) }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.selectedVessel != null && uiState.selectedAtoN == null && uiState.selectedLighthouse == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.selectedVessel?.let { vessel ->
                    VesselDetailBottomCard(
                        vessel = vessel,
                        userLat = uiState.userVessel.latitude,
                        userLon = uiState.userVessel.longitude,
                        onClose = { viewModel.selectVessel(null) }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.selectedZppi != null && uiState.selectedVessel == null && uiState.selectedAtoN == null && uiState.selectedLighthouse == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.selectedZppi?.let { zppi ->
                    ZppiDetailBottomCard(
                        zppi = zppi,
                        onClose = { viewModel.selectZppi(null) },
                        onNavigateToTarget = {
                            viewModel.setZppiAsNavTarget(zppi)
                            android.widget.Toast.makeText(context, "Navigasi diaktifkan ke: ${zppi.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isRulerToolActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                RulerMeasurementBar(
                    start = uiState.rulerStartPoint ?: Pair(uiState.userVessel.latitude, uiState.userVessel.longitude),
                    end = uiState.rulerEndPoint,
                    onReset = { viewModel.setRulerPoints(null, null) },
                    onClose = { viewModel.toggleRulerTool() }
                )
            }
        }
    }

    // ================= 5. MODAL DIALOGS =================
    if (showLocationPicker) {
        LocationPickerDialog(
            onSelectLocation = { lat, lon, name ->
                viewModel.changeUserSeaLocation(lat, lon, name)
                offsetX = 0f
                offsetY = 0f
                showLocationPicker = false
            },
            onSyncRealGps = {
                viewModel.syncWithRealDeviceGps(context)
                offsetX = 0f
                offsetY = 0f
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
    }

    if (showLayerPicker) {
        NavionicsLayerDialog(
            currentLayer = uiState.navionicsChartLayer,
            onSelectLayer = { layer ->
                viewModel.setNavionicsChartLayer(layer)
                showLayerPicker = false
            },
            onDismiss = { showLayerPicker = false }
        )
    }

    if (showSafetyDepthDialog) {
        SafetyDepthDialog(
            currentDepth = uiState.safetyDepthMeters,
            onSelectDepth = { depth ->
                viewModel.setSafetyDepthMeters(depth)
                showSafetyDepthDialog = false
            },
            onDismiss = { showSafetyDepthDialog = false }
        )
    }

    if (showFishFinderDialog) {
        FishFinderSonarDialog(
            depthMeters = uiState.sonarDepthMeters,
            waterTempC = uiState.sonarWaterTempC,
            fishDetections = uiState.sonarDetections,
            onDismiss = { showFishFinderDialog = false },
            onMarkWaypoint = {
                viewModel.saveSonarSpotAsWaypoint(context)
                showFishFinderDialog = false
                android.widget.Toast.makeText(context, "Titik Ikan Disimpan ke Waypoint!", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showNmeaDialog) {
        NmeaTelemetryDialog(
            userLat = uiState.userVessel.latitude,
            userLon = uiState.userVessel.longitude,
            speedKnots = uiState.userVessel.currentSpeedKnots,
            headingDeg = uiState.userVessel.headingDeg,
            onDismiss = { showNmeaDialog = false }
        )
    }

    if (showAnchorDialog) {
        AnchorWatchDialog(
            anchorState = uiState.anchorWatch,
            onStartAnchorWatch = { radius ->
                viewModel.startAnchorWatch(radius)
            },
            onStopAnchorWatch = {
                viewModel.stopAnchorWatch()
            },
            onDismiss = { showAnchorDialog = false }
        )
    }

    if (showTrackDialog) {
        TrackRecorderDialog(
            isRecording = uiState.isRecordingTrack,
            tracks = uiState.breadcrumbTracks,
            onToggleRecording = { viewModel.toggleTrackRecording() },
            onClearTracks = { viewModel.clearTrackRecording() },
            onBacktrack = { viewModel.backtrackRoute() },
            onDismiss = { showTrackDialog = false }
        )
    }

    if (showTideDialog) {
        val tide = uiState.tidePrediction ?: com.example.data.util.MaritimeMath.predictTideAndCurrent(uiState.userVessel.latitude, uiState.userVessel.longitude)
        TideCurrentDialog(
            tide = tide,
            locationName = uiState.currentSeaLocationName,
            onDismiss = { showTideDialog = false }
        )
    }

    if (showEditVesselDialog) {
        EditVesselProfileDialog(
            userVessel = uiState.userVessel,
            onSaveProfile = { name, reg, cap, gt, crew ->
                viewModel.updateUserVesselProfile(name, reg, cap, gt, crew)
                android.widget.Toast.makeText(context, "Profil Kapal '$name' Berhasil Disimpan!", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showEditVesselDialog = false }
        )
    }
}

// =========================================================================
// NAVIONICS HUD TAPE BAR & UNIFIED GPS TELEMETRY BADGE
// =========================================================================

@Composable
private fun NavionicsUnifiedGpsHud(
    user: UserVesselState,
    sonarDepthMeters: Float,
    locationName: String,
    isUsingRealGps: Boolean,
    navWaypoint: FishNetWaypointEntity?,
    navZppi: ZppiFishingZone?,
    selectedVessel: Vessel?,
    selectedAtoN: NavigationalAid?,
    selectedLighthouse: Lighthouse?,
    isExpanded: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    onToggleExpand: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    onOpenLayerPicker: () -> Unit
) {
    val targetLat = navWaypoint?.latitude ?: navZppi?.latitude ?: selectedVessel?.latitude ?: selectedAtoN?.latitude ?: selectedLighthouse?.latitude
    val targetLon = navWaypoint?.longitude ?: navZppi?.longitude ?: selectedVessel?.longitude ?: selectedAtoN?.longitude ?: selectedLighthouse?.longitude

    val dtwNM = if (targetLat != null && targetLon != null) {
        MaritimeMath.calculateDistanceNM(user.latitude, user.longitude, targetLat, targetLon)
    } else null

    val brgDeg = if (targetLat != null && targetLon != null) {
        MaritimeMath.calculateBearingDeg(user.latitude, user.longitude, targetLat, targetLon)
    } else user.headingDeg

    val etaStr = if (dtwNM != null && user.currentSpeedKnots > 0.5) {
        val hours = dtwNM / user.currentSpeedKnots
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        "${h}j ${m}m"
    } else if (dtwNM != null) "--:--" else "SIAGA"

    val speedKmH = user.currentSpeedKnots * 1.852

    if (isLandscape || !isExpanded) {
        // Ultra-compact single horizontal HUD pill/bar (integrated into GPS badge)
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color(0xFF071426).copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, OceanCyan.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // GPS Location Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenLocationPicker() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "GPS",
                        tint = if (isUsingRealGps) SeafoamGreen else OceanCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = locationName.take(18),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                // Inline Telemetry Badges (SOG, COG, DEPTH, BRG, DTW, ETA)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 6.dp)
                ) {
                    // SOG
                    CompactTelemetryPill(
                        label = "SOG",
                        value = String.format(Locale.US, "%.1f kts", user.currentSpeedKnots),
                        subValue = String.format(Locale.US, "%.0f km/j", speedKmH),
                        color = OceanCyan
                    )
                    // COG
                    CompactTelemetryPill(
                        label = "COG",
                        value = String.format(Locale.US, "%03.0f°", user.headingDeg),
                        subValue = MaritimeMath.getCardinalDirection(user.headingDeg).substringBefore(" "),
                        color = Color.White
                    )
                    // DEPTH
                    CompactTelemetryPill(
                        label = "DEPTH",
                        value = String.format(Locale.US, "%.1f m", sonarDepthMeters),
                        subValue = if (sonarDepthMeters < 5.0f) "DANGKAL" else "AMAN",
                        color = if (sonarDepthMeters < 5.0f) WarningOrange else SeafoamGreen
                    )
                    // BRG
                    CompactTelemetryPill(
                        label = "BRG",
                        value = String.format(Locale.US, "%03.0f°", brgDeg),
                        subValue = if (targetLat != null) "TARGET" else "HDG",
                        color = WarningAmber
                    )
                    // DTW
                    CompactTelemetryPill(
                        label = "DTW",
                        value = if (dtwNM != null) String.format(Locale.US, "%.1f NM", dtwNM) else "--",
                        subValue = if (dtwNM != null) String.format(Locale.US, "%.1f km", dtwNM * 1.852) else "--",
                        color = if (dtwNM != null) WarningAmber else Color(0xFF64748B)
                    )
                    // ETA
                    if (isLandscape) {
                        CompactTelemetryPill(
                            label = "ETA",
                            value = etaStr,
                            subValue = "WIB",
                            color = SeafoamGreen
                        )
                    }
                }

                // Expand / Collapse Action
                if (!isLandscape) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Perluas HUD",
                            tint = OceanCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Full Portrait HUD with Tape Bar
        NavionicsNavigationTapeBar(
            user = user,
            sonarDepthMeters = sonarDepthMeters,
            navWaypoint = navWaypoint,
            navZppi = navZppi,
            selectedVessel = selectedVessel,
            selectedAtoN = selectedAtoN,
            selectedLighthouse = selectedLighthouse,
            onToggleCollapse = onToggleExpand
        )
    }
}

@Composable
private fun CompactTelemetryPill(
    label: String,
    value: String,
    subValue: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Text(value, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = color)
        }
        Text(subValue, fontSize = 7.5.sp, color = Color(0xFF64748B), maxLines = 1)
    }
}

@Composable
private fun NavionicsNavigationTapeBar(
    user: UserVesselState,
    sonarDepthMeters: Float,
    navWaypoint: FishNetWaypointEntity?,
    navZppi: ZppiFishingZone?,
    selectedVessel: Vessel?,
    selectedAtoN: NavigationalAid?,
    selectedLighthouse: Lighthouse?,
    modifier: Modifier = Modifier,
    onToggleCollapse: (() -> Unit)? = null
) {
    // Determine active target for Bearing (BRG) and Distance (DTW)
    val targetLat = navWaypoint?.latitude ?: navZppi?.latitude ?: selectedVessel?.latitude ?: selectedAtoN?.latitude ?: selectedLighthouse?.latitude
    val targetLon = navWaypoint?.longitude ?: navZppi?.longitude ?: selectedVessel?.longitude ?: selectedAtoN?.longitude ?: selectedLighthouse?.longitude

    val dtwNM = if (targetLat != null && targetLon != null) {
        MaritimeMath.calculateDistanceNM(user.latitude, user.longitude, targetLat, targetLon)
    } else null

    val brgDeg = if (targetLat != null && targetLon != null) {
        MaritimeMath.calculateBearingDeg(user.latitude, user.longitude, targetLat, targetLon)
    } else user.headingDeg

    val etaStr = if (dtwNM != null && user.currentSpeedKnots > 0.5) {
        val hours = dtwNM / user.currentSpeedKnots
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        "${h}j ${m}m"
    } else if (dtwNM != null) "--:--" else "SIAGA"

    val speedKmH = user.currentSpeedKnots * 1.852

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xFF0A192F).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, OceanCyan.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOG (Knot & km/jam)
            NavTapeItem(
                label = "SOG (SPEED)",
                value = String.format(Locale.US, "%.1f", user.currentSpeedKnots),
                unit = String.format(Locale.US, "KTS (%.1f km/j)", speedKmH),
                valueColor = OceanCyan
            )
            // COG / HDG
            NavTapeItem(
                label = "COG / HDG",
                value = String.format(Locale.US, "%03.0f°", user.headingDeg),
                unit = MaritimeMath.getCardinalDirection(user.headingDeg).substringBefore(" "),
                valueColor = Color.White
            )
            // DEPTH
            NavTapeItem(
                label = "DEPTH",
                value = String.format(Locale.US, "%.1f", sonarDepthMeters),
                unit = "METER",
                valueColor = if (sonarDepthMeters < 5.0f) WarningOrange else SeafoamGreen
            )
            // BRG (Sudut Haluan ke Target / Kompas)
            NavTapeItem(
                label = if (targetLat != null) "BRG (TARGET)" else "KOMPAS",
                value = String.format(Locale.US, "%03.0f°", brgDeg),
                unit = MaritimeMath.getCardinalDirection(brgDeg).substringBefore(" "),
                valueColor = WarningAmber
            )
            // DTW (Jarak ke Target dlm NM & km)
            NavTapeItem(
                label = "DTW (JARAK)",
                value = if (dtwNM != null) String.format(Locale.US, "%.1f", dtwNM) else "0.0",
                unit = if (dtwNM != null) String.format(Locale.US, "NM (%.1f km)", dtwNM * 1.852) else "NM",
                valueColor = if (dtwNM != null) WarningAmber else Color(0xFF64748B)
            )
            // ETA
            NavTapeItem(
                label = "ETA",
                value = etaStr,
                unit = "WIB",
                valueColor = SeafoamGreen
            )

            // Collapse Button (if provided)
            if (onToggleCollapse != null) {
                IconButton(onClick = onToggleCollapse, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Ciutkan HUD",
                        tint = OceanCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTapeItem(label: String, value: String, unit: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = valueColor)
        Text(text = unit, fontSize = 7.5.sp, color = Color(0xFF64748B), maxLines = 1)
    }
}

// =========================================================================
// SLIPPY TILE RENDERER & VECTOR DRAWING FUNCTIONS
// =========================================================================

private fun DrawScope.drawSlippyMapTiles(
    tileManager: NauticalTileManager,
    centerX: Float,
    centerY: Float,
    centerTileX: Double,
    centerTileY: Double,
    intZoom: Int,
    renderedTileSize: Float,
    canvasW: Float,
    canvasH: Float,
    layer: NavionicsChartLayer
) {
    val numTiles = 1 shl intZoom

    val minTileX = floor(centerTileX - (centerX / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)
    val maxTileX = ceil(centerTileX + ((canvasW - centerX) / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)
    val minTileY = floor(centerTileY - (centerY / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)
    val maxTileY = ceil(centerTileY + ((canvasH - centerY) / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)

    for (tx in minTileX..maxTileX) {
        for (ty in minTileY..maxTileY) {
            val tileScreenX = centerX + ((tx - centerTileX) * renderedTileSize).toFloat()
            val tileScreenY = centerY + ((ty - centerTileY) * renderedTileSize).toFloat()

            val tileBitmap = tileManager.getTile(tx, ty, intZoom, layer)
            if (tileBitmap != null) {
                drawImage(
                    image = tileBitmap,
                    dstOffset = IntOffset(tileScreenX.roundToInt(), tileScreenY.roundToInt()),
                    dstSize = IntSize(ceil(renderedTileSize).toInt() + 1, ceil(renderedTileSize).toInt() + 1)
                )
            } else {
                // Background oceanic placeholder while downloading
                drawRect(
                    color = if (layer == NavionicsChartLayer.SATELLITE_OVERLAY) Color(0xFF041829) else Color(0xFF0C2B45),
                    topLeft = Offset(tileScreenX, tileScreenY),
                    size = Size(renderedTileSize + 1f, renderedTileSize + 1f)
                )
                // Subtle grid border
                drawRect(
                    color = Color(0xFF1E3A5F).copy(alpha = 0.35f),
                    topLeft = Offset(tileScreenX, tileScreenY),
                    size = Size(renderedTileSize, renderedTileSize),
                    style = Stroke(width = 1f)
                )
            }
        }
    }
}

private fun DrawScope.drawNavionicsGridMercator(
    centerX: Float,
    centerY: Float,
    centerTileX: Double,
    centerTileY: Double,
    intZoom: Int,
    renderedTileSize: Float,
    canvasW: Float,
    canvasH: Float
) {
    val gridPaint = Paint().apply {
        color = android.graphics.Color.argb(130, 56, 189, 248)
        textSize = 9f * density
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        val numTiles = 1 shl intZoom
        val minTileX = floor(centerTileX - (centerX / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)
        val maxTileX = ceil(centerTileX + ((canvasW - centerX) / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)
        val minTileY = floor(centerTileY - (centerY / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)
        val maxTileY = ceil(centerTileY + ((canvasH - centerY) / renderedTileSize)).toInt().coerceIn(0, numTiles - 1)

        for (tx in minTileX..maxTileX) {
            val sx = centerX + ((tx - centerTileX) * renderedTileSize).toFloat()
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                start = Offset(sx, 0f),
                end = Offset(sx, canvasH),
                strokeWidth = 1f
            )
            val lon = NauticalTileManager.normalizedXToLon(tx.toDouble() / numTiles)
            val label = String.format(Locale.US, "%.2f°E", lon)
            canvas.nativeCanvas.drawText(label, sx + 4f, canvasH - 8f, gridPaint)
        }

        for (ty in minTileY..maxTileY) {
            val sy = centerY + ((ty - centerTileY) * renderedTileSize).toFloat()
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                start = Offset(0f, sy),
                end = Offset(canvasW, sy),
                strokeWidth = 1f
            )
            val lat = NauticalTileManager.normalizedYToLat(ty.toDouble() / numTiles)
            val label = String.format(Locale.US, "%.2f°S", abs(lat))
            canvas.nativeCanvas.drawText(label, 6f, sy - 4f, gridPaint)
        }
    }
}

private fun DrawScope.drawNavionicsHazardsAndTSS(
    geoToCanvas: (Double, Double) -> Offset,
    scale: Float
) {
    val magentaTss = Color(0xFFD946EF)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)

    // 1. TSS Separation Traffic Lane (Tanjung Priok / Merak Approach)
    val p1 = geoToCanvas(-5.96, 106.88)
    val p2 = geoToCanvas(-5.88, 106.88)
    val p3 = geoToCanvas(-5.96, 106.92)
    val p4 = geoToCanvas(-5.88, 106.92)

    drawLine(color = magentaTss, start = p1, end = p2, strokeWidth = 2.5f, pathEffect = dashEffect)
    drawLine(color = magentaTss, start = p3, end = p4, strokeWidth = 2.5f, pathEffect = dashEffect)

    // Center separation line
    val pMid1 = Offset((p1.x + p3.x) / 2f, (p1.y + p3.y) / 2f)
    val pMid2 = Offset((p2.x + p4.x) / 2f, (p2.y + p4.y) / 2f)
    drawLine(color = magentaTss.copy(alpha = 0.6f), start = pMid1, end = pMid2, strokeWidth = 4f)

    // 2. Submarine Cable (Kabel Bawah Laut)
    val c1 = geoToCanvas(-5.98, 106.75)
    val c2 = geoToCanvas(-5.92, 106.82)
    val c3 = geoToCanvas(-5.85, 106.86)
    val cablePath = Path().apply {
        moveTo(c1.x, c1.y)
        lineTo(c2.x, c2.y)
        lineTo(c3.x, c3.y)
    }
    drawPath(cablePath, magentaTss.copy(alpha = 0.75f), style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)))

    // 3. Sunken Shipwreck Hazard (⚓⚠️ Bangkai Kapal Karam)
    val wreckPos = geoToCanvas(-5.865, 106.81)
    drawCircle(color = DangerRed.copy(alpha = 0.25f), radius = 16f * scale.coerceIn(0.8f, 2.0f), center = wreckPos)
    drawCircle(color = DangerRed, radius = 6f * scale.coerceIn(0.8f, 2.0f), center = wreckPos)
    drawLine(color = DangerRed, start = Offset(wreckPos.x - 8f, wreckPos.y), end = Offset(wreckPos.x + 8f, wreckPos.y), strokeWidth = 2f)
    drawLine(color = DangerRed, start = Offset(wreckPos.x, wreckPos.y - 8f), end = Offset(wreckPos.x, wreckPos.y + 8f), strokeWidth = 2f)
}

private fun DrawScope.drawNavionicsSpotSoundings(
    geoToCanvas: (Double, Double) -> Offset,
    soundings: List<SpotSounding>,
    scale: Float
) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = android.graphics.Color.argb(220, 226, 232, 240)
            textSize = 10f * scale.coerceIn(0.8f, 1.6f) * density
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        }

        soundings.forEach { snd ->
            val pos = geoToCanvas(snd.latitude, snd.longitude)
            val text = String.format(Locale.US, "%.1f", snd.depthMeters)
            canvas.nativeCanvas.drawText(text, pos.x, pos.y, paint)
        }
    }
}

private fun DrawScope.drawNavionicsAtoN(
    geoToCanvas: (Double, Double) -> Offset,
    atons: List<NavigationalAid>,
    selectedAtoN: NavigationalAid?,
    scale: Float
) {
    val s = scale.coerceIn(0.8f, 2.2f)
    val textPaint = Paint().apply {
        color = android.graphics.Color.argb(235, 241, 245, 249)
        textSize = 9.5f * density
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }
    val bgPaint = Paint().apply {
        color = android.graphics.Color.argb(180, 10, 25, 47)
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        atons.forEach { aton ->
            val pos = geoToCanvas(aton.latitude, aton.longitude)
            val ax = pos.x
            val ay = pos.y
            val isSelected = aton.id == selectedAtoN?.id

            if (isSelected) {
                drawCircle(color = OceanCyan.copy(alpha = 0.35f), radius = 24f * s, center = Offset(ax, ay))
                drawCircle(color = OceanCyan, radius = 16f * s, center = Offset(ax, ay), style = Stroke(width = 2.5f))
            }

            when (aton.type) {
                BuoyType.PORT_HAND -> {
                    // Red Can Buoy
                    drawRect(
                        color = DangerRed,
                        topLeft = Offset(ax - 6f * s, ay - 9f * s),
                        size = Size(12f * s, 14f * s)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(ax - 6f * s, ay - 9f * s),
                        size = Size(12f * s, 14f * s),
                        style = Stroke(width = 1.2f)
                    )
                    drawLine(color = DangerRed, start = Offset(ax, ay - 9f * s), end = Offset(ax, ay - 15f * s), strokeWidth = 2.2f)
                }
                BuoyType.STARBOARD_HAND -> {
                    // Green Conical Buoy
                    val conePath = Path().apply {
                        moveTo(ax, ay - 15f * s)
                        lineTo(ax + 7f * s, ay + 5f * s)
                        lineTo(ax - 7f * s, ay + 5f * s)
                        close()
                    }
                    drawPath(conePath, SeafoamGreen)
                    drawPath(conePath, Color.White, style = Stroke(width = 1.2f))
                }
                BuoyType.NORTH_CARDINAL -> {
                    drawCircle(color = WarningAmber, radius = 7f * s, center = Offset(ax, ay))
                    drawCircle(color = Color.Black, radius = 3.5f * s, center = Offset(ax, ay))
                }
                BuoyType.SOUTH_CARDINAL -> {
                    drawCircle(color = Color.Black, radius = 7f * s, center = Offset(ax, ay))
                    drawCircle(color = WarningAmber, radius = 3.5f * s, center = Offset(ax, ay))
                }
                BuoyType.ISOLATED_DANGER -> {
                    drawCircle(color = DangerRed, radius = 7f * s, center = Offset(ax, ay))
                    drawCircle(color = Color.Black, radius = 2.5f * s, center = Offset(ax, ay - 9f * s))
                    drawCircle(color = Color.Black, radius = 2.5f * s, center = Offset(ax, ay - 14f * s))
                }
                BuoyType.SAFE_WATER -> {
                    drawCircle(color = DangerRed, radius = 7f * s, center = Offset(ax, ay))
                    drawCircle(color = Color.White, radius = 3.5f * s, center = Offset(ax, ay))
                }
                else -> {
                    drawCircle(color = WarningAmber, radius = 6f * s, center = Offset(ax, ay))
                }
            }

            // Draw Buoy Name Tag Label
            val shortLabel = aton.name
            val textWidth = textPaint.measureText(shortLabel)
            canvas.nativeCanvas.drawRoundRect(
                ax - textWidth / 2f - 5f,
                ay + 8f * s,
                ax + textWidth / 2f + 5f,
                ay + 8f * s + 16f,
                4f, 4f, bgPaint
            )
            canvas.nativeCanvas.drawText(
                shortLabel,
                ax - textWidth / 2f,
                ay + 8f * s + 12f,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawNavionicsLighthouses(
    geoToCanvas: (Double, Double) -> Offset,
    lights: List<Lighthouse>,
    selectedLight: Lighthouse?,
    scale: Float
) {
    val s = scale.coerceIn(0.8f, 2.2f)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 10f * density
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }
    val bgPaint = Paint().apply {
        color = android.graphics.Color.argb(190, 15, 23, 42)
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        lights.forEach { light ->
            val pos = geoToCanvas(light.latitude, light.longitude)
            val lx = pos.x
            val ly = pos.y
            val isSelected = light.id == selectedLight?.id

            // Light Beam Sector Arc (Glowing Yellow Halo)
            drawCircle(
                color = WarningAmber.copy(alpha = if (isSelected) 0.35f else 0.20f),
                radius = 38f * s,
                center = Offset(lx, ly)
            )
            drawCircle(
                color = WarningAmber.copy(alpha = 0.7f),
                radius = 24f * s,
                center = Offset(lx, ly),
                style = Stroke(width = if (isSelected) 2.2f else 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f))
            )

            // Lighthouse Tower
            val towerPath = Path().apply {
                moveTo(lx - 5f * s, ly + 9f * s)
                lineTo(lx + 5f * s, ly + 9f * s)
                lineTo(lx + 2.5f * s, ly - 9f * s)
                lineTo(lx - 2.5f * s, ly - 9f * s)
                close()
            }
            drawPath(towerPath, Color.White)
            drawPath(towerPath, Color.Black, style = Stroke(width = 1.5f))

            // Lantern Flare Top
            drawCircle(color = WarningAmber, radius = 5f * s, center = Offset(lx, ly - 11f * s))

            // Draw Lighthouse Label
            val textWidth = textPaint.measureText(light.name)
            canvas.nativeCanvas.drawRoundRect(
                lx - textWidth / 2f - 6f,
                ly + 12f * s,
                lx + textWidth / 2f + 6f,
                ly + 12f * s + 17f,
                5f, 5f, bgPaint
            )
            canvas.nativeCanvas.drawText(
                light.name,
                lx - textWidth / 2f,
                ly + 12f * s + 13f,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawZppiHotspots(
    geoToCanvas: (Double, Double) -> Offset,
    zppiList: List<ZppiFishingZone>,
    scale: Float
) {
    val s = scale.coerceIn(0.8f, 2.2f)
    zppiList.forEach { zppi ->
        val pos = geoToCanvas(zppi.latitude, zppi.longitude)
        drawCircle(color = SeafoamGreen.copy(alpha = 0.20f), radius = 32f * s, center = pos)
        drawCircle(color = OceanCyan.copy(alpha = 0.7f), radius = 20f * s, center = pos, style = Stroke(width = 1.5f))
        drawCircle(color = SeafoamGreen, radius = 7f * s, center = pos)
    }
}

private fun DrawScope.drawWaypoints(
    geoToCanvas: (Double, Double) -> Offset,
    waypoints: List<FishNetWaypointEntity>,
    scale: Float
) {
    val s = scale.coerceIn(0.8f, 2.2f)
    waypoints.forEach { wp ->
        val pos = geoToCanvas(wp.latitude, wp.longitude)
        val wpColor = when (wp.type) {
            "JARING_HANYUT" -> WarningAmber
            "RUMPON" -> SeafoamGreen
            "SPOT_IKAN" -> OceanCyan
            else -> DangerRed
        }

        drawCircle(color = wpColor.copy(alpha = 0.35f), radius = 18f * s, center = pos)
        drawCircle(color = wpColor, radius = 7f * s, center = pos)
        drawLine(color = Color.White, start = Offset(pos.x - 4f, pos.y), end = Offset(pos.x + 4f, pos.y), strokeWidth = 2f)
        drawLine(color = Color.White, start = Offset(pos.x, pos.y - 4f), end = Offset(pos.x, pos.y + 4f), strokeWidth = 2f)
    }
}

private fun DrawScope.drawNavigationHighway(
    geoToCanvas: (Double, Double) -> Offset,
    uiState: MaritimeUiState
) {
    val targetLat = uiState.selectedNavWaypoint?.latitude ?: uiState.selectedZppi?.latitude ?: uiState.selectedVessel?.latitude ?: return
    val targetLon = uiState.selectedNavWaypoint?.longitude ?: uiState.selectedZppi?.longitude ?: uiState.selectedVessel?.longitude ?: return

    val user = uiState.userVessel
    val uPos = geoToCanvas(user.latitude, user.longitude)
    val tPos = geoToCanvas(targetLat, targetLon)

    // Outer Glow Corridor
    drawLine(
        color = OceanCyan.copy(alpha = 0.35f),
        start = uPos,
        end = tPos,
        strokeWidth = 14f
    )
    // Dashed Highway
    drawLine(
        color = WarningAmber,
        start = uPos,
        end = tPos,
        strokeWidth = 3.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
    )

    // Target Destination Halo
    drawCircle(color = WarningAmber.copy(alpha = 0.3f), radius = 24f, center = tPos)
    drawCircle(color = WarningAmber, radius = 9f, center = tPos)
    drawCircle(color = Color.Black, radius = 4f, center = tPos)
}

private fun DrawScope.drawAisVessels(
    geoToCanvas: (Double, Double) -> Offset,
    vessels: List<Vessel>,
    selectedVessel: Vessel?,
    scale: Float
) {
    val s = scale.coerceIn(0.8f, 2.2f)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 10f * density
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }
    val bgPaint = Paint().apply {
        color = android.graphics.Color.argb(180, 15, 23, 42)
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        vessels.forEach { vessel ->
            val pos = geoToCanvas(vessel.latitude, vessel.longitude)
            val vx = pos.x
            val vy = pos.y
            val isSelected = vessel.id == selectedVessel?.id

            rotate(vessel.headingDeg, pivot = Offset(vx, vy)) {
                val shipPath = Path().apply {
                    moveTo(vx, vy - (if (isSelected) 18f else 14f) * s)
                    lineTo(vx + (if (isSelected) 9f else 7f) * s, vy + (if (isSelected) 12f else 9f) * s)
                    lineTo(vx, vy + (if (isSelected) 7f else 5f) * s)
                    lineTo(vx - (if (isSelected) 9f else 7f) * s, vy + (if (isSelected) 12f else 9f) * s)
                    close()
                }
                drawPath(shipPath, vessel.type.color)
                drawPath(shipPath, if (isSelected) WarningAmber else Color.White, style = Stroke(width = if (isSelected) 2.5f else 1.2f))
            }

            // Speed Vector Line
            val vectorLength = (vessel.speedKnots * 3.5f * s).toFloat()
            val rad = Math.toRadians(vessel.headingDeg.toDouble() - 90.0)
            val targetX = vx + (cos(rad) * vectorLength).toFloat()
            val targetY = vy + (sin(rad) * vectorLength).toFloat()
            drawLine(
                color = vessel.type.color.copy(alpha = 0.85f),
                start = Offset(vx, vy),
                end = Offset(targetX, targetY),
                strokeWidth = 2.0f
            )

            // Draw Vessel Name Badge Label
            val speedKm = vessel.speedKnots * 1.852
            val labelText = "${vessel.name} (${String.format(Locale.US, "%.1f", vessel.speedKnots)} kts / ${String.format(Locale.US, "%.0f", speedKm)} km/j)"
            val textWidth = textPaint.measureText(labelText)
            canvas.nativeCanvas.drawRoundRect(
                vx - textWidth / 2f - 6f,
                vy + 12f * s,
                vx + textWidth / 2f + 6f,
                vy + 12f * s + 18f,
                6f, 6f, bgPaint
            )
            canvas.nativeCanvas.drawText(
                labelText,
                vx - textWidth / 2f,
                vy + 12f * s + 13f,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawUserVessel(
    geoToCanvas: (Double, Double) -> Offset,
    user: UserVesselState,
    scale: Float
) {
    val s = scale.coerceIn(0.8f, 2.2f)
    val pos = geoToCanvas(user.latitude, user.longitude)
    val ux = pos.x
    val uy = pos.y

    // Radar Pulse Outer Ring
    drawCircle(color = OceanCyan.copy(alpha = 0.25f), radius = 34f * s, center = Offset(ux, uy))
    drawCircle(color = OceanCyan.copy(alpha = 0.7f), radius = 22f * s, center = Offset(ux, uy), style = Stroke(width = 2f))

    // Projected Course Line with 15-min vector ticks
    val projectedLen = (user.currentSpeedKnots * 8f * s).toFloat().coerceAtLeast(40f)
    val rad = Math.toRadians(user.headingDeg.toDouble() - 90.0)
    val pX = ux + (cos(rad) * projectedLen).toFloat()
    val pY = uy + (sin(rad) * projectedLen).toFloat()

    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(ux, uy),
        end = Offset(pX, pY),
        strokeWidth = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
    )
    drawCircle(color = Color(0xFF38BDF8), radius = 4f * s, center = Offset(pX, pY))

    // User Boat Icon
    rotate(user.headingDeg, pivot = Offset(ux, uy)) {
        val userShipPath = Path().apply {
            moveTo(ux, uy - 18f * s)
            lineTo(ux + 9f * s, uy + 11f * s)
            lineTo(ux, uy + 7f * s)
            lineTo(ux - 9f * s, uy + 11f * s)
            close()
        }
        drawPath(userShipPath, DangerRed)
        drawPath(userShipPath, Color.White, style = Stroke(width = 2f))
    }

    // Draw Own Vessel Name Badge Label (Nama Kapal Nelayan / Sendiri di Peta)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 10f * density
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }
    val bgPaint = Paint().apply {
        color = android.graphics.Color.argb(210, 15, 23, 42) // Dark Navy Glass
        isAntiAlias = true
    }
    val borderPaint = Paint().apply {
        color = android.graphics.Color.argb(240, 239, 68, 68) // DangerRed / Accent Border
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        val speedKm = user.currentSpeedKnots * 1.852
        val labelText = "🚩 ${user.name} (${String.format(Locale.US, "%.1f", user.currentSpeedKnots)} kts / ${String.format(Locale.US, "%.0f", speedKm)} km/j)"
        val textWidth = textPaint.measureText(labelText)
        val rectLeft = ux - textWidth / 2f - 7f
        val rectTop = uy + 14f * s
        val rectRight = ux + textWidth / 2f + 7f
        val rectBottom = uy + 14f * s + 19f

        canvas.nativeCanvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, 6f, 6f, bgPaint)
        canvas.nativeCanvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, 6f, 6f, borderPaint)
        canvas.nativeCanvas.drawText(labelText, ux - textWidth / 2f, uy + 14f * s + 14f, textPaint)
    }
}

private fun DrawScope.drawRulerTool(
    geoToCanvas: (Double, Double) -> Offset,
    start: Pair<Double, Double>?,
    end: Pair<Double, Double>?
) {
    if (start == null) return

    val sPos = geoToCanvas(start.first, start.second)
    drawCircle(color = WarningAmber, radius = 7f, center = sPos)

    if (end != null) {
        val ePos = geoToCanvas(end.first, end.second)
        drawCircle(color = WarningAmber, radius = 7f, center = ePos)
        drawLine(
            color = WarningAmber,
            start = sPos,
            end = ePos,
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawCompassRose(w: Float, h: Float, headingDeg: Float) {
    val roseX = w - 48f
    val roseY = 165f
    val radius = 24f

    // Outer Circle
    drawCircle(
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        radius = radius,
        center = Offset(roseX, roseY)
    )
    drawCircle(
        color = OceanCyan.copy(alpha = 0.7f),
        radius = radius,
        center = Offset(roseX, roseY),
        style = Stroke(width = 1.5f)
    )

    // Red North Arrow
    val nPath = Path().apply {
        moveTo(roseX, roseY - radius + 3f)
        lineTo(roseX + 5f, roseY)
        lineTo(roseX - 5f, roseY)
        close()
    }
    drawPath(nPath, DangerRed)

    // White South Arrow
    val sPath = Path().apply {
        moveTo(roseX, roseY + radius - 3f)
        lineTo(roseX + 5f, roseY)
        lineTo(roseX - 5f, roseY)
        close()
    }
    drawPath(sPath, Color.White)
}

// =========================================================================
// DETAIL BOTTOM CARDS (AtoN, Lighthouse, Vessel, ZPPI, Ruler)
// =========================================================================

@Composable
private fun NavigationalAidDetailCard(
    aton: NavigationalAid,
    userLat: Double,
    userLon: Double,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val distNM = MaritimeMath.calculateDistanceNM(userLat, userLon, aton.latitude, aton.longitude)
    val distKm = distNM * 1.852
    val bearing = MaritimeMath.calculateBearingDeg(userLat, userLon, aton.latitude, aton.longitude)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, OceanCyan),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = OceanCyan)
                    Column {
                        Text(aton.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text("Rambu Suar Navigasi (SBNP IALA Region A)", fontSize = 11.sp, color = OceanCyan)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VesselStatItem("TIPE RAMBU", aton.type.name.replace("_", " "))
                VesselStatItem("KILATAN SUAR", aton.lightFlash)
                VesselStatItem("JARAK DARI KAPAL", String.format(Locale.US, "%.1f NM (%.1f km)", distNM, distKm))
                VesselStatItem("HALUAN (BRG)", String.format(Locale.US, "%03.0f° %s", bearing, MaritimeMath.getCardinalDirection(bearing).substringBefore(" ")))
            }
        }
    }
}

@Composable
private fun LighthouseDetailCard(
    light: Lighthouse,
    userLat: Double,
    userLon: Double,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val distNM = MaritimeMath.calculateDistanceNM(userLat, userLon, light.latitude, light.longitude)
    val distKm = distNM * 1.852
    val bearing = MaritimeMath.calculateBearingDeg(userLat, userLon, light.latitude, light.longitude)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, WarningAmber),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WbIncandescent, contentDescription = null, tint = WarningAmber)
                    Column {
                        Text(light.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text("Mercusuar & Menara Suar Aktif Navigasi", fontSize = 11.sp, color = WarningAmber)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VesselStatItem("KARAKTER SUAR", light.lightCharacter)
                VesselStatItem("JARAK JANGKAU", "${light.rangeNM} NM")
                VesselStatItem("JARAK DARI KAPAL", String.format(Locale.US, "%.1f NM (%.1f km)", distNM, distKm))
                VesselStatItem("HALUAN (BRG)", String.format(Locale.US, "%03.0f°", bearing))
            }
        }
    }
}

@Composable
private fun VesselDetailBottomCard(
    vessel: Vessel,
    userLat: Double,
    userLon: Double,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val distNM = MaritimeMath.calculateDistanceNM(userLat, userLon, vessel.latitude, vessel.longitude)
    val distKm = distNM * 1.852
    val bearing = MaritimeMath.calculateBearingDeg(userLat, userLon, vessel.latitude, vessel.longitude)
    val speedKm = vessel.speedKnots * 1.852

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, vessel.type.color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(vessel.type.color))
                    Column {
                        Text(vessel.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text("${vessel.type.label} • MMSI: ${vessel.mmsi} • Call Sign: ${vessel.callSign}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VesselStatItem("KECEPATAN", String.format(Locale.US, "%.1f Kts (%.0f km/j)", vessel.speedKnots, speedKm))
                VesselStatItem("HALUAN", "${vessel.headingDeg.toInt()}° ${MaritimeMath.getCardinalDirection(vessel.headingDeg).substringBefore(" ")}")
                VesselStatItem("JARAK KITA", String.format(Locale.US, "%.1f NM (%.1f km)", distNM, distKm))
                VesselStatItem("BEARING", String.format(Locale.US, "%03.0f°", bearing))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VesselStatItem("STATUS", vessel.status)
                VesselStatItem("TUJUAN", vessel.destination)
            }
        }
    }
}

@Composable
private fun ZppiDetailBottomCard(
    zppi: ZppiFishingZone,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onNavigateToTarget: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, SeafoamGreen),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Waves, contentDescription = null, tint = SeafoamGreen)
                    Column {
                        Text(zppi.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text("Zona Potensi Penangkapan Ikan (${zppi.wppZone})", fontSize = 11.sp, color = SeafoamGreen)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VesselStatItem("TARGET IKAN", zppi.targetSpecies)
                VesselStatItem("SUHU PERMUKAAN", "${zppi.sstCelsius}°C")
                VesselStatItem("KLOROFIL-A", "${zppi.chlorophyllA} mg/m³")
                VesselStatItem("POTENSI", "${zppi.potentialScore}%")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onNavigateToTarget,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SeafoamGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Arahkan Haluan ke Spot Ikan Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RulerMeasurementBar(
    start: Pair<Double, Double>,
    end: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, WarningAmber),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = WarningAmber)
                    Text("PENGUKUR JARAK & HALUAN (RULER)", fontWeight = FontWeight.Black, fontSize = 12.5.sp, color = WarningAmber)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (end != null) {
                val distNM = MaritimeMath.calculateDistanceNM(start.first, start.second, end.first, end.second)
                val distKm = MaritimeMath.calculateDistanceKm(start.first, start.second, end.first, end.second)
                val bearing = MaritimeMath.calculateBearingDeg(start.first, start.second, end.first, end.second)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    VesselStatItem("JARAK LAUT", String.format(Locale.US, "%.2f NM", distNM))
                    VesselStatItem("JARAK KM", String.format(Locale.US, "%.2f km", distKm))
                    VesselStatItem("SUDUT HALUAN (BRG)", String.format(Locale.US, "%03.1f°", bearing))
                }
            } else {
                Text("Ketuk titik kedua di peta laut untuk mengukur jarak & haluan.", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun VesselStatItem(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
    }
}

// =========================================================================
// MODAL DIALOGS
// =========================================================================

@Composable
private fun NavionicsLayerDialog(
    currentLayer: NavionicsChartLayer,
    onSelectLayer: (NavionicsChartLayer) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = OceanCyan)
                Text("PILIH LAYER PETA LAUT", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NavionicsChartLayer.values().forEach { layer ->
                    val isSelected = layer == currentLayer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLayer(layer) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) OceanCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, OceanCyan) else null
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RadioButton(selected = isSelected, onClick = { onSelectLayer(layer) })
                            Column {
                                Text(layer.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) OceanCyan else MaterialTheme.colorScheme.onSurface)
                                Text(layer.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun SafetyDepthDialog(
    currentDepth: Float,
    onSelectDepth: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val depthOptions = listOf(2.0f, 3.0f, 5.0f, 10.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SeafoamGreen)
                Text("SAFETY CONTOUR (DRAFT KAPAL)", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih batas sarat air (draft) kapal Anda untuk menyorot zona dangkal berbahaya:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                depthOptions.forEach { depth ->
                    val isSelected = currentDepth == depth
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDepth(depth) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SeafoamGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SeafoamGreen) else null
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RadioButton(selected = isSelected, onClick = { onSelectDepth(depth) })
                            Text("Batas Aman Kedalaman: ${depth.toInt()} Meter", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun LocationPickerDialog(
    onSelectLocation: (Double, Double, String) -> Unit,
    onSyncRealGps: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Wilayah Perairan Indonesia", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSyncRealGps() },
                    shape = RoundedCornerShape(8.dp),
                    color = SeafoamGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SeafoamGreen)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SeafoamGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("📍 GUNAKAN GPS ASLI HP SAAT INI", fontWeight = FontWeight.Black, fontSize = 12.5.sp, color = SeafoamGreen)
                            Text("Otomatis kunci koordinat nyata & zona WPP posisi Anda", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text("ATAU PILIH SIMULASI ZONA LAUT:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                val sampleLocations = listOf(
                    Triple("Pelabuhan Patimban (Subang)", -6.2300, 107.9050),
                    Triple("PLTU Indramayu & Sukra - Eretan", -6.2950, 108.0100),
                    Triple("Kilang & Pelabuhan Balongan Indramayu", -6.3400, 108.3800),
                    Triple("Teluk Jakarta & Kepulauan Seribu", -5.9320, 106.8380),
                    Triple("Selat Sunda (Merak - Bakauheni)", -5.8800, 105.8500),
                    Triple("Laut Jawa (Cirebon - Semarang)", -6.2000, 109.1000),
                    Triple("Laut Natuna Utara", 3.9000, 108.3000),
                    Triple("Selat Bali & Banyuwangi", -8.2500, 114.4500),
                    Triple("Selat Makassar & Balikpapan", -1.4000, 117.2000)
                )
                sampleLocations.forEach { (name, lat, lon) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLocation(lat, lon, name) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Waves, contentDescription = null, tint = OceanCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(MaritimeMath.formatMaritimeCoord(lat, lon), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun FishFinderSonarDialog(
    depthMeters: Float,
    waterTempC: Float,
    fishDetections: List<SonarFishDetection>,
    onDismiss: () -> Unit,
    onMarkWaypoint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = OceanCyan)
                Column {
                    Text("ECHOSOUNDER & FISH FINDER", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Frekuensi 200 kHz • CHIRP High-Wide Sonar", style = MaterialTheme.typography.labelSmall, color = OceanCyan)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("KEDALAMAN AIR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", depthMeters)} m", fontWeight = FontWeight.Black, fontSize = 18.sp, color = OceanCyan)
                        }
                        Column {
                            Text("SUHU AIR (SST)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", waterTempC)} °C", fontWeight = FontWeight.Black, fontSize = 18.sp, color = SeafoamGreen)
                        }
                        Column {
                            Text("TARGET IKAN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${fishDetections.size} Echo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = WarningAmber)
                        }
                    }
                }

                // Visual Acoustic Column
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F3E5D),
                                    Color(0xFF0A263D),
                                    Color(0xFF04121E)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0m (Permukaan)", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Text("10m", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Text("20m (Termoklin)", fontSize = 8.sp, color = OceanCyan.copy(alpha = 0.8f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Text("30m", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Text("Dasar Laut (${depthMeters.toInt()}m)", fontSize = 8.sp, color = WarningAmber, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }

                    // Render Fish Arches
                    fishDetections.forEach { fish ->
                        val topOffsetFraction = (fish.depthMeters / 45f).coerceIn(0.1f, 0.85f)
                        val startOffsetFraction = fish.xOffsetPercent.coerceIn(0.15f, 0.85f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = (180 * topOffsetFraction).dp - 12.dp,
                                    start = (240 * startOffsetFraction).dp
                                )
                        ) {
                            Surface(
                                color = if (fish.fishSize == "BESAR") DangerRed.copy(alpha = 0.85f) else if (fish.fishSize == "SEDANG") WarningAmber.copy(alpha = 0.85f) else SeafoamGreen.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp),
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Waves, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    Text(
                                        text = "${fish.depthMeters}m (${fish.estimatedWeightKg}kg)",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onMarkWaypoint,
                colors = ButtonDefaults.buttonColors(containerColor = SeafoamGreen, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tandai Titik Ikan", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun NmeaTelemetryDialog(
    userLat: Double,
    userLon: Double,
    speedKnots: Double,
    headingDeg: Float,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mockSentences = remember(userLat, userLon, speedKnots, headingDeg) {
        com.example.data.util.Nmea0183Helper.generateMockNmeaStream(userLat, userLon, speedKnots, headingDeg)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = SeafoamGreen)
                Column {
                    Text("NMEA 0183 & AIS TELEMETRY", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Serial Receiver • 38400 Baud", style = MaterialTheme.typography.labelSmall, color = SeafoamGreen)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = SeafoamGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SeafoamGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("STATUS GPS: 3D DGPS FIX", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SeafoamGreen)
                            Text("Satelit: 9 (GPS/GLONASS)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("HDOP: 1.1", fontWeight = FontWeight.Black, fontSize = 12.sp, color = SeafoamGreen)
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mockSentences.forEach { sentence ->
                            Text(
                                text = sentence,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (sentence.startsWith("!AIVDM")) WarningAmber else OceanCyan,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAll = mockSentences.joinToString("\n")
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Raw NMEA", rawAll)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "NMEA disalin!", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanCyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salin Raw NMEA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

// =========================================================================
// DRAWING HELPERS: BREADCRUMBS, ANCHOR WATCH, CPA COLLISION RISKS
// =========================================================================

private fun DrawScope.drawBreadcrumbTracks(
    geoToCanvas: (Double, Double) -> Offset,
    tracks: List<BreadcrumbTrackPoint>,
    scale: Float,
    isNightVision: Boolean
) {
    if (tracks.isEmpty()) return
    val s = scale.coerceIn(0.8f, 2.2f)
    val trackColor = if (isNightVision) DangerRed else OceanCyan
    val glowColor = trackColor.copy(alpha = 0.35f)

    if (tracks.size >= 2) {
        val path = Path()
        val first = geoToCanvas(tracks.first().latitude, tracks.first().longitude)
        path.moveTo(first.x, first.y)
        for (i in 1 until tracks.size) {
            val pt = geoToCanvas(tracks[i].latitude, tracks[i].longitude)
            path.lineTo(pt.x, pt.y)
        }
        drawPath(path, color = glowColor, style = Stroke(width = 6f * s))
        drawPath(path, color = trackColor, style = Stroke(width = 2.5f * s))
    }

    tracks.forEachIndexed { index, pt ->
        if (index % 3 == 0 || index == tracks.lastIndex) {
            val pos = geoToCanvas(pt.latitude, pt.longitude)
            drawCircle(color = trackColor, radius = 3f * s, center = pos)
        }
    }
}

private fun DrawScope.drawAnchorWatchZone(
    geoToCanvas: (Double, Double) -> Offset,
    anchor: AnchorWatchState,
    numTiles: Int,
    renderedTileSize: Float,
    scale: Float
) {
    if (!anchor.isActive) return
    val anchorPos = geoToCanvas(anchor.anchorLat, anchor.anchorLon)

    val latRad = Math.toRadians(anchor.anchorLat)
    val metersPerTile = (40075016.686 * cos(latRad)) / numTiles
    val pixelsPerMeter = (renderedTileSize / metersPerTile).toFloat()
    val radiusPx = (anchor.radiusMeters * pixelsPerMeter).coerceAtLeast(15f)

    val ringColor = if (anchor.isAlarmTriggered) DangerRed else SeafoamGreen
    val fillColor = ringColor.copy(alpha = if (anchor.isAlarmTriggered) 0.25f else 0.12f)

    drawCircle(color = fillColor, radius = radiusPx, center = anchorPos)
    drawCircle(
        color = ringColor,
        radius = radiusPx,
        center = anchorPos,
        style = Stroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    )

    drawCircle(color = ringColor, radius = 6f, center = anchorPos)
    drawCircle(color = Color.Black, radius = 3f, center = anchorPos)
}

private fun DrawScope.drawCpaCollisionRisks(
    geoToCanvas: (Double, Double) -> Offset,
    dangerousTargets: List<CpaCalculationResult>,
    scale: Float
) {
    if (dangerousTargets.isEmpty()) return
    val s = scale.coerceIn(0.8f, 2.2f)

    dangerousTargets.forEach { target ->
        val vPos = geoToCanvas(target.vessel.latitude, target.vessel.longitude)
        drawCircle(color = DangerRed.copy(alpha = 0.35f), radius = 28f * s, center = vPos)
        drawCircle(
            color = DangerRed,
            radius = 20f * s,
            center = vPos,
            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f))
        )
    }
}

// =========================================================================
// ALERT BANNERS: CPA COLLISION & ANCHOR DRAG
// =========================================================================

@Composable
private fun CpaCollisionWarningBanner(
    danger: CpaCalculationResult,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = DangerRed.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Bahaya Tabrakan",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "BAHAYA TABRAKAN! (CPA ALARM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "${danger.vessel.name} • CPA: ${String.format(Locale.US, "%.2f", danger.cpaDistanceNM)} NM • TCPA: ${String.format(Locale.US, "%.1f", danger.tcpaMinutes)} min",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
            }
        }
    }
}

@Composable
private fun AnchorDragAlertBanner(
    anchor: AnchorWatchState,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = DangerRed.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Anchor,
                    contentDescription = "Jangkar Hanyut",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "PERINGATAN: JANGKAR HANYUT!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Kapal bergeser ${String.format(Locale.US, "%.0f", anchor.currentDistanceMeters)}m (Batas: ${String.format(Locale.US, "%.0f", anchor.radiusMeters)}m)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
            }
        }
    }
}

// =========================================================================
// MODAL DIALOGS: ANCHOR WATCH, TRACK RECORDER, TIDE & CURRENT
// =========================================================================

@Composable
private fun AnchorWatchDialog(
    anchorState: AnchorWatchState,
    onStartAnchorWatch: (Float) -> Unit,
    onStopAnchorWatch: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRadius by remember { mutableFloatStateOf(anchorState.radiusMeters.takeIf { it > 0f } ?: 50f) }
    val radii = listOf(30f, 50f, 100f, 150f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Anchor, contentDescription = null, tint = SeafoamGreen)
                Column {
                    Text("ALARM LEGO JANGKAR (ANCHOR WATCH)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Pengawasan Hanyut & Batas Aman Lingkaran", style = MaterialTheme.typography.labelSmall, color = SeafoamGreen)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = if (anchorState.isActive) {
                        if (anchorState.isAlarmTriggered) DangerRed.copy(alpha = 0.15f) else SeafoamGreen.copy(alpha = 0.12f)
                    } else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (anchorState.isActive) (if (anchorState.isAlarmTriggered) DangerRed else SeafoamGreen) else Color(0xFF334155)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("STATUS JANGKAR:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (anchorState.isActive) {
                                    if (anchorState.isAlarmTriggered) "⚠️ HANYUT / DANGER" else "AKTIF MENGAWASI"
                                } else "NONAKTIF",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (anchorState.isActive) (if (anchorState.isAlarmTriggered) DangerRed else SeafoamGreen) else Color.Gray
                            )
                        }
                        if (anchorState.isActive) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("JARAK HANYUT:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${String.format(Locale.US, "%.1f", anchorState.currentDistanceMeters)} m / ${String.format(Locale.US, "%.0f", anchorState.radiusMeters)} m",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (anchorState.isAlarmTriggered) DangerRed else Color.White
                                )
                            }
                        }
                    }
                }

                Text("PILIH RADIUS BATAS AMAN JANGKAR:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    radii.forEach { r ->
                        FilterChip(
                            selected = selectedRadius == r,
                            onClick = { selectedRadius = r },
                            label = { Text("${r.toInt()}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SeafoamGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (anchorState.isActive) {
                Button(
                    onClick = {
                        onStopAnchorWatch()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Angkat Jangkar (Stop)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = {
                        onStartAnchorWatch(selectedRadius)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeafoamGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Anchor, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lego Jangkar (${selectedRadius.toInt()}m)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun TrackRecorderDialog(
    isRecording: Boolean,
    tracks: List<BreadcrumbTrackPoint>,
    onToggleRecording: () -> Unit,
    onClearTracks: () -> Unit,
    onBacktrack: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalDistanceNM = remember(tracks) {
        if (tracks.size < 2) 0.0
        else {
            var dist = 0.0
            for (i in 0 until tracks.size - 1) {
                dist += MaritimeMath.calculateDistanceNM(
                    tracks[i].latitude, tracks[i].longitude,
                    tracks[i + 1].latitude, tracks[i + 1].longitude
                )
            }
            dist
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Route, contentDescription = null, tint = OceanCyan)
                Column {
                    Text("PEREKAM JEJAK RUTE (TRACK RECORDER)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Breadcrumb Log & Fitur Navigasi Balik", style = MaterialTheme.typography.labelSmall, color = OceanCyan)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STATUS PEREKAM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (isRecording) "🔴 SEDANG MEREKAM" else "PAUSED",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (isRecording) DangerRed else WarningAmber
                            )
                        }
                        Column {
                            Text("TITIK JEJAK", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${tracks.size} Pts", fontWeight = FontWeight.Black, fontSize = 14.sp, color = OceanCyan)
                        }
                        Column {
                            Text("TOTAL JARAK", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.2f", totalDistanceNM)} NM", fontWeight = FontWeight.Black, fontSize = 14.sp, color = SeafoamGreen)
                        }
                    }
                }

                if (tracks.isNotEmpty()) {
                    Button(
                        onClick = {
                            onBacktrack()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mulai Navigasi Balik (Backtrack)", fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tracks.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearTracks,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Text("Hapus Jejak", fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) WarningAmber else SeafoamGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(if (isRecording) Icons.Default.Pause else Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Pause" else "Rekam", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun TideCurrentDialog(
    tide: TideCurrentPrediction,
    locationName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Water, contentDescription = null, tint = OceanCyan)
                Column {
                    Text("PREDIKSI PASANG SURUT & ARUS", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Lokasi: $locationName", style = MaterialTheme.typography.labelSmall, color = OceanCyan)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TINGGI PASANG SURUT", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${if (tide.tideHeightMeters >= 0) "+" else ""}${String.format(Locale.US, "%.2f", tide.tideHeightMeters)} m",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = OceanCyan
                            )
                            Text(tide.currentTideState, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SeafoamGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("VEKTOR ARUS LAUT", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${String.format(Locale.US, "%.1f", tide.currentSpeedKnots)} kts",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = WarningAmber
                            )
                            Text("Arah: ${String.format(Locale.US, "%03.0f°", tide.currentDirectionDeg)}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("JADWAL PASANG SURUT HARI INI:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pasang Tertinggi (HW):", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                            Text(tide.nextHighTideTime, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = OceanCyan)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Surut Terendah (LW):", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                            Text(tide.nextLowTideTime, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun EditVesselProfileDialog(
    userVessel: UserVesselState,
    onSaveProfile: (name: String, regNo: String, captain: String, gt: Int, crew: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(userVessel.name) }
    var regNo by remember { mutableStateOf(userVessel.registrationNo) }
    var captain by remember { mutableStateOf(userVessel.captainName) }
    var grossTonnage by remember { mutableStateOf(userVessel.grossTonnage.toString()) }
    var crewCount by remember { mutableStateOf(userVessel.crewCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DirectionsBoat, contentDescription = null, tint = WarningAmber)
                Column {
                    Text("NAMA & IDENTITAS KAPAL", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Nama ini akan tampil di titik koordinat kapal Anda di peta", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kapal Anda (misal: KM. BARUNA JAYA)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = regNo,
                    onValueChange = { regNo = it },
                    label = { Text("No. Tanda Selar / Registrasi (misal: ID-SUB-2024)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = captain,
                    onValueChange = { captain = it },
                    label = { Text("Nama Nahkoda / Pemilik Kapal", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grossTonnage,
                        onValueChange = { if (it.all { char -> char.isDigit() }) grossTonnage = it },
                        label = { Text("Gross Tonnage (GT)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = crewCount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) crewCount = it },
                        label = { Text("Jumlah ABK", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(
                        name,
                        regNo,
                        captain,
                        grossTonnage.toIntOrNull() ?: 15,
                        crewCount.toIntOrNull() ?: 4
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SeafoamGreen, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simpan Profil Kapal", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}


