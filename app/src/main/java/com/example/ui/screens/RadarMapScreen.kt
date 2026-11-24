package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.CamoPlace
import com.example.model.CamoSessionState
import com.example.model.GeoPoint
import com.example.model.MockGpsStatus
import com.example.model.MovementMode
import com.example.ui.components.InteractiveCamoMap
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldSignal
import com.example.util.WhatsAppCardRenderer
import com.example.viewmodel.GeoCamoUiState

@Composable
fun RadarMapScreen(
    sessionState: CamoSessionState,
    uiState: GeoCamoUiState,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectPlace: (CamoPlace) -> Unit,
    onMapCoordinateSelected: (GeoPoint) -> Unit,
    onMovementModeSelected: (MovementMode) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCamouflage: (Boolean) -> Unit,
    onOpenCardStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val hasLoc = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onToggleCamouflage(hasLoc)
    }

    val isCurrentFavorite = remember(uiState.favoritePlaces, sessionState.selectedPlace) {
        uiState.favoritePlaces.any {
            it.coordinates.formatted(4) == sessionState.selectedPlace.coordinates.formatted(4)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Status & System Mock Banner
        StatusHeaderCard(
            sessionState = sessionState,
            isMockAppSelected = uiState.isMockAppSelectedInSystem,
            onOpenDevOptions = { WhatsAppCardRenderer.openDeveloperOptions(context) }
        )

        // Search input + dropdown
        Column {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_location_input"),
                placeholder = { Text("Buscar rua, shopping, bairro ou cidade...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar endereço")
                },
                trailingIcon = {
                    when {
                        uiState.isSearching -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        uiState.searchQuery.isNotEmpty() -> IconButton(onClick = onClearSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            AnimatedVisibility(visible = uiState.searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        uiState.searchResults.take(5).forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlace(result) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = result.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Preset Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(uiState.presetPlaces, key = { it.id }) { preset ->
                val isSelected = preset.coordinates.formatted(4) ==
                    sessionState.selectedPlace.coordinates.formatted(4)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPlace(preset) },
                    label = {
                        Text(
                            text = preset.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Interactive Camouflage Map
        InteractiveCamoMap(
            sessionState = sessionState,
            isFavorite = isCurrentFavorite,
            onMapCoordinateSelected = onMapCoordinateSelected,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 290.dp, max = 360.dp)
        )

        // Selected Place Summary
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sessionState.selectedPlace.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sessionState.selectedPlace.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Movement Mode Selector
        Text(
            text = "Modo de Simulação em Tempo Real",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MovementMode.entries.forEach { mode ->
                val selected = sessionState.movementMode == mode
                val icon = when (mode) {
                    MovementMode.STATIC -> Icons.Filled.GpsFixed
                    MovementMode.DRIFT -> Icons.Filled.Waves
                    MovementMode.WALK -> Icons.Filled.DirectionsWalk
                    MovementMode.DRIVE -> Icons.Filled.DirectionsCar
                }
                FilterChip(
                    selected = selected,
                    onClick = { onMovementModeSelected(mode) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Primary CTA Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (sessionState.isCamouflaging) {
                        onToggleCamouflage(true)
                    } else {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasFine || hasCoarse) {
                            onToggleCamouflage(true)
                        } else {
                            val toRequest = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                            permissionLauncher.launch(toRequest)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1.35f)
                    .height(54.dp)
                    .testTag("toggle_camouflage_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sessionState.isCamouflaging) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = if (sessionState.isCamouflaging) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sessionState.isCamouflaging) "Parar Camuflagem" else "Ativar Camuflagem",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onOpenCardStudio,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("quick_open_card_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Card WA", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusHeaderCard(
    sessionState: CamoSessionState,
    isMockAppSelected: Boolean,
    onOpenDevOptions: () -> Unit
) {
    val active = sessionState.isCamouflaging
    val isSystemGps = sessionState.mockGpsStatus == MockGpsStatus.ACTIVE_SYSTEM_GPS
    val badgeColor = when {
        isSystemGps -> EmeraldGlow
        active -> EmeraldSignal
        isMockAppSelected -> MaterialTheme.colorScheme.primary
        else -> AlertAmber
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = badgeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sessionState.mockGpsStatus.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        isSystemGps -> "GPS do aparelho mascarado para outros apps"
                        active -> "Simulando rota no app • ${sessionState.movementMode.label}"
                        isMockAppSelected -> "App fictício pronto • toque em Ativar"
                        else -> "Para tempo real nativo no WhatsApp, habilite Local Fictício"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isMockAppSelected) {
                TextButton(onClick = onOpenDevOptions) {
                    Text("Configurar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
