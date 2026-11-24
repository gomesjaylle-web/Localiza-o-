package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.CardStyleMode
import com.example.model.CardTheme
import com.example.model.GeoPoint
import com.example.model.WhatsAppCardConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

object WhatsAppCardRenderer {

    private val tileClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    fun resolveCurrentTimestamp(custom: String): String {
        if (custom.isNotBlank()) return custom.trim()
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    fun resolveExpiryText(minutes: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutes)
        val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        return "Atualizado agora • até às $endTime"
    }

    suspend fun renderAndShareCard(
        context: Context,
        config: WhatsAppCardConfig,
        coordinate: GeoPoint,
        preferWhatsAppPackage: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = renderCardBitmap(context, config, coordinate)
            val cacheFolder = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(cacheFolder, "geocamo_whatsapp_card_${System.currentTimeMillis()}.png")
            // Clean older files to avoid cache bloat
            cacheFolder.listFiles()?.forEach { old ->
                if (old.name != outFile.name && System.currentTimeMillis() - old.lastModified() > 60_000L) {
                    old.delete()
                }
            }
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )

            val caption = buildShareCaption(config, coordinate)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (caption.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (preferWhatsAppPackage) {
                    val waPkg = findInstalledWhatsAppPackage(context)
                    if (waPkg != null) {
                        setPackage(waPkg)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (preferWhatsAppPackage && sendIntent.`package` != null) {
                    context.startActivity(sendIntent)
                } else {
                    val chooser = Intent.createChooser(
                        sendIntent,
                        context.getString(R.string.share_card_chooser)
                    )
                    context.startActivity(chooser)
                }
            }
        }
    }

