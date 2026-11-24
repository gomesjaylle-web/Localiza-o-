package com.example.model

import java.util.Locale

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    fun formatted(decimals: Int = 5): String {
        return String.format(Locale.US, "%.${decimals}f, %.${decimals}f", latitude, longitude)
    }

    fun googleMapsUrl(): String {
        return String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", latitude, longitude)
    }
}

enum class PlaceCategory(val label: String) {
    MALL("Shopping"),
    WORK("Trabalho"),
    HOME("Casa"),
    PARK("Parque"),
    AIRPORT("Aeroporto"),
    CAFE("Café"),
    CUSTOM("Personalizado")
}

data class CamoPlace(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: GeoPoint,
    val category: PlaceCategory = PlaceCategory.CUSTOM,
    val isFavorite: Boolean = false
)

enum class MovementMode(val label: String, val speedKmh: Double, val description: String) {
    STATIC("Ponto Fixo", 0.0, "Mantém o pino imóvel com precisão constante"),
    DRIFT("Oscilação Natural", 1.2, "Pequenos desvios de GPS simulando sinal real parado"),
    WALK("Caminhando", 5.2, "Deslocamento suave a pé em rota circular realista"),
    DRIVE("De Carro", 42.0, "Movimento contínuo em vias simulando trajeto urbano")
}

enum class MockGpsStatus(val title: String, val isActive: Boolean) {
    INACTIVE("Camuflagem Desativada", false),
    ACTIVE_SYSTEM_GPS("GPS do Sistema Camuflado", true),
    ACTIVE_APP_SIMULATION_ONLY("Simulação em Tempo Real Ativa", true),
    NEEDS_DEV_OPTION("Requer App Fictício no Android", false),
    NEEDS_PERMISSION("Permissão de Localização Pendente", false)
}

enum class CardStyleMode(val label: String) {
    LIVE_LOCATION("Tempo Real"),
    PINNED_LOCATION("Local Fixo")
}

enum class CardTheme(val label: String) {
    WHATSAPP_LIGHT("Claro"),
    WHATSAPP_DARK("Escuro")
}

enum class LiveDuration(val label: String, val minutes: Int) {
    MIN_15("15 min", 15),
    HOUR_1("1 hora", 60),
    HOUR_8("8 horas", 480)
}

data class WhatsAppCardConfig(
    val styleMode: CardStyleMode = CardStyleMode.LIVE_LOCATION,
    val theme: CardTheme = CardTheme.WHATSAPP_LIGHT,
    val placeTitle: String = "Shopping Iguatemi São Paulo",
    val placeAddress: String = "Av. Brig. Faria Lima, 2232 - Jardim Paulistano, São Paulo",
    val liveDuration: LiveDuration = LiveDuration.HOUR_1,
    val commentText: String = "",
    val senderName: String = "Você",
    val customTimestamp: String = "",
    val showReadChecks: Boolean = true,
    val avatarUriString: String? = null,
    val includeMapsLinkOnShare: Boolean = true
)

data class CamoSessionState(
    val isCamouflaging: Boolean = false,
    val selectedPlace: CamoPlace,
    val currentCoordinate: GeoPoint,
    val movementMode: MovementMode = MovementMode.DRIFT,
    val headingDegrees: Float = 0f,
    val speedKmh: Double = 0.0,
    val accuracyMeters: Float = 4.5f,
    val elapsedSeconds: Long = 0L,
    val mockGpsStatus: MockGpsStatus = MockGpsStatus.INACTIVE,
    val trailHistory: List<GeoPoint> = emptyList()
)
