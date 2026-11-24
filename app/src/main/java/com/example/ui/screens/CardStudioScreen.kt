package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CamoSessionState
import com.example.model.CardStyleMode
import com.example.model.CardTheme
import com.example.model.LiveDuration
import com.example.model.WhatsAppCardConfig
import com.example.ui.components.WhatsAppLocationCardPreview
import com.example.ui.theme.WaHeaderDark
import com.example.ui.theme.WaHeaderLight
import com.example.viewmodel.GeoCamoUiState

@Composable
fun CardStudioScreen(
    sessionState: CamoSessionState,
    uiState: GeoCamoUiState,
    onUpdateConfig: ((WhatsAppCardConfig) -> WhatsAppCardConfig) -> Unit,
    onSetStyle: (CardStyleMode) -> Unit,
    onSetTheme: (CardTheme) -> Unit,
    onSetDuration: (LiveDuration) -> Unit,
    onShareToWhatsApp: () -> Unit,
    onShareToSystemSheet: () -> Unit,
    onShareLinkOnly: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = uiState.cardConfig

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateConfig { it.copy(avatarUriString = uri.toString()) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhatsAppPreviewFrame(config = config, sessionState = sessionState)
                    ShareActionPanel(
                        isSharing = uiState.isSharingCard,
                        onShareToWhatsApp = onShareToWhatsApp,
                        onShareToSystemSheet = onShareToSystemSheet,
                        onShareLinkOnly = onShareLinkOnly,
                        onCopyLink = onCopyLink
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CardCustomizationControls(
                        config = config,
                        onUpdateConfig = onUpdateConfig,
                        onSetStyle = onSetStyle,
                        onSetTheme = onSetTheme,
                        onSetDuration = onSetDuration,
                        onPickAvatar = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WhatsAppPreviewFrame(config = config, sessionState = sessionState)

                ShareActionPanel(
                    isSharing = uiState.isSharingCard,
                    onShareToWhatsApp = onShareToWhatsApp,
                    onShareToSystemSheet = onShareToSystemSheet,
                    onShareLinkOnly = onShareLinkOnly,
                    onCopyLink = onCopyLink
                )

                CardCustomizationControls(
                    config = config,
                    onUpdateConfig = onUpdateConfig,
                    onSetStyle = onSetStyle,
                    onSetTheme = onSetTheme,
                    onSetDuration = onSetDuration,
                    onPickAvatar = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WhatsAppPreviewFrame(
    config: WhatsAppCardConfig,
    sessionState: CamoSessionState
) {
    val isLight = config.theme == CardTheme.WHATSAPP_LIGHT
    val headerBg = if (isLight) WaHeaderLight else WaHeaderDark

    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mock chat top strip matching WhatsApp visual context
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prévia do Card Compartilhado",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sessionState.currentCoordinate.formatted(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            WhatsAppLocationCardPreview(
                config = config,
                coordinate = sessionState.currentCoordinate
            )
        }
    }
}

@Composable
private fun ShareActionPanel(
    isSharing: Boolean,
    onShareToWhatsApp: () -> Unit,
    onShareToSystemSheet: () -> Unit,
    onShareLinkOnly: () -> Unit,
    onCopyLink: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onShareToWhatsApp,
            enabled = !isSharing,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("share_whatsapp_card_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF25D366),
                contentColor = Color(0xFF002412)
            )
        ) {
            if (isSharing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF002412)
                )
            } else {
                Icon(Icons.Filled.Send, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enviar Card Idêntico no WhatsApp",
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onShareToSystemSheet,
                enabled = !isSharing,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_system_sheet_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Outros Apps", maxLines = 1)
            }

            OutlinedButton(
                onClick = onShareLinkOnly,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_link_only_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Link Mapa", maxLines = 1)
            }

            IconButton(
                onClick = onCopyLink,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("copy_maps_link_button")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar link do mapa")
            }
        }
    }
}

@Composable
private fun CardCustomizationControls(
    config: WhatsAppCardConfig,
    onUpdateConfig: ((WhatsAppCardConfig) -> WhatsAppCardConfig) -> Unit,
    onSetStyle: (CardStyleMode) -> Unit,
    onSetTheme: (CardTheme) -> Unit,
    onSetDuration: (LiveDuration) -> Unit,
    onPickAvatar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Personalizar Card do WhatsApp",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Card Type selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Formato do Card", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CardStyleMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = config.styleMode == mode,
                            onClick = { onSetStyle(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CardStyleMode.entries.size
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (mode == CardStyleMode.LIVE_LOCATION) {
                                        Icons.Filled.MyLocation
                                    } else {
                                        Icons.Filled.LocationOn
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(mode.label)
                        }
                    }
                }
            }

            // WhatsApp Bubble Theme selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tema da Bolha", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CardTheme.entries.forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = config.theme == theme,
                            onClick = { onSetTheme(theme) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CardTheme.entries.size
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (theme == CardTheme.WHATSAPP_LIGHT) {
                                        Icons.Filled.LightMode
                                    } else {
                                        Icons.Filled.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(theme.label)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = config.styleMode == CardStyleMode.LIVE_LOCATION) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Duração simulada", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveDuration.entries.forEach { duration ->
                            FilterChip(
                                selected = config.liveDuration == duration,
                                onClick = { onSetDuration(duration) },
                                label = { Text(duration.label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickAvatar,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_avatar_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escolher Foto do Perfil")
                        }
                        if (config.avatarUriString != null) {
                            IconButton(
                                onClick = { onUpdateConfig { it.copy(avatarUriString = null) } },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "Remover foto personalizada"
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = config.styleMode == CardStyleMode.PINNED_LOCATION) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.placeTitle,
                        onValueChange = { v -> onUpdateConfig { it.copy(placeTitle = v) } },
                        label = { Text("Nome do Local no Card") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = config.placeAddress,
                        onValueChange = { v -> onUpdateConfig { it.copy(placeAddress = v) } },
                        label = { Text("Endereço Exibido") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = config.commentText,
                onValueChange = { v -> onUpdateConfig { it.copy(commentText = v) } },
                label = { Text("Legenda / Mensagem (opcional)") },
                placeholder = { Text("Ex: Já estou aqui esperando...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = config.customTimestamp,
                onValueChange = { v -> onUpdateConfig { it.copy(customTimestamp = v) } },
                label = { Text("Horário exibido (vazio = hora atual)") },
                placeholder = { Text("Ex: 20:45") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Check duplo azul (✓✓)", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = config.showReadChecks,
                    onCheckedChange = { checked -> onUpdateConfig { it.copy(showReadChecks = checked) } }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Anexar link do Google Maps", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Envia também o link clicável da coordenada camuflada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.includeMapsLinkOnShare,
                    onCheckedChange = { checked ->
                        onUpdateConfig { it.copy(includeMapsLinkOnShare = checked) }
                    }
                )
            }
        }
    }
}