    fun shareLinkOnlyToWhatsApp(
        context: Context,
        config: WhatsAppCardConfig,
        coordinate: GeoPoint
    ) {
        val text = buildShareCaption(config, coordinate).ifBlank {
            "${config.placeTitle}\n${coordinate.googleMapsUrl()}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            val waPkg = findInstalledWhatsAppPackage(context)
            if (waPkg != null) setPackage(waPkg)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(fallback, context.getString(R.string.share_card_chooser)))
        }
    }

    fun copyMapsLink(context: Context, coordinate: GeoPoint) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Localização", coordinate.googleMapsUrl()))
    }

    fun openDeveloperOptions(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun buildShareCaption(config: WhatsAppCardConfig, coordinate: GeoPoint): String {
        val parts = mutableListOf<String>()
        if (config.commentText.isNotBlank()) {
            parts.add(config.commentText.trim())
        }
        if (config.includeMapsLinkOnShare) {
            if (config.styleMode == CardStyleMode.LIVE_LOCATION) {
                parts.add("📍 Localização em tempo real (${config.placeTitle}): ${coordinate.googleMapsUrl()}")
            } else {
                parts.add("📍 ${config.placeTitle}\n${coordinate.googleMapsUrl()}")
            }
        }
        return parts.joinToString("\n")
    }

    private fun findInstalledWhatsAppPackage(context: Context): String? {
        val candidates = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in candidates) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(pkg, 0)
                }
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return null
    }

    private fun fetchOsmTileBitmap(coordinate: GeoPoint, zoom: Int = 15): Bitmap? {
        return try {
            val tilePos = MapTileHelper.latLngToTile(coordinate, zoom)
            val request = Request.Builder()
                .url(tilePos.osmUrl())
                .header("User-Agent", "GeoCamoAndroid/1.0")
                .build()
            tileClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAvatarBitmap(context: Context, uriString: String?): Bitmap? {
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) return decoded
                }
            } catch (_: Exception) {
            }
        }
        return BitmapFactory.decodeResource(context.resources, R.drawable.img_default_avatar)
    }

    private fun renderCardBitmap(
        context: Context,
        config: WhatsAppCardConfig,
        coordinate: GeoPoint
    ): Bitmap {
        val isLight = config.theme == CardTheme.WHATSAPP_LIGHT
        val isLive = config.styleMode == CardStyleMode.LIVE_LOCATION

        val width = 1020
        val hasComment = config.commentText.isNotBlank()
        val height = when {
            isLive && hasComment -> 790
            isLive -> 700
            hasComment -> 710
            else -> 630
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Authentic WhatsApp chat wallpaper background
        val wallpaperColor = if (isLight) Color.parseColor("#EFEAE2") else Color.parseColor("#0B141A")
        canvas.drawColor(wallpaperColor)

        // Subtle wallpaper pattern
        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#10000000") else Color.parseColor("#10FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        for (gx in 60..width step 140) {
            for (gy in 50..height step 140) {
                canvas.drawCircle(gx.toFloat(), gy.toFloat(), 14f, patternPaint)
                canvas.drawLine(gx + 30f, gy - 12f, gx + 48f, gy + 6f, patternPaint)
            }
        }

        val bubbleColor = if (isLight) Color.parseColor("#D9FDD3") else Color.parseColor("#005C4B")
        val textPrimary = if (isLight) Color.parseColor("#111B21") else Color.parseColor("#E9EDEF")
        val textSecondary = if (isLight) Color.parseColor("#667781") else Color.parseColor("#A0B3BA")
        val dividerColor = if (isLight) Color.parseColor("#C2E7BD") else Color.parseColor("#0D6F5D")
        val linkColor = if (isLight) Color.parseColor("#027EB5") else Color.parseColor("#53BDEB")

        val bubbleLeft = 96f
        val bubbleTop = 36f
        val bubbleRight = width - 48f
        val bubbleBottom = height - 36f
        val cornerRadius = 28f

        // Drop shadow for bubble
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(bubbleLeft + 2f, bubbleTop + 4f, bubbleRight + 2f, bubbleBottom + 5f),
            cornerRadius,
            cornerRadius,
            shadowPaint
        )

        // Bubble body
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bubbleColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom),
            cornerRadius,
            cornerRadius,
            bubblePaint
        )

        // Outgoing top-right tail
        val tailPath = Path().apply {
            moveTo(bubbleRight - 14f, bubbleTop)
            lineTo(bubbleRight + 20f, bubbleTop)
            quadTo(bubbleRight + 24f, bubbleTop + 2f, bubbleRight + 6f, bubbleTop + 28f)
            lineTo(bubbleRight - 6f, bubbleTop + 34f)
            close()
        }
        canvas.drawPath(tailPath, bubblePaint)

        // Map thumbnail box inside bubble
        val mapPad = 14f
        val mapRect = RectF(
            bubbleLeft + mapPad,
            bubbleTop + mapPad,
            bubbleRight - mapPad,
            bubbleTop + 365f
        )

        canvas.save()
        val mapClip = Path().apply {
            addRoundRect(mapRect, 20f, 20f, Path.Direction.CW)
        }
        canvas.clipPath(mapClip)

        val tileBmp = fetchOsmTileBitmap(coordinate, zoom = 15)
        if (tileBmp != null) {
            canvas.drawBitmap(tileBmp, null, mapRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            drawProceduralStreetMap(canvas, mapRect, coordinate)
        }

        // Subtle Google-style attribution label at bottom-left of map
        val attrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0FFFFFF")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Mapa", mapRect.left + 16f, mapRect.bottom - 14f, attrPaint)

        val pinCx = mapRect.centerX()
        val pinCy = mapRect.centerY()

        if (isLive) {
            // Pulsing emerald radar halo + circular avatar
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3825D366")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pinCx, pinCy, 74f, haloPaint)

            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#25D366")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(pinCx, pinCy, 74f, ringPaint)

            val whiteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pinCx, pinCy, 44f, whiteBorder)

            val avatar = loadAvatarBitmap(context, config.avatarUriString)
            if (avatar != null) {
                drawCircularBitmap(canvas, avatar, pinCx, pinCy, 39f)
            }
        } else {
            drawRedMapPin(canvas, pinCx, pinCy + 8f)
        }

        canvas.restore()

        val contentLeft = bubbleLeft + 24f
        val contentRight = bubbleRight - 24f
        var cursorY = mapRect.bottom + 36f

        if (isLive) {
            // Mini avatar + "Localização em tempo real" + subtitle
            val miniR = 30f
            val avatar = loadAvatarBitmap(context, config.avatarUriString)
            if (avatar != null) {
                drawCircularBitmap(canvas, avatar, contentLeft + miniR, cursorY + 14f, miniR)
            }
            val titleX = contentLeft + miniR * 2 + 18f
            val liveTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 31f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("Localização em tempo real", titleX, cursorY + 10f, liveTitlePaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            canvas.drawText(
                resolveExpiryText(config.liveDuration.minutes),
                titleX,
                cursorY + 44f,
                subPaint
            )

            cursorY += 74f
            if (hasComment) {
                val commentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textPrimary
                    textSize = 28f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                }
                canvas.drawText(
                    ellipsize(config.commentText.trim(), commentPaint, contentRight - contentLeft),
                    contentLeft,
                    cursorY + 16f,
                    commentPaint
                )
                cursorY += 46f
            }

            // Timestamp + checkmarks
            drawTimestampAndChecks(
                canvas = canvas,
                rightX = contentRight,
                baselineY = cursorY + 14f,
                timestamp = resolveCurrentTimestamp(config.customTimestamp),
                showChecks = config.showReadChecks,
                timeColor = textSecondary
            )

            cursorY += 32f
            val divPaint = Paint().apply {
                color = dividerColor
                strokeWidth = 2f
            }
            canvas.drawLine(bubbleLeft, cursorY, bubbleRight, cursorY, divPaint)

            val actionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = linkColor
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(
                "Ver localização em tempo real",
                (bubbleLeft + bubbleRight) / 2f,
                cursorY + 54f,
                actionPaint
            )
        } else {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textPrimary
                textSize = 32f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(
                ellipsize(config.placeTitle.ifBlank { "Localização" }, titlePaint, contentRight - contentLeft),
                contentLeft,
                cursorY + 12f,
                titlePaint
            )

            val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textSecondary
                textSize = 25f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            canvas.drawText(
                ellipsize(config.placeAddress.ifBlank { coordinate.formatted(5) }, addrPaint, contentRight - contentLeft),
                contentLeft,
                cursorY + 48f,
                addrPaint
            )
            cursorY += 76f

            if (hasComment) {
                val commentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textPrimary
                    textSize = 28f
                }
                canvas.drawText(
                    ellipsize(config.commentText.trim(), commentPaint, contentRight - contentLeft),
                    contentLeft,
                    cursorY + 20f,
                    commentPaint
                )
                cursorY += 46f
            }

            drawTimestampAndChecks(
                canvas = canvas,
                rightX = contentRight,
                baselineY = cursorY + 28f,
                timestamp = resolveCurrentTimestamp(config.customTimestamp),
                showChecks = config.showReadChecks,
                timeColor = textSecondary
            )
        }

        return bitmap
    }

    private fun drawProceduralStreetMap(canvas: Canvas, rect: RectF, coord: GeoPoint) {
        val bgPaint = Paint().apply { color = Color.parseColor("#ECE9E1") }
        canvas.drawRect(rect, bgPaint)

        val seedAngle = ((coord.latitude + coord.longitude) * 1000.0) % 360.0
        val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CBE7C3") }
        canvas.drawRoundRect(
            RectF(rect.left + 40f, rect.top + 30f, rect.left + 260f, rect.top + 170f),
            16f, 16f, parkPaint
        )
        canvas.drawRoundRect(
            RectF(rect.right - 220f, rect.bottom - 130f, rect.right - 30f, rect.bottom - 20f),
            16f, 16f, parkPaint
        )

        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAD3DF")
            style = Paint.Style.STROKE
            strokeWidth = 34f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            rect.left - 20f,
            rect.bottom - 40f,
            rect.left + 260f,
            rect.top - 20f,
            waterPaint
        )

        val roadCasing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6D1C6")
            style = Paint.Style.STROKE
            strokeWidth = 22f
            strokeCap = Paint.Cap.ROUND
        }
        val roadFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 16f
            strokeCap = Paint.Cap.ROUND
        }

        val dx = (cos(Math.toRadians(seedAngle)) * 45.0).toFloat()
        val dy = (sin(Math.toRadians(seedAngle)) * 35.0).toFloat()

        val roads = listOf(
            floatArrayOf(rect.left, rect.centerY() - 60f + dy, rect.right, rect.centerY() + 30f - dy),
            floatArrayOf(rect.centerX() - 160f + dx, rect.top, rect.centerX() - 110f - dx, rect.bottom),
            floatArrayOf(rect.centerX() + 140f - dx, rect.top, rect.centerX() + 190f + dx, rect.bottom)
        )
        for (r in roads) {
            canvas.drawLine(r[0], r[1], r[2], r[3], roadCasing)
            canvas.drawLine(r[0], r[1], r[2], r[3], roadFill)
        }

        val mainAvCasing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E3B478")
            style = Paint.Style.STROKE
            strokeWidth = 30f
        }
        val mainAvFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCE2A6")
            style = Paint.Style.STROKE
            strokeWidth = 24f
        }
        canvas.drawLine(
            rect.left,
            rect.centerY() + 70f,
            rect.right,
            rect.centerY() - 55f,
            mainAvCasing
        )
        canvas.drawLine(
            rect.left,
            rect.centerY() + 70f,
            rect.right,
            rect.centerY() - 55f,
            mainAvFill
        )
    }

    private fun drawRedMapPin(canvas: Canvas, cx: Float, tipY: Float) {
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40000000") }
        canvas.drawOval(RectF(cx - 18f, tipY - 5f, cx + 18f, tipY + 5f), shadow)

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EA4335") }
        val pinPath = Path().apply {
            moveTo(cx, tipY)
            cubicTo(cx - 28f, tipY - 26f, cx - 32f, tipY - 58f, cx, tipY - 76f)
            cubicTo(cx + 32f, tipY - 58f, cx + 28f, tipY - 26f, cx, tipY)
            close()
        }
        canvas.drawPath(pinPath, pinPaint)
        canvas.drawCircle(cx, tipY - 48f, 23f, pinPaint)

        val innerDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8C1D18") }
        canvas.drawCircle(cx, tipY - 48f, 9f, innerDot)
    }

    private fun drawCircularBitmap(canvas: Canvas, source: Bitmap, cx: Float, cy: Float, radius: Float) {
        val size = (radius * 2).toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        c.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        c.drawBitmap(source, null, rect, paint)
        canvas.drawBitmap(output, cx - radius, cy - radius, null)
    }

    private fun drawTimestampAndChecks(
        canvas: Canvas,
        rightX: Float,
        baselineY: Float,
        timestamp: String,
        showChecks: Boolean,
        timeColor: Int
    ) {
        val checkWidth = if (showChecks) 42f else 0f
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = timeColor
            textSize = 23f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(timestamp, rightX - checkWidth, baselineY, timePaint)

        if (showChecks) {
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#53BDEB")
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val baseX = rightX - 30f
            val baseY = baselineY - 8f
            // First check
            val p1 = Path().apply {
                moveTo(baseX, baseY)
                lineTo(baseX + 6f, baseY + 6f)
                lineTo(baseX + 17f, baseY - 8f)
            }
            // Second check
            val p2 = Path().apply {
                moveTo(baseX + 9f, baseY)
                lineTo(baseX + 15f, baseY + 6f)
                lineTo(baseX + 26f, baseY - 8f)
            }
            canvas.drawPath(p1, checkPaint)
            canvas.drawPath(p2, checkPaint)
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "..." else ""
    }
}
