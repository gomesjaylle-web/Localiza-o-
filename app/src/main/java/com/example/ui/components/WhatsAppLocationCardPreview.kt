package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.CardStyleMode
import com.example.model.CardTheme
import com.example.model.GeoPoint
import com.example.model.WhatsAppCardConfig
import com.example.ui.theme.WaActionBlueLight
import com.example.ui.theme.WaActionTealDark
import com.example.ui.theme.WaBubbleDark
import com.example.ui.theme.WaBubbleLight
import com.example.ui.theme.WaCheckBlue
import com.example.ui.theme.WaDividerDark
import com.example.ui.theme.WaDividerLight
import com.example.ui.theme.WaSubTextDark
import com.example.ui.theme.WaSubTextLight
import com.example.ui.theme.WaTextDark
import com.example.ui.theme.WaTextLight
import com.example.ui.theme.WaWallpaperDark
import com.example.ui.theme.WaWallpaperLight
import com.example.util.MapTileHelper
import com.example.util.WhatsAppCardRenderer

@Composable
fun WhatsAppLocationCardPreview(
    config: WhatsAppCardConfig,
    coordinate: GeoPoint,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLight = config.theme == CardTheme.WHATSAPP_LIGHT
    val isLive = config.styleMode == CardStyleMode.LIVE_LOCATION

    val wallpaperColor = if (isLight) WaWallpaperLight else WaWallpaperDark
    val bubbleColor = if (isLight) WaBubbleLight else WaBubbleDark
    val textPrimary = if (isLight) WaTextDark else WaTextLight
    val textSecondary = if (isLight) WaSubTextLight else WaSubTextDark
    val dividerColor = if (isLight) WaDividerLight else WaDividerDark
    val linkColor = if (isLight) WaActionBlueLight else WaActionTealDark

    val tilePos = remember(coordinate) { MapTileHelper.latLngToTile(coordinate, 15) }
    val timestampText = remember(config.customTimestamp) {
        WhatsAppCardRenderer.resolveCurrentTimestamp(config.customTimestamp)
    }
    val expiryText = remember(config.liveDuration) {
        WhatsAppCardRenderer.resolveExpiryText(config.liveDuration.minutes)
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

    val infiniteTransition = rememberInfiniteTransition(label = "waLivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waPulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(wallpaperColor)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        // Subtle WhatsApp doodle background pattern
        Canvas(modifier = Modifier.matchParentSize()) {
            val patternTint = if (isLight) Color(0x10000000) else Color(0x0EFFFFFF)
            var gx = 20f
            while (gx < size.width) {
                var gy = 20f
                while (gy < size.height) {
                    drawCircle(color = patternTint, radius = 8f, center = Offset(gx, gy), style = Stroke(2f))
                    drawLine(
                        color = patternTint,
                        start = Offset(gx + 18f, gy - 8f),
                        end = Offset(gx + 28f, gy + 4f),
                        strokeWidth = 2f
                    )
                    gy += 70f
                }
                gx += 80f
            }
        }

        // Right-aligned outgoing WhatsApp message bubble + tail
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .widthIn(max = 315.dp)
                .fillMaxWidth(0.92f)
        ) {
            // Tail at top right
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
            ) {
                val tail = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    quadraticTo(size.width, size.height * 0.25f, 4f, size.height)
                    close()
                }
                drawPath(path = tail, color = bubbleColor)
            }

            Surface(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 14.dp)),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                color = bubbleColor
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Map Preview Thumbnail inside bubble
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth()
                            .height(144.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFECE9E1))
                    ) {
                        // Procedural street map underlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = Color(0xFFECE9E1))
                            drawRoundRect(
                                color = Color(0xFFCBE7C3),
                                topLeft = Offset(24f, 18f),
                                size = Size(130f, 80f),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawLine(
                                color = Color(0xFFAAD3DF),
                                start = Offset(-10f, size.height - 20f),
                                end = Offset(130f, -10f),
                                strokeWidth = 18f
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, size.height * 0.45f),
                                end = Offset(size.width, size.height * 0.6f),
                                strokeWidth = 10f
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(size.width * 0.35f, 0f),
                                end = Offset(size.width * 0.48f, size.height),
                                strokeWidth = 10f
                            )
                            drawLine(
                                color = Color(0xFFFCE2A6),
                                start = Offset(0f, size.height * 0.72f),
                                end = Offset(size.width, size.height * 0.25f),
                                strokeWidth = 14f
                            )
                        }

                        // Real OSM tile
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tilePos.osmUrl())
                                .crossfade(true)
                                .build(),
                            imageLoader = tileImageLoader,
                            contentDescription = "Prévia do mapa no card",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Center Pin or Live Avatar Beacon
                        if (isLive) {
                            Box(
                                modifier = Modifier.align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((44 * pulseScale).dp)
                                        .clip(CircleShape)
                                        .background(Color(0x3825D366))
                                        .border(1.5.dp, Color(0xFF25D366), CircleShape)
                                )
                                AvatarCircle(
                                    avatarUriString = config.avatarUriString,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(2.5.dp, Color.White, CircleShape)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Pino de localização",
                                tint = Color(0xFFEA4335),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                            )
                        }

                        // Subtle attribution watermark
                        Text(
                            text = "Mapa",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xCCFFFFFF),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(Color(0x33000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (isLive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(
                                avatarUriString = config.avatarUriString,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Localização em tempo real",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = expiryText,
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = config.placeTitle.ifBlank { "Localização" },
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = config.placeAddress.ifBlank { coordinate.formatted(5) },
                                fontSize = 12.sp,
                                color = textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (config.commentText.isNotBlank()) {
                        Text(
                            text = config.commentText,
                            fontSize = 13.5.sp,
                            color = textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }

                    // Timestamp + double blue checkmarks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp, bottom = 6.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timestampText,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        if (config.showReadChecks) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Mensagem lida",
                                tint = WaCheckBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (isLive) {
                        HorizontalDivider(color = dividerColor, thickness = 0.8.dp)
                        Text(
                            text = "Ver localização em tempo real",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = linkColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarCircle(
    avatarUriString: String?,
    modifier: Modifier = Modifier
) {
    if (!avatarUriString.isNullOrBlank()) {
        AsyncImage(
            model = Uri.parse(avatarUriString),
            contentDescription = "Avatar do card",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_default_avatar),
            contentDescription = "Avatar padrão",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    }
}
