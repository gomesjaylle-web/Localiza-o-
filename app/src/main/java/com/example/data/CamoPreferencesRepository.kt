package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CamoPlace
import com.example.model.CardStyleMode
import com.example.model.CardTheme
import com.example.model.GeoPoint
import com.example.model.LiveDuration
import com.example.model.MovementMode
import com.example.model.PlaceCategory
import com.example.model.WhatsAppCardConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geocamo_prefs")

class CamoPreferencesRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val placeListType = Types.newParameterizedType(List::class.java, CamoPlace::class.java)
    private val placeListAdapter = moshi.adapter<List<CamoPlace>>(placeListType)

    private object Keys {
        val FAVORITE_PLACES = stringPreferencesKey("favorite_places_json")
        val LAST_PLACE_NAME = stringPreferencesKey("last_place_name")
        val LAST_PLACE_ADDRESS = stringPreferencesKey("last_place_address")
        val LAST_LAT = doublePreferencesKey("last_lat")
        val LAST_LNG = doublePreferencesKey("last_lng")
        val MOVEMENT_MODE = stringPreferencesKey("movement_mode")

        val CARD_STYLE = stringPreferencesKey("card_style")
        val CARD_THEME = stringPreferencesKey("card_theme")
        val CARD_TITLE = stringPreferencesKey("card_title")
        val CARD_ADDRESS = stringPreferencesKey("card_address")
        val CARD_DURATION = stringPreferencesKey("card_duration")
        val CARD_COMMENT = stringPreferencesKey("card_comment")
        val CARD_TIMESTAMP = stringPreferencesKey("card_timestamp")
        val CARD_CHECKS = booleanPreferencesKey("card_checks")
        val CARD_AVATAR_URI = stringPreferencesKey("card_avatar_uri")
        val CARD_INCLUDE_LINK = booleanPreferencesKey("card_include_link")
    }

    val favoritePlacesFlow: Flow<List<CamoPlace>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.FAVORITE_PLACES]
        if (json.isNullOrBlank()) {
            PresetLocations.defaultPlaces.take(3).map { it.copy(isFavorite = true) }
        } else {
            runCatching { placeListAdapter.fromJson(json) }.getOrNull().orEmpty()
        }
    }

    val lastSelectedPlaceFlow: Flow<CamoPlace> = context.dataStore.data.map { prefs ->
        val default = PresetLocations.defaultPlaces.first()
        val lat = prefs[Keys.LAST_LAT] ?: default.coordinates.latitude
        val lng = prefs[Keys.LAST_LNG] ?: default.coordinates.longitude
        val name = prefs[Keys.LAST_PLACE_NAME] ?: default.name
        val addr = prefs[Keys.LAST_PLACE_ADDRESS] ?: default.address
        CamoPlace(
            id = "last_selected",
            name = name,
            address = addr,
            coordinates = GeoPoint(lat, lng),
            category = PlaceCategory.CUSTOM
        )
    }

    val movementModeFlow: Flow<MovementMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.MOVEMENT_MODE] ?: MovementMode.DRIFT.name
        runCatching { MovementMode.valueOf(raw) }.getOrDefault(MovementMode.DRIFT)
    }

    val cardConfigFlow: Flow<WhatsAppCardConfig> = context.dataStore.data.map { prefs ->
        val default = PresetLocations.defaultPlaces.first()
        WhatsAppCardConfig(
            styleMode = runCatching {
                CardStyleMode.valueOf(prefs[Keys.CARD_STYLE] ?: CardStyleMode.LIVE_LOCATION.name)
            }.getOrDefault(CardStyleMode.LIVE_LOCATION),
            theme = runCatching {
                CardTheme.valueOf(prefs[Keys.CARD_THEME] ?: CardTheme.WHATSAPP_LIGHT.name)
            }.getOrDefault(CardTheme.WHATSAPP_LIGHT),
            placeTitle = prefs[Keys.CARD_TITLE] ?: default.name,
            placeAddress = prefs[Keys.CARD_ADDRESS] ?: default.address,
            liveDuration = runCatching {
                LiveDuration.valueOf(prefs[Keys.CARD_DURATION] ?: LiveDuration.HOUR_1.name)
            }.getOrDefault(LiveDuration.HOUR_1),
            commentText = prefs[Keys.CARD_COMMENT] ?: "",
            customTimestamp = prefs[Keys.CARD_TIMESTAMP] ?: "",
            showReadChecks = prefs[Keys.CARD_CHECKS] ?: true,
            avatarUriString = prefs[Keys.CARD_AVATAR_URI],
            includeMapsLinkOnShare = prefs[Keys.CARD_INCLUDE_LINK] ?: true
        )
    }

    suspend fun saveSelectedPlace(place: CamoPlace) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_PLACE_NAME] = place.name
            prefs[Keys.LAST_PLACE_ADDRESS] = place.address
            prefs[Keys.LAST_LAT] = place.coordinates.latitude
            prefs[Keys.LAST_LNG] = place.coordinates.longitude
        }
    }

    suspend fun saveMovementMode(mode: MovementMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MOVEMENT_MODE] = mode.name
        }
    }

    suspend fun saveFavoritePlaces(places: List<CamoPlace>) {
        val json = placeListAdapter.toJson(places)
        context.dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_PLACES] = json
        }
    }

    suspend fun saveCardConfig(config: WhatsAppCardConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CARD_STYLE] = config.styleMode.name
            prefs[Keys.CARD_THEME] = config.theme.name
            prefs[Keys.CARD_TITLE] = config.placeTitle
            prefs[Keys.CARD_ADDRESS] = config.placeAddress
            prefs[Keys.CARD_DURATION] = config.liveDuration.name
            prefs[Keys.CARD_COMMENT] = config.commentText
            prefs[Keys.CARD_TIMESTAMP] = config.customTimestamp
            prefs[Keys.CARD_CHECKS] = config.showReadChecks
            if (config.avatarUriString != null) {
                prefs[Keys.CARD_AVATAR_URI] = config.avatarUriString
            } else {
                prefs.remove(Keys.CARD_AVATAR_URI)
            }
            prefs[Keys.CARD_INCLUDE_LINK] = config.includeMapsLinkOnShare
        }
    }
}
