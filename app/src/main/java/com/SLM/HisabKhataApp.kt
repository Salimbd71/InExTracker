package com.SLM

import android.app.Application
import android.util.Log
import com.SLM.ads.AdManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class HisabKhataApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebaseSafely()
        initRealtimeDatabaseSafely()
        AdManager.initialize(this)
    }

    private fun initRealtimeDatabaseSafely() {
        try {
            val db = try {
                FirebaseDatabase.getInstance()
            } catch (e: Throwable) {
                FirebaseDatabase.getInstance("https://inex-tracker-c3ecb-default-rtdb.asia-southeast1.firebasedatabase.app")
            }
            db.setPersistenceEnabled(true)
        } catch (e: Throwable) {
            Log.w("HisabKhataApp", "FirebaseDatabase persistence config: ${e.message}")
        }
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Throwable) {
                    null
                }
                if (app == null) {
                    val fallbackOptions = FirebaseOptions.Builder()
                        .setApplicationId("1:908865541753:android:c8284b10c7facc9f3126a8")
                        .setApiKey("AIzaSyDummyKeyForLocalOfflineFallbackMode")
                        .setProjectId("inex-tracker-c3ecb")
                        .setDatabaseUrl("https://inex-tracker-c3ecb-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .build()
                    FirebaseApp.initializeApp(this, fallbackOptions)
                }
            }
        } catch (e: Throwable) {
            Log.w("HisabKhataApp", "Firebase auto-init skipped or fallback used: ${e.message}")
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    val fallbackOptions = FirebaseOptions.Builder()
                        .setApplicationId("1:908865541753:android:c8284b10c7facc9f3126a8")
                        .setApiKey("AIzaSyDummyKeyForLocalOfflineFallbackMode")
                        .setProjectId("inex-tracker-c3ecb")
                        .setDatabaseUrl("https://inex-tracker-c3ecb-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .build()
                    FirebaseApp.initializeApp(this, fallbackOptions)
                }
            } catch (e2: Throwable) {
                Log.e("HisabKhataApp", "Firebase init error: ${e2.message}")
            }
        }
    }
}
