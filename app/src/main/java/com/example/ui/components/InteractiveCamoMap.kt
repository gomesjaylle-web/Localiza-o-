package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CamoSessionState
import com.example.model.GeoPoint
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldSignal
import com.example.util.MapTileHelper
import java.util.Locale
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun InteractiveCamoMap(
    sessionState: CamoSessionState,
    isFavorite: Boolean,
    onMapCoordinateSelected: (GeoPoint) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var zoomLevel by remember { mutableIntStateOf(15) }
    var viewportCenter by remember(sessionState.selectedPlace.id) {
        mutableStateOf(sessionState.currentCoordinate)
    }

    // Keep viewport synced if user picks a preset or search result
    var lastOrigin by remember { mutableStateOf(sessionState.selectedPlace.coordinates) }
    if (lastOrigin != sessionState.selectedPlace.coordinates) {
        lastOrigin = sessionState.selectedPlace.coordinates
        viewportCenter = sessionState.selectedPlace.coordinates
    }

    val tileImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .header("User-Agent", "GeoCamoAndroid/1.0")
                            .build()
                        chain.proceed(req)
                    }
                    .build()
            }
            .build()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    val tilePos = remember(viewportCenter, zoomLevel) {
        MapTileHelper.latLngToTile(viewportCenter, zoomLevel)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFE7ECE9),
        tonalElevation = 2.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val tilePx = 512f

            // Layer 1: Procedural Vector Cartography Base (instant offline & loading underlay)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color(0xFFECEAE3))
                val gridStep = 72f
                val offsetX = ((-tilePos.fracX * tilePx) % gridStep)
                val offsetY = ((-tilePos.fracY * tilePx) % gridStep)

                // Park blocks
                drawRoundRect(
                    color = Color(0xFFCDE7C4),
                    topLeft = Offset(size.width * 0.1f + offsetX * 0.4f, size.height * 0.15f + offsetY * 0.4f),
                    size = Size(size.width * 0.28f, size.height * 0.22f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                drawRoundRect(
                    color = Color(0xFFCDE7C4),
                    topLeft = Offset(size.width * 0.62f + offsetX * 0.4f, size.height * 0.6f + offsetY * 0.4f),
                    size = Size(size.width * 0.26f, size.height * 0.24f),
                    cornerRadius = CornerRadius(24f, 24f)
                )

                // City street grid
                var x = offsetX
                while (x < size.width) {
                    drawLine(
                        color = Color(0xFFD6D2C9),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 6f
                    )
                    x += gridStep
                }
                var y = offsetY
                while (y < size.height) {
                    drawLine(
                        color = Color(0xFFD6D2C9),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 6f
                    )
                    y += gridStep
                }
            }

            // Layer 2: Real OpenStreetMap Tile (centered tile)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(tilePos.osmUrl())
                    .crossfade(true)
                    .build(),
                imageLoader = tileImageLoader,
                contentDescription = "Mapa da localização camuflada",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 3: Subtle tactical vignette & interactive touch layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x14091116))
                    .pointerInput(zoomLevel, widthPx, heightPx) {
                        detectTapGestures { tapOffset ->
                            val dx = tapOffset.x - widthPx / 2f
                            val dy = tapOffset.y - heightPx / 2f
                            val tappedCoord = MapTileHelper.tilePixelOffsetToLatLng(
                                center = viewportCenter,
                                zoom = zoomLevel,
                                deltaXPx = dx,
                                deltaYPx = dy,
                                tileSizePx = tilePx
                            )
                            viewportCenter = tappedCoord
                            onMapCoordinateSelected(tappedCoord)
                        }
                    }
                    .pointerInput(zoomLevel) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                viewportCenter = MapTileHelper.tilePixelOffsetToLatLng(
                                    center = viewportCenter,
                                    zoom = zoomLevel,
                                    deltaXPx = -dragAmount.x,
                                    deltaYPx = -dragAmount.y,
                                    tileSizePx = tilePx
                                )
                            },
                            onDragEnd = {
                                onMapCoordinateSelected(viewportCenter)
                            }
                        )
                    }
            )

            // Layer 4: Trail, Radar Pulse, and Live Camouflage Pin
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = 2.0.pow(zoomLevel)
                fun geoToScreen(pt: GeoPoint): Offset {
                    val centerTile = MapTileHelper.latLngToTile(viewportCenter, zoomLevel)
                    val ptTile = MapTileHelper.latLngToTile(pt, zoomLevel)
                    val dxTiles = (ptTile.xTile + ptTile.fracX) - (centerTile.xTile + centerTile.fracX)
                    val dyTiles = (ptTile.yTile + ptTile.fracY) - (centerTile.yTile + centerTile.fracY)
                    return Offset(
                        x = size.width / 2f + dxTiles * tilePx,
                        y = size.height / 2f + dyTiles * tilePx
                    )
                }

                // Trail polyline
                val trail = sessionState.trailHistory
                if (trail.size >= 2) {
                    val path = Path()
                    trail.forEachIndexed { index, point ->
                        val pos = geoToScreen(point)
                        if (index == 0) path.moveTo(pos.x, pos.y) else path.lineTo(pos.x, pos.y)
                    }
                    drawPath(
                        path = path,
                        color = EmeraldSignal.copy(alpha = 0.7f),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }

                // Active simulated dot position
                val pinScreen = geoToScreen(sessionState.currentCoordinate)

                if (sessionState.isCamouflaging) {
                    val maxRadius = 110f
                    val radius = 20f + pulseProgress * maxRadius
                    val alpha = (1f - pulseProgress) * 0.45f
                    drawCircle(
                        color = EmeraldGlow.copy(alpha = alpha),
                        radius = radius,
                        center = pinScreen
                    )
                    drawCircle(
                        color = EmeraldSignal.copy(alpha = alpha * 0.9f),
                        radius = radius,
                        center = pinScreen,
                        style = Stroke(width = 3f)
                    )
                }

                // Target marker outer glow
                drawCircle(
                    color = EmeraldSignal.copy(alpha = 0.25f),
                    radius = 34f,
                    center = pinScreen
                )
                // White ring
                drawCircle(
                    color = Color.White,
                    radius = 18f,
                    center = pinScreen
                )
                // Core dot
                drawCircle(
                    color = if (sessionState.isCamouflaging) EmeraldGlow else Color(0xFF008767),
                    radius = 12f,
                    center = pinScreen
                )

                // Direction cone when moving
                if (sessionState.isCamouflaging && sessionState.speedKmh > 1.5) {
                    val rad = Math.toRadians(sessionState.headingDegrees.toDouble() - 90.0)
                    val tip = Offset(
                        pinScreen.x + (cos(rad) * 34f).toFloat(),
                        pinScreen.y + (sin(rad) * 34f).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF004D3B),
                        start = pinScreen,
                        end = tip,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Top-right Hint pill
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 3.dp
            ) {
                Text(
                    text = "Toque ou arraste o mapa para posicionar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Right-side map controls (Favorite, Zoom+, Zoom-, Recenter)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_favorite_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Salvar local nos favoritos",
                        tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalIconButton(
                    onClick = { if (zoomLevel < 18) zoomLevel++ },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_in_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aproximar zoom")
                }

                FilledTonalIconButton(
                    onClick = { if (zoomLevel > 4) zoomLevel-- },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_out_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    )
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Afastar zoom")
                }

                FilledTonalIconButton(
                    onClick = { viewportCenter = sessionState.currentCoordinate },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_recenter_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    )
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Centralizar no pino camuflado",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bottom telemetry HUD pill
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xE6091116)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (sessionState.isCamouflaging) EmeraldGlow else Color(0xFF8696A0))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sessionState.currentCoordinate.formatted(5),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f km/h", sessionState.speedKmh),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSignal
                    )
                }
            }
        }
    }
}
