package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CamoPlace
import com.example.model.PlaceCategory
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.EmeraldGlow
import com.example.util.WhatsAppCardRenderer
import com.example.viewmodel.GeoCamoUiState

@Composable
fun PresetsAndGuideScreen(
    uiState: GeoCamoUiState,
    onSelectPlaceAndOpenRadar: (CamoPlace) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onRefreshMockStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real GPS Camouflage Setup Guide Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isMockAppSelectedInSystem) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.Warning
                        },
                        contentDescription = null,
                        tint = if (uiState.isMockAppSelectedInSystem) EmeraldGlow else AlertAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.isMockAppSelectedInSystem) {
                            "GPS Fictício Configurado no Android"
                        } else {
                            "Ativar Camuflagem Nativa no GPS do WhatsApp"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Você tem duas formas de camuflar sua localização:\n\n" +
                        "1. Card Instantâneo: Na aba 'Card WhatsApp', personalize o card idêntico (Tempo Real ou Local Fixo) e envie em 1 toque.\n" +
                        "2. Tempo Real Nativo dentro do WhatsApp: Ative o Modo Desenvolvedor do Android > selecione GeoCamo em 'App de local fictício'. Assim, quando você enviar localização em tempo real dentro do WhatsApp, ele transmitirá a rota camuflada!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { WhatsAppCardRenderer.openDeveloperOptions(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_dev_options_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opções de Desenvolvedor", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = onRefreshMockStatus,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Verificar status")
                    }
                }
            }
        }

        // Saved Favorites Section
        Text(
            text = "Locais Salvos (${uiState.favoritePlaces.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (uiState.favoritePlaces.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Nenhum local salvo ainda. Toque no ícone de coração no mapa para salvar seus disfarces favoritos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.favoritePlaces.forEach { place ->
                    PlaceItemCard(
                        place = place,
                        onSelect = { onSelectPlaceAndOpenRadar(place) },
                        onDelete = { onRemoveFavorite(place.id) }
                    )
                }
            }
        }

        // Preset Scenarios Section
        Text(
            text = "Cenários Prontos de Camuflagem",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.presetPlaces.forEach { preset ->
                PlaceItemCard(
                    place = preset,
                    onSelect = { onSelectPlaceAndOpenRadar(preset) },
                    onDelete = null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PlaceItemCard(
    place: CamoPlace,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val icon = when (place.category) {
        PlaceCategory.MALL -> Icons.Filled.ShoppingBag
        PlaceCategory.WORK -> Icons.Filled.Work
        PlaceCategory.HOME -> Icons.Filled.Home
        PlaceCategory.PARK -> Icons.Filled.Park
        PlaceCategory.AIRPORT -> Icons.Filled.FlightTakeoff
        PlaceCategory.CAFE -> Icons.Filled.LocalCafe
        PlaceCategory.CUSTOM -> Icons.Filled.LocationOn
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = place.coordinates.formatted(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Remover local salvo"
                    )
                }
            }
        }
    }
}
