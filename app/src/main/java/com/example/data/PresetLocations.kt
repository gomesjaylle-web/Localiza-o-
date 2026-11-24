package com.example.data

import com.example.model.CamoPlace
import com.example.model.GeoPoint
import com.example.model.PlaceCategory

object PresetLocations {
    val defaultPlaces: List<CamoPlace> = listOf(
        CamoPlace(
            id = "preset_paulista",
            name = "Shopping Cidade São Paulo",
            address = "Av. Paulista, 1230 - Bela Vista, São Paulo - SP",
            coordinates = GeoPoint(-23.56310, -46.65425),
            category = PlaceCategory.MALL
        ),
        CamoPlace(
            id = "preset_ibirapuera",
            name = "Parque Ibirapuera",
            address = "Av. Pedro Álvares Cabral - Vila Mariana, São Paulo - SP",
            coordinates = GeoPoint(-23.58742, -46.65763),
            category = PlaceCategory.PARK
        ),
        CamoPlace(
            id = "preset_faria_lima",
            name = "Escritório Faria Lima",
            address = "Av. Brig. Faria Lima, 3477 - Itaim Bibi, São Paulo - SP",
            coordinates = GeoPoint(-23.58672, -46.68195),
            category = PlaceCategory.WORK
        ),
        CamoPlace(
            id = "preset_gru",
            name = "Aeroporto Internacional de Guarulhos",
            address = "Rod. Hélio Smidt, s/n - Cumbica, Guarulhos - SP",
            coordinates = GeoPoint(-23.43557, -46.47306),
            category = PlaceCategory.AIRPORT
        ),
        CamoPlace(
            id = "preset_copacabana",
            name = "Praia de Copacabana - Posto 4",
            address = "Av. Atlântica, 2600 - Copacabana, Rio de Janeiro - RJ",
            coordinates = GeoPoint(-22.97118, -43.18678),
            category = PlaceCategory.PARK
        ),
        CamoPlace(
            id = "preset_leblon",
            name = "Shopping Leblon",
            address = "Av. Afrânio de Melo Franco, 290 - Leblon, Rio de Janeiro - RJ",
            coordinates = GeoPoint(-22.98225, -43.21738),
            category = PlaceCategory.MALL
        ),
        CamoPlace(
            id = "preset_savassi",
            name = "Praça da Savassi",
            address = "Av. Cristóvão Colombo, 400 - Savassi, Belo Horizonte - MG",
            coordinates = GeoPoint(-19.93954, -43.93446),
            category = PlaceCategory.CAFE
        ),
        CamoPlace(
            id = "preset_brasilia",
            name = "Parque da Cidade Sarah Kubitschek",
            address = "Asa Sul, Brasília - DF",
            coordinates = GeoPoint(-15.80045, -47.90734),
            category = PlaceCategory.PARK
        )
    )
}
