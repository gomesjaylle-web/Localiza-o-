package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.GeoCamoApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CamouflageLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            (application as GeoCamoApplication).camouflageEngine.stopCamouflage()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val engine = (application as GeoCamoApplication).camouflageEngine
        engine.startCamouflage()

        val initialState = engine.sessionState.value
        val notification = buildNotification(
            placeName = initialState.selectedPlace.name,
            coordText = initialState.currentCoordinate.formatted(5),
            isSystemGps = initialState.mockGpsStatus.title
        )

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasLocationPermission) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                // Without runtime location permission, Android 14+ forbids FOREGROUND_SERVICE_TYPE_LOCATION.
                // Keep engine running in-app cleanly and stop foreground wrapper.
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        observerJob?.cancel()
        observerJob = serviceScope.launch {
            engine.sessionState.collectLatest { state ->
                if (!state.isCamouflaging) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }
                val updated = buildNotification(
                    placeName = state.selectedPlace.name,
                    coordText = state.currentCoordinate.formatted(5),
                    isSystemGps = state.mockGpsStatus.title
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, updated)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(placeName: String, coordText: String, isSystemGps: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CamouflageLocationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.camo_active_title))
            .setContentText("$placeName • $coordText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$placeName\n$coordText\n$isSystemGps"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.camo_stop_action), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camuflagem de GPS em Tempo Real",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a localização camuflada ativa em segundo plano"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "geocamo_live_channel"
        private const val NOTIFICATION_ID = 4091
        const val ACTION_STOP = "com.example.action.STOP_CAMO"

        fun start(context: Context) {
            val intent = Intent(context, CamouflageLocationService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CamouflageLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
