package com.example.zainqhchat.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * يحمّل ويعرض إعلان مكافأة (Rewarded) حقيقي من AdMob. المكافأة الفعلية
 * (استدعاء RPC الخاص بالخادم) لا تُمنَح إلا من داخل [onUserEarnedReward] —
 * أي بعد تأكيد Google نفسها أن المستخدم شاهد الإعلان كاملاً، وليس بمجرد
 * فتحه أو الضغط على الزر.
 */
class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            AdConfig.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * يعرض الإعلان إن كان جاهزًا. [onEarned] يُستدعى فقط إذا شاهد المستخدم
     * الإعلان فعليًا حتى النهاية (وليس عند مجرد الإغلاق المبكر).
     * [onUnavailable] يُستدعى إن لم يوجد إعلان جاهز بعد (مثلاً لضعف الإنترنت).
     */
    fun show(activity: Activity, onEarned: () -> Unit, onUnavailable: () -> Unit, onClosed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            preload()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                preload()
                onUnavailable()
            }
        }

        ad.show(activity) { onEarned() }
    }

    companion object {
        private const val TAG = "RewardedAdManager"
    }
}
