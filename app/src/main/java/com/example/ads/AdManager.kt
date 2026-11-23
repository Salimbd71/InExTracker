package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {
    private const val TAG = "AdManager"

    // Production Ad Unit IDs
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-3239967742299196/8831544140"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3239967742299196/6105308459"

    // Google Official Test Ad Unit IDs (used automatically in Debug builds)
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var retryAttempt = 0
    private var lastInterstitialShownAt = 0L
    private var actionCounter = 0

    private var rewardedAd: RewardedAd? = null

    private val _isAdsEnabled = MutableStateFlow(true)
    val isAdsEnabled: StateFlow<Boolean> = _isAdsEnabled.asStateFlow()

    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        if (isInitialized) {
            if (interstitialAd == null) {
                loadInterstitialAd(appContext)
            }
            return
        }
        try {
            MobileAds.initialize(appContext) {
                isInitialized = true
                Log.d(TAG, "AdMob initialized successfully (debug=${BuildConfig.DEBUG})")
                loadInterstitialAd(appContext)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize AdMob: ${e.message}")
        }
    }

    fun setAdsEnabled(enabled: Boolean) {
        _isAdsEnabled.value = enabled
    }

    fun loadInterstitialAd(context: Context) {
        if (!_isAdsEnabled.value || isInterstitialLoading || interstitialAd != null) return
        val appContext = context.applicationContext
        isInterstitialLoading = true
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                appContext,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        retryAttempt = 0
                        Log.d(TAG, "Interstitial ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.w(TAG, "Interstitial ad failed to load (code=${error.code}): ${error.message}")
                        if (retryAttempt < 3) {
                            retryAttempt++
                            val delayMs = retryAttempt * 15_000L
                            mainHandler.postDelayed({ loadInterstitialAd(appContext) }, delayMs)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            isInterstitialLoading = false
            Log.w(TAG, "Error loading interstitial ad: ${e.message}")
        }
    }

    /**
     * Shows interstitial ad immediately if loaded (e.g. after user clicks PDF export),
     * otherwise runs [onAdClosed] immediately and preloads for next time.
     */
    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit = {}) {
        if (!_isAdsEnabled.value || activity.isFinishing || activity.isDestroyed) {
            onAdClosed()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitialAd(activity.applicationContext)
            onAdClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = System.currentTimeMillis()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd(activity.applicationContext)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAd(activity.applicationContext)
                onAdClosed()
            }
        }

        ad.show(activity)
    }

    /**
     * Respectful frequency-capped interstitial after saving an entry (every 2 saves, min 60s cooldown).
     */
    fun maybeShowInterstitialAfterSave(activity: Activity?) {
        if (activity == null || !_isAdsEnabled.value) return
        actionCounter++
        val now = System.currentTimeMillis()
        val cooldownElapsed = (now - lastInterstitialShownAt) >= 60_000L
        if (actionCounter >= 2 && cooldownElapsed && interstitialAd != null) {
            actionCounter = 0
            showInterstitial(activity)
        } else if (interstitialAd == null) {
            loadInterstitialAd(activity.applicationContext)
        }
    }

    fun loadRewardedAd(context: Context) {
        if (!_isAdsEnabled.value) return
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context.applicationContext,
                TEST_REWARDED_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        Log.d(TAG, "Rewarded ad loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error loading rewarded ad: ${e.message}")
        }
    }

    fun showRewarded(activity: Activity, onUserEarnedReward: () -> Unit) {
        if (!_isAdsEnabled.value || rewardedAd == null) {
            onUserEarnedReward()
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd(activity.applicationContext)
            }
        }

        rewardedAd?.show(activity) {
            onUserEarnedReward()
        }
    }
}
