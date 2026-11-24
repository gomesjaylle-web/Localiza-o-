package com.example.util

import com.example.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

data class TilePosition(
    val zoom: Int,
    val xTile: Int,
    val yTile: Int,
    val fracX: Float,
    val fracY: Float
) {
    fun osmUrl(): String = "https://tile.openstreetmap.org/$zoom/$xTile/$yTile.png"
}

object MapTileHelper {
    fun latLngToTile(point: GeoPoint, zoom: Int): TilePosition {
        val clampedLat = point.latitude.coerceIn(-85.0511, 85.0511)
        val n = 2.0.pow(zoom)
        val xExact = (point.longitude + 180.0) / 360.0 * n
        val latRad = Math.toRadians(clampedLat)
        val yExact = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

        val maxTile = n.toInt() - 1
        val xTile = xExact.toInt().coerceIn(0, maxTile)
        val yTile = yExact.toInt().coerceIn(0, maxTile)
        val fracX = (xExact - xTile).toFloat().coerceIn(0f, 1f)
        val fracY = (yExact - yTile).toFloat().coerceIn(0f, 1f)
        return TilePosition(zoom, xTile, yTile, fracX, fracY)
    }

    fun tilePixelOffsetToLatLng(
        center: GeoPoint,
        zoom: Int,
        deltaXPx: Float,
        deltaYPx: Float,
        tileSizePx: Float = 256f
    ): GeoPoint {
        val n = 2.0.pow(zoom)
        val clampedLat = center.latitude.coerceIn(-85.0511, 85.0511)
        val xExact = (center.longitude + 180.0) / 360.0 * n
        val latRad = Math.toRadians(clampedLat)
        val yExact = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

        val newX = xExact + (deltaXPx / tileSizePx)
        val newY = (yExact + (deltaYPx / tileSizePx)).coerceIn(0.001, n - 0.001)

        val lon = (newX / n * 360.0 - 180.0).coerceIn(-180.0, 180.0)
        val nLat = PI - 2.0 * PI * newY / n
        val lat = Math.toDegrees(atan(sinh(nLat))).coerceIn(-85.0, 85.0)
        return GeoPoint(lat, lon)
    }
}
