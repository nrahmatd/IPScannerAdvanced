package com.codelabs.ipscanneradvanced

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.codelabs.ipscanneradvanced.navigation.BottomNavigationBar
import com.codelabs.ipscanneradvanced.navigation.Navigation
import com.codelabs.ipscanneradvanced.navigation.Screen
import com.codelabs.ipscanneradvanced.ui.ToolbarComponent
import com.codelabs.ipscanneradvanced.ui.theme.IPScannerAdvanceTheme
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tej.androidnetworktools.lib.scanner.NetworkScanner
import tej.androidnetworktools.lib.scanner.Traceroute

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private val configSettings by lazy {
        FirebaseRemoteConfigSettings
            .Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashscreen.setKeepOnScreenCondition { keepSplashScreen }
        lifecycleScope.launch {
            delay(2000)
            keepSplashScreen = false
        }
        enableEdgeToEdge()
        initNetworkScan()
        initFirebase()
        firebaseRemoteConfig()
        firebaseAnalytics()
    }

    private fun initNetworkScan() {
        NetworkScanner.init(this)
        Traceroute.init(this)
    }

    private fun initView(isNeedUpdate: Boolean) {
        setContent {

            val navController = rememberNavController()

            IPScannerAdvanceTheme {
                Scaffold(
                    topBar = {
                        ToolbarComponent {
                            navController.navigate(Screen.About.route)
                        }
                    },
                    containerColor = Color.White,
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) { padding ->
                    Navigation(
                        activity = this,
                        modifier = Modifier.padding(padding),
                        navController = navController,
                        startDestination = if (isNeedUpdate) "update" else "home"
                    ) {
                        firebaseAnalytics.logEvent("on_scan", Bundle().apply {
                            putString("brand", Build.BRAND)
                            putString("manufacturer", Build.MANUFACTURER)
                            putString("model", Build.MODEL)
                        })
                    }
                }
            }
        }
    }

    private fun initFirebase() {
        firebaseAnalytics = Firebase.analytics
        firebaseRemoteConfig = Firebase.remoteConfig
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
    }

    private fun firebaseRemoteConfig() {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val isForceUpdate = firebaseRemoteConfig.getBoolean("force_update_required")
                val currentVersion = firebaseRemoteConfig.getString("current_version")
                val localVersion = getAppVersion(applicationContext)

                if (isForceUpdate)
                    initView(isNeedUpdate = currentVersion > localVersion)
                else
                    initView(isNeedUpdate = false)
            }
        }
    }

    private fun firebaseAnalytics() {
        lifecycleScope.launch {
            val adId = getAdvertisingId(applicationContext)

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, this@MainActivity.javaClass.simpleName)
            })

            adId?.let {
                firebaseAnalytics.logEvent("device_info", Bundle().apply {
                    putString("advertisingId", it)
                    putString("brand", Build.BRAND)
                    putString("manufacturer", Build.MANUFACTURER)
                    putString("model", Build.MODEL)
                })
            }
        }
    }

    private suspend fun getAdvertisingId(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                adInfo.id
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
