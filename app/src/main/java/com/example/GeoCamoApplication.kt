package com.example

import android.app.Application
import com.example.data.CamoPreferencesRepository
import com.example.service.CamouflageLocationEngine

class GeoCamoApplication : Application() {
    lateinit var preferencesRepository: CamoPreferencesRepository
        private set

    lateinit var camouflageEngine: CamouflageLocationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = CamoPreferencesRepository(this)
        camouflageEngine = CamouflageLocationEngine(this)
    }
}
