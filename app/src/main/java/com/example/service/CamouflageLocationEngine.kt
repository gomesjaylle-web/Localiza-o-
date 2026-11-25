package com.example.service

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.example.data.PresetLocations
import com.example.model.CamoPlace
import com.example.model.CamoSessionState
import com.example.model.GeoPoint
import com.example.model.MockGpsStatus
import com.example.model.MovementMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CamouflageLocationEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val defaultPlace = PresetLocations.defaultPlaces.first()
    private val _sessionState = MutableStateFlow(
        CamoSessionState(
            isCamouflaging = false,
            selectedPlace = defaultPlace,
            currentCoordinate = defaultPlace.coordinates,
            movementMode = MovementMode.DRIFT
        )
    )
    val sessionState: StateFlow<CamoSessionState> = _sessionState.asStateFlow()

    private var simulationJob: Job? = null
    private var angleRad = 0.0
    private var testProvidersRegistered = false

    fun updateTargetPlace(place: CamoPlace) {
        angleRad = 0.0
        _sessionState.update { state ->
            state.copy(
                selectedPlace = place,
                currentCoordinate = place.coordinates,
                trailHistory = if (state.isCamouflaging) listOf(place.coordinates) else emptyList()
            )
        }
        if (_sessionState.value.isCamouflaging) {
            pushSystemMockLocation(_sessionState.value.currentCoordinate, 0f, 0f)
        }
    }

    fun updateMovementMode(mode: MovementMode) {
        _sessionState.update { it.copy(movementMode = mode) }
    }

    fun isMockLocationAppSelected(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun startCamouflage() {
        if (_sessionState.value.isCamouflaging) return
        val systemMockEnabled = ensureTestProviders()
        val initialStatus = if (systemMockEnabled) {
            MockGpsStatus.ACTIVE_SYSTEM_GPS
        } else {
            MockGpsStatus.ACTIVE_APP_SIMULATION_ONLY
        }

        _sessionState.update { state ->
            state.copy(
                isCamouflaging = true,
                elapsedSeconds = 0L,
                mockGpsStatus = initialStatus,
                trailHistory = listOf(state.currentCoordinate)
            )
        }

        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive) {
                tickSimulationStep()
                delay(1000L)
            }
        }
    }

    fun stopCamouflage() {
        simulationJob?.cancel()
        simulationJob = null
        removeTestProviders()
        _sessionState.update { state ->
            state.copy(
                isCamouflaging = false,
                speedKmh = 0.0,
                mockGpsStatus = MockGpsStatus.INACTIVE,
                currentCoordinate = state.selectedPlace.coordinates
            )
        }
    }

    private fun tickSimulationStep() {
        val current = _sessionState.value
        val origin = current.selectedPlace.coordinates
        val mode = current.movementMode

        val (nextPoint, speedKmh, bearing) = when (mode) {
            MovementMode.STATIC -> {
                Triple(origin, 0.0, 0f)
            }
            MovementMode.DRIFT -> {
                angleRad = (angleRad + 0.35) % (2 * PI)
                val jitterMeters = 3.0 + Random.nextDouble(0.0, 5.5)
                val point = offsetPoint(origin, jitterMeters, angleRad)
                Triple(point, 0.8 + Random.nextDouble(0.0, 0.9), Math.toDegrees(angleRad).toFloat())
            }
            MovementMode.WALK -> {
                // Realistic ~90m loop walk around the chosen location
                angleRad = (angleRad + 0.045) % (2 * PI)
                val radiusMeters = 85.0 + 12.0 * sin(angleRad * 3.0)
                val point = offsetPoint(origin, radiusMeters, angleRad)
                val heading = ((Math.toDegrees(angleRad) + 90.0) % 360.0).toFloat()
                Triple(point, 4.8 + Random.nextDouble(-0.4, 0.7), heading)
            }
            MovementMode.DRIVE -> {
                // Realistic ~420m urban block loop
                angleRad = (angleRad + 0.065) % (2 * PI)
                val radiusMeters = 420.0 + 60.0 * cos(angleRad * 2.0)
                val point = offsetPoint(origin, radiusMeters, angleRad)
                val heading = ((Math.toDegrees(angleRad) + 90.0) % 360.0).toFloat()
                Triple(point, 38.0 + Random.nextDouble(-4.0, 8.5), heading)
            }
        }

        val pushedToSystem = pushSystemMockLocation(
            point = nextPoint,
            speedMs = (speedKmh / 3.6).toFloat(),
            bearing = bearing
        )

        val status = if (pushedToSystem) {
            MockGpsStatus.ACTIVE_SYSTEM_GPS
        } else {
            MockGpsStatus.ACTIVE_APP_SIMULATION_ONLY
        }

        val updatedTrail = (current.trailHistory + nextPoint).takeLast(45)

        _sessionState.update { state ->
            state.copy(
                currentCoordinate = nextPoint,
                speedKmh = speedKmh,
                headingDegrees = bearing,
                accuracyMeters = 3.5f + Random.nextFloat() * 3.0f,
                elapsedSeconds = state.elapsedSeconds + 1L,
                mockGpsStatus = status,
                trailHistory = updatedTrail
            )
        }
    }

    private fun offsetPoint(origin: GeoPoint, distanceMeters: Double, radians: Double): GeoPoint {
        val earthRadius = 6378137.0
        val dLat = (distanceMeters * cos(radians)) / earthRadius
        val dLng = (distanceMeters * sin(radians)) / (earthRadius * cos(Math.toRadians(origin.latitude)))
        return GeoPoint(
            latitude = origin.latitude + Math.toDegrees(dLat),
            longitude = origin.longitude + Math.toDegrees(dLng)
        )
    }

    @SuppressLint("WrongConstant")
    private fun ensureTestProviders(): Boolean {
        if (testProvidersRegistered) return true
        return try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                try {
                    locationManager.removeTestProvider(provider)
                } catch (_: Exception) {
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationManager.addTestProvider(
                        provider,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_FINE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    locationManager.addTestProvider(
                        provider,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        Criteria.POWER_LOW,
                        Criteria.ACCURACY_FINE
                    )
                }
                locationManager.setTestProviderEnabled(provider, true)
            }
            testProvidersRegistered = true
            true
        } catch (_: SecurityException) {
            testProvidersRegistered = false
            false
        } catch (_: Exception) {
            testProvidersRegistered = false
            false
        }
    }

    private fun pushSystemMockLocation(point: GeoPoint, speedMs: Float, bearing: Float): Boolean {
        if (!testProvidersRegistered && !ensureTestProviders()) {
            return false
        }
        return try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                val loc = Location(provider).apply {
                    latitude = point.latitude
                    longitude = point.longitude
                    altitude = 760.0
                    accuracy = 4.0f
                    speed = speedMs
                    this.bearing = bearing
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bearingAccuracyDegrees = 3.0f
                        speedAccuracyMetersPerSecond = 0.5f
                        verticalAccuracyMeters = 3.0f
                    }
                }
                locationManager.setTestProviderLocation(provider, loc)
            }
            true
        } catch (_: SecurityException) {
            testProvidersRegistered = false
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun removeTestProviders() {
        if (!testProvidersRegistered) return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                locationManager.setTestProviderEnabled(provider, false)
                locationManager.removeTestProvider(provider)
            } catch (_: Exception) {
            }
        }
        testProvidersRegistered = false
    }
}
