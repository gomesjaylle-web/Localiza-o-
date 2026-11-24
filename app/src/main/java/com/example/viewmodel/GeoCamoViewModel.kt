package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GeoCamoApplication
import com.example.data.GeocodingClient
import com.example.data.PresetLocations
import com.example.model.CamoPlace
import com.example.model.CamoSessionState
import com.example.model.CardStyleMode
import com.example.model.CardTheme
import com.example.model.GeoPoint
import com.example.model.LiveDuration
import com.example.model.MovementMode
import com.example.model.PlaceCategory
import com.example.model.WhatsAppCardConfig
import com.example.service.CamouflageLocationService
import com.example.util.WhatsAppCardRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeoCamoUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<CamoPlace> = emptyList(),
    val favoritePlaces: List<CamoPlace> = emptyList(),
    val presetPlaces: List<CamoPlace> = PresetLocations.defaultPlaces,
    val cardConfig: WhatsAppCardConfig = WhatsAppCardConfig(),
    val isMockAppSelectedInSystem: Boolean = false,
    val isSharingCard: Boolean = false,
    val statusBannerMessage: String? = null
)

class GeoCamoViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GeoCamoApplication
    private val repository = app.preferencesRepository
    private val engine = app.camouflageEngine

    val sessionState: StateFlow<CamoSessionState> = engine.sessionState

    private val _uiState = MutableStateFlow(GeoCamoUiState())
    val uiState: StateFlow<GeoCamoUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    init {
        viewModelScope.launch {
            val savedPlace = repository.lastSelectedPlaceFlow.first()
            val savedMode = repository.movementModeFlow.first()
            engine.updateTargetPlace(savedPlace)
            engine.updateMovementMode(savedMode)
        }
        viewModelScope.launch {
            repository.favoritePlacesFlow.collect { favorites ->
                _uiState.update { it.copy(favoritePlaces = favorites) }
            }
        }
        viewModelScope.launch {
            repository.cardConfigFlow.collect { config ->
                _uiState.update { it.copy(cardConfig = config) }
            }
        }
        refreshSystemMockStatus()
    }

    fun refreshSystemMockStatus() {
        val enabled = engine.isMockLocationAppSelected()
        _uiState.update { it.copy(isMockAppSelectedInSystem = enabled) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350L)
            _uiState.update { it.copy(isSearching = true) }
            val results = GeocodingClient.search(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun selectPlace(place: CamoPlace) {
        engine.updateTargetPlace(place)
        viewModelScope.launch {
            repository.saveSelectedPlace(place)
            val updatedCard = _uiState.value.cardConfig.copy(
                placeTitle = place.name,
                placeAddress = place.address
            )
            repository.saveCardConfig(updatedCard)
        }
        clearSearch()
    }

    fun selectMapCoordinate(point: GeoPoint) {
        val tempPlace = CamoPlace(
            id = "custom_${System.currentTimeMillis()}",
            name = "Ponto no Mapa",
            address = point.formatted(5),
            coordinates = point,
            category = PlaceCategory.CUSTOM
        )
        engine.updateTargetPlace(tempPlace)

        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            delay(450L)
            val (title, address) = GeocodingClient.reverse(point)
            val resolvedPlace = tempPlace.copy(name = title, address = address)
            engine.updateTargetPlace(resolvedPlace)
            repository.saveSelectedPlace(resolvedPlace)
            val updatedCard = _uiState.value.cardConfig.copy(
                placeTitle = title,
                placeAddress = address
            )
            repository.saveCardConfig(updatedCard)
        }
    }

    fun setMovementMode(mode: MovementMode) {
        engine.updateMovementMode(mode)
        viewModelScope.launch {
            repository.saveMovementMode(mode)
        }
    }

    fun toggleCamouflage(context: Context, canStartLocationForegroundService: Boolean) {
        if (sessionState.value.isCamouflaging) {
            if (canStartLocationForegroundService) {
                CamouflageLocationService.stop(context)
            } else {
                engine.stopCamouflage()
            }
            _uiState.update { it.copy(statusBannerMessage = "Camuflagem encerrada.") }
        } else {
            refreshSystemMockStatus()
            if (canStartLocationForegroundService) {
                CamouflageLocationService.start(context)
            } else {
                engine.startCamouflage()
            }
            val msg = if (_uiState.value.isMockAppSelectedInSystem) {
                "Camuflagem ativa no GPS do sistema!"
            } else {
                "Simulação em tempo real ativa. Ative 'Local fictício' para refletir em outros apps."
            }
            _uiState.update { it.copy(statusBannerMessage = msg) }
        }
    }

    fun toggleFavoriteCurrentPlace() {
        val current = sessionState.value.selectedPlace
        val favorites = _uiState.value.favoritePlaces
        val existing = favorites.find {
            it.coordinates.formatted(4) == current.coordinates.formatted(4)
        }
        val nextFavorites = if (existing != null) {
            favorites.filterNot { it.id == existing.id }
        } else {
            listOf(current.copy(id = "fav_${System.currentTimeMillis()}", isFavorite = true)) + favorites
        }
        viewModelScope.launch {
            repository.saveFavoritePlaces(nextFavorites)
        }
        _uiState.update {
            it.copy(
                statusBannerMessage = if (existing != null) {
                    "Removido dos locais salvos."
                } else {
                    "Local salvo nos favoritos!"
                }
            )
        }
    }

    fun removeFavoritePlace(placeId: String) {
        val updated = _uiState.value.favoritePlaces.filterNot { it.id == placeId }
        viewModelScope.launch {
            repository.saveFavoritePlaces(updated)
        }
    }

    fun updateCardConfig(transform: (WhatsAppCardConfig) -> WhatsAppCardConfig) {
        val updated = transform(_uiState.value.cardConfig)
        _uiState.update { it.copy(cardConfig = updated) }
        viewModelScope.launch {
            repository.saveCardConfig(updated)
        }
    }

    fun setCardStyle(styleMode: CardStyleMode) = updateCardConfig { it.copy(styleMode = styleMode) }
    fun setCardTheme(theme: CardTheme) = updateCardConfig { it.copy(theme = theme) }
    fun setCardDuration(duration: LiveDuration) = updateCardConfig { it.copy(liveDuration = duration) }

    fun shareCard(context: Context, preferWhatsApp: Boolean) {
        if (_uiState.value.isSharingCard) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSharingCard = true) }
            val result = WhatsAppCardRenderer.renderAndShareCard(
                context = context,
                config = _uiState.value.cardConfig,
                coordinate = sessionState.value.currentCoordinate,
                preferWhatsAppPackage = preferWhatsApp
            )
            _uiState.update {
                it.copy(
                    isSharingCard = false,
                    statusBannerMessage = if (result.isFailure) {
                        "Não foi possível gerar o card agora. Tente novamente."
                    } else null
                )
            }
        }
    }

    fun shareLinkOnly(context: Context) {
        WhatsAppCardRenderer.shareLinkOnlyToWhatsApp(
            context = context,
            config = _uiState.value.cardConfig,
            coordinate = sessionState.value.currentCoordinate
        )
    }

    fun copyMapsLink(context: Context) {
        WhatsAppCardRenderer.copyMapsLink(context, sessionState.value.currentCoordinate)
        _uiState.update { it.copy(statusBannerMessage = "Link do mapa copiado!") }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }
}
