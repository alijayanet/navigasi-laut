package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.NavionicsChartLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * High-performance Slippy Map Tile Manager for Maritime Navigation.
 * Supports ESRI World Imagery (High-Resolution Satellite), OpenStreetMap Marine,
 * with Multi-tier caching (Memory LRU + Disk Cache + Async Network fetching).
 */
class NauticalTileManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // 64 MB Memory Cache for Decoded Bitmaps
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceIn(16 * 1024, 64 * 1024)

    private val memoryCache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    private val activeRequests = ConcurrentHashMap.newKeySet<String>()
    private val diskCacheDir = File(context.cacheDir, "maritime_tiles").apply { if (!exists()) mkdirs() }

    // Revision trigger for Jetpack Compose Canvas recomposition
    private val _tileRevision = mutableIntStateOf(0)
    val tileRevision: State<Int> get() = _tileRevision

    fun getTile(x: Int, y: Int, z: Int, layer: NavionicsChartLayer): ImageBitmap? {
        val maxCoord = 1 shl z
        if (x < 0 || x >= maxCoord || y < 0 || y >= maxCoord) return null

        val cacheKey = "${layer.name}_${z}_${x}_${y}"

        // 1. Check Memory Cache
        memoryCache.get(cacheKey)?.let { return it }

        // 2. Queue Asynchronous Load (Disk / Network)
        if (activeRequests.add(cacheKey)) {
            scope.launch {
                try {
                    val bitmap = loadFromDiskOrNetwork(x, y, z, layer, cacheKey)
                    if (bitmap != null) {
                        val imageBitmap = bitmap.asImageBitmap()
                        memoryCache.put(cacheKey, imageBitmap)
                        _tileRevision.intValue += 1
                    }
                } catch (_: Exception) {
                    // Ignore network failure, fallback gracefully
                } finally {
                    activeRequests.remove(cacheKey)
                }
            }
        }

        return null
    }

    private fun loadFromDiskOrNetwork(
        x: Int,
        y: Int,
        z: Int,
        layer: NavionicsChartLayer,
        cacheKey: String
    ): Bitmap? {
        val diskFile = File(diskCacheDir, "$cacheKey.png")
        if (diskFile.exists() && diskFile.length() > 0) {
            return BitmapFactory.decodeFile(diskFile.absolutePath)
        }

        val url = getTileUrl(x, y, z, layer) ?: return null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NusantaraBahari-MarineApp/1.0 (Android; Maritime Navigation)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: return null
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    try {
                        FileOutputStream(diskFile).use { out ->
                            out.write(bytes)
                        }
                    } catch (_: Exception) {}
                    return bitmap
                }
            }
        }
        return null
    }

    private fun getTileUrl(x: Int, y: Int, z: Int, layer: NavionicsChartLayer): String? {
        return when (layer) {
            NavionicsChartLayer.SATELLITE_OVERLAY -> {
                // ESRI World Imagery (High-Resolution Satellite HD)
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
            }
            NavionicsChartLayer.NAUTICAL_ENC -> {
                // OpenStreetMap Standard Nautical Base
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            }
            NavionicsChartLayer.SONARCHART_HD -> {
                // CartoDB Voyager High-Contrast Coastal Hydrography
                "https://basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png"
            }
            NavionicsChartLayer.CURRENTS_WEATHER -> {
                // ESRI Ocean Base (Marine currents and ocean floor topography)
                "https://server.arcgisonline.com/ArcGIS/rest/services/Ocean/World_Ocean_Base/MapServer/tile/$z/$y/$x"
            }
        }
    }

    companion object {
        const val TILE_SIZE = 256

        fun lonToNormalizedX(lon: Double): Double {
            return (lon + 180.0) / 360.0
        }

        fun latToNormalizedY(lat: Double): Double {
            val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
            val sinLat = sin(Math.toRadians(clampedLat))
            return 0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI)
        }

        fun normalizedXToLon(normX: Double): Double {
            return normX * 360.0 - 180.0
        }

        fun normalizedYToLat(normY: Double): Double {
            val y2 = (0.5 - normY) * 2.0 * Math.PI
            return Math.toDegrees(atan(sinh(y2)))
        }

        fun calculateZoom(scale: Float): Int {
            // Scale 1.0f corresponds to zoom level 10 (regional harbor view)
            // Zoom range: 4 (entire Indonesia) to 16 (close harbor & jetty view)
            val zoom = (10.0 + log2(scale.toDouble())).toInt()
            return zoom.coerceIn(4, 16)
        }
    }
}
