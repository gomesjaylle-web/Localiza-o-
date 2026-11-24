package com.example.data

import com.example.model.CamoPlace
import com.example.model.GeoPoint
import com.example.model.PlaceCategory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimResult(
    @Json(name = "place_id") val placeId: Long,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "lat") val lat: String,
    @Json(name = "lon") val lon: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class NominatimReverseResult(
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "name") val name: String? = null,
    @Json(name = "address") val address: NominatimAddress? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddress(
    @Json(name = "road") val road: String? = null,
    @Json(name = "house_number") val houseNumber: String? = null,
    @Json(name = "suburb") val suburb: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "town") val town: String? = null,
    @Json(name = "state") val state: String? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 8,
        @Query("accept-language") language: String = "pt-BR"
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("accept-language") language: String = "pt-BR"
    ): NominatimReverseResult
}

object GeocodingClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "GeoCamoAndroid/1.0 (privacy-utility)")
                .build()
            chain.proceed(request)
        })
        .build()

    val api: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApi::class.java)
    }

    suspend fun search(query: String): List<CamoPlace> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val localMatches = PresetLocations.defaultPlaces.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.address.contains(trimmed, ignoreCase = true)
        }
        return try {
            val remote = api.searchPlaces(trimmed).mapNotNull { item ->
                val lat = item.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lon = item.lon.toDoubleOrNull() ?: return@mapNotNull null
                val parts = item.displayName.split(",").map { it.trim() }
                val shortTitle = item.name?.takeIf { it.isNotBlank() } ?: parts.firstOrNull().orEmpty()
                val shortAddress = parts.drop(1).take(3).joinToString(", ").ifBlank { item.displayName }
                CamoPlace(
                    id = "osm_${item.placeId}",
                    name = shortTitle.ifBlank { "Local Selecionado" },
                    address = shortAddress,
                    coordinates = GeoPoint(lat, lon),
                    category = PlaceCategory.CUSTOM
                )
            }
            (remote + localMatches).distinctBy { it.name to it.coordinates.formatted(3) }
        } catch (_: Exception) {
            localMatches
        }
    }

    suspend fun reverse(point: GeoPoint): Pair<String, String> {
        return try {
            val res = api.reverseGeocode(point.latitude, point.longitude)
            val addr = res.address
            val roadPart = listOfNotNull(addr?.road, addr?.houseNumber).joinToString(", ")
            val cityPart = addr?.city ?: addr?.town ?: addr?.suburb ?: addr?.state
            val title = res.name?.takeIf { it.isNotBlank() }
                ?: roadPart.takeIf { it.isNotBlank() }
                ?: "Ponto Personalizado"
            val subtitle = listOfNotNull(
                roadPart.takeIf { it.isNotBlank() && it != title },
                addr?.suburb,
                cityPart,
                addr?.state
            ).distinct().joinToString(" - ").ifBlank {
                res.displayName ?: point.formatted(5)
            }
            title to subtitle
        } catch (_: Exception) {
            "Local Camuflado" to "Coord: ${point.formatted(5)}"
        }
    }
}
