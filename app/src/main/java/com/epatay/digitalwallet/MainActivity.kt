package com.epatay.digitalwallet

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieCompositionFactory
import com.epatay.digitalwallet.databinding.ActivityMainBinding
import com.epatay.digitalwallet.recurring.RecurringTransactionScheduler
import com.epatay.digitalwallet.ui.ViewPagerAdapter
import com.github.mikephil.charting.BuildConfig
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : AppCompatActivity() {

    companion object {

        /*
         * Lottie bölümü 1800 ms,
         * kapanış geçişi 200 ms sürer.
         *
         * Toplam intro süresi yaklaşık 2 saniyedir.
         */
        private const val INTRO_LOTTIE_DURATION_MS =
            3000L

        private const val INTRO_FADE_DURATION_MS =
            300L

        private const val BRAND_REVEAL_PROGRESS =
            0.52f

        private const val BRAND_ANIMATION_DURATION_MS =
            420L

        private const val SYSTEM_SPLASH_EXIT_DURATION_MS =
            70L

        private const val INTRO_LOAD_TIMEOUT_MS =
            3000L

        private const val INTRO_FALLBACK_TIMEOUT_MS =
            3900L

        /*
         * Yalnızca DEBUG derlemesinde Türkiye'deki cihazı
         * Avrupa bölgesindeymiş gibi test eder.
         *
         * Release derlemesine hiçbir etkisi yoktur.
         */
        private const val FORCE_EEA_CONSENT_TEST =
            false

        private const val UMP_TEST_DEVICE_HASH =
            "B3EEABB8EE11C2BE770B684D95219ECB"

        /*
         * Debug/emülatör derlemelerinde Google'ın test reklamı kullanılır.
         */
        private const val TEST_BANNER_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/9214589741"

        /*
         * Release derlemesinde kullanılacak gerçek VarlıkCep banner kimliği.
         */
        private const val LIVE_BANNER_AD_UNIT_ID =
            "ca-app-pub-1209621045102488/5586741853"

        private const val ADS_LOG_TAG =
            "VARLIKCEP_ADS"

        private const val CONSENT_LOG_TAG =
            "VARLIKCEP_CONSENT"
    }

    private lateinit var binding:
            ActivityMainBinding

    private var bannerAdView:
            AdView? = null

    private lateinit var consentInformation:
            ConsentInformation

    private var mobileAdsInitialized =
        false

    private var bannerLoadStarted =
        false

    private val bannerAdUnitId:
            String
        get() =
            if (BuildConfig.DEBUG) {
                TEST_BANNER_AD_UNIT_ID
            } else {
                LIVE_BANNER_AD_UNIT_ID
            }

    private var timeReceiverRegistered =
        false

    private var timeRefreshPending =
        false

    private var introBrandRevealed =
        false

    private var introFinished =
        false

    private var systemSplashExited =
        false

    private var introCompositionReady =
        false

    private var introStarted =
        false

    private var introPlaybackSpeed =
        1f

    /*
     * UMP, AdMob ve WorkManager işlemleri intro animasyonu bitene
     * kadar başlatılmaz. Böylece açılış animasyonu bu ağır işlerle
     * aynı anda çalışmaz.
     */
    private var postIntroTasksStarted =
        false

    private var activityCurrentlyResumed =
        false

    private val introLoadTimeoutRunnable =
        Runnable {

            if (
                ::binding.isInitialized &&
                !introCompositionReady &&
                !introFinished
            ) {
                finishIntroImmediately()
            }
        }

    private val introFallbackRunnable =
        Runnable {

            if (
                ::binding.isInitialized &&
                !introFinished
            ) {
                finishIntroAnimation()
            }
        }

    private val timeChangeReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context,
                intent: Intent
            ) {

                RecurringTransactionScheduler.runNow(
                    context
                )

                if (!timeRefreshPending) {

                    timeRefreshPending =
                        true

                    binding.root.post {

                        timeRefreshPending =
                            false

                        if (
                            !isFinishing &&
                            !isDestroyed
                        ) {
                            recreate()
                        }
                    }
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        val isFreshLaunch =
            savedInstanceState == null

        val splashScreen =
            installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding =
            ActivityMainBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        binding.btnPrivacyOptions.apply {

            visibility =
                View.GONE

            setOnClickListener {
                showPrivacyOptionsForm()
            }
        }

        /*
         * Intro hazırlığını ilk sırada başlatıyoruz.
         * Böylece sistem splash logosu gereksiz
         * şekilde uzun süre ekranda kalmaz.
         */
        prepareIntroAnimation(
            splashScreen = splashScreen,
            isFreshLaunch = isFreshLaunch
        )

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.mainContent
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        configureSwipeHint(
            isFreshLaunch
        )

        setupViewPager(
            isFreshLaunch
        )
    }

    override fun onResume() {

        super.onResume()

        activityCurrentlyResumed =
            true

        /*
         * İlk açılışta intro bitmeden arka plan işi başlatılmaz.
         * Sonraki dönüşlerde ise yinelenen işlemler kontrol edilir.
         */
        if (postIntroTasksStarted) {

            RecurringTransactionScheduler.runNow(
                this
            )
        }

        bannerAdView?.resume()
    }

    override fun onPause() {

        activityCurrentlyResumed =
            false

        bannerAdView?.pause()

        super.onPause()
    }

    override fun onStart() {

        super.onStart()

        if (!timeReceiverRegistered) {

            ContextCompat.registerReceiver(
                this,
                timeChangeReceiver,
                IntentFilter().apply {

                    addAction(
                        Intent.ACTION_DATE_CHANGED
                    )

                    addAction(
                        Intent.ACTION_TIME_CHANGED
                    )

                    addAction(
                        Intent.ACTION_TIMEZONE_CHANGED
                    )
                },
                ContextCompat.RECEIVER_EXPORTED
            )

            timeReceiverRegistered =
                true
        }
    }

    override fun onStop() {

        if (timeReceiverRegistered) {

            unregisterReceiver(
                timeChangeReceiver
            )

            timeReceiverRegistered =
                false
        }

        super.onStop()
    }

    override fun onDestroy() {

        if (::binding.isInitialized) {

            binding.root.removeCallbacks(
                introLoadTimeoutRunnable
            )

            binding.introOverlay.removeCallbacks(
                introFallbackRunnable
            )

            binding.lottieIntro.cancelAnimation()
        }

        bannerLoadStarted =
            false

        destroyBannerAd()

        super.onDestroy()
    }

    private fun startPostIntroTasks() {

        if (
            postIntroTasksStarted ||
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        postIntroTasksStarted =
            true

        Log.d(
            ADS_LOG_TAG,
            "Intro tamamlandı; izin, reklam ve arka plan işlemleri başlatılıyor."
        )

        /*
         * Periyodik işlem planlaması artık animasyonla aynı anda
         * başlamaz.
         */
        RecurringTransactionScheduler.schedule(
            this
        )

        /*
         * UMP kontrolü ve ardından AdMob başlatma akışı.
         */
        requestUserConsent()

        /*
         * Activity hâlen ekrandaysa yinelenen işlemleri hemen kontrol et.
         * onResume introdan önce çalıştıysa bu satır ilk kontrolü sağlar.
         */
        if (activityCurrentlyResumed) {

            RecurringTransactionScheduler.runNow(
                this
            )
        }
    }

    private fun requestUserConsent() {

        consentInformation =
            UserMessagingPlatform.getConsentInformation(
                this
            )

        val consentRequestBuilder =
            ConsentRequestParameters
                .Builder()

        if (
            BuildConfig.DEBUG &&
            FORCE_EEA_CONSENT_TEST
        ) {

            /*
             * Önceki test seçimini sıfırlar. Bu işlem yalnızca debug
             * derlemesinde çalışır ve formun yeniden test edilmesini sağlar.
             */
            consentInformation.reset()

            val consentDebugSettings =
                ConsentDebugSettings
                    .Builder(this)
                    .setDebugGeography(
                        ConsentDebugSettings
                            .DebugGeography
                            .DEBUG_GEOGRAPHY_EEA
                    )
                    .addTestDeviceHashedId(
                        UMP_TEST_DEVICE_HASH
                    )
                    .build()

            consentRequestBuilder
                .setConsentDebugSettings(
                    consentDebugSettings
                )

            Log.d(
                CONSENT_LOG_TAG,
                "DEBUG UMP testi aktif: cihaz EEA bölgesinde gösterilecek."
            )
        }

        val consentRequestParameters =
            consentRequestBuilder.build()

        Log.d(
            CONSENT_LOG_TAG,
            "Kullanıcı izin bilgisi güncelleniyor."
        )

        /*
         * Google, izin bilgisinin her uygulama açılışında
         * güncellenmesini ister.
         */
        consentInformation.requestConsentInfoUpdate(
            this,
            consentRequestParameters,
            {
                Log.d(
                    CONSENT_LOG_TAG,
                    "İzin bilgisi güncellendi. " +
                            "status=${consentInformation.consentStatus}, " +
                            "canRequestAds=${consentInformation.canRequestAds()}, " +
                            "privacyOptions=" +
                            consentInformation
                                .privacyOptionsRequirementStatus
                )

                updatePrivacyOptionsButton()

                UserMessagingPlatform
                    .loadAndShowConsentFormIfRequired(
                        this
                    ) { formError ->

                        if (formError != null) {

                            Log.e(
                                CONSENT_LOG_TAG,
                                "İzin formu tamamlanamadı. " +
                                        "code=${formError.errorCode}, " +
                                        "message=${formError.message}"
                            )

                        } else {

                            Log.d(
                                CONSENT_LOG_TAG,
                                "İzin formu işlemi tamamlandı."
                            )
                        }

                        /*
                         * Form gösterilmiş, gerekmemiş veya hata vermiş
                         * olabilir. Her durumda UMP'nin güncel kararını
                         * canRequestAds() üzerinden kontrol ediyoruz.
                         */
                        updatePrivacyOptionsButton()

                        requestAdsIfAllowed()
                    }
            },
            { requestConsentError ->

                Log.e(
                    CONSENT_LOG_TAG,
                    "İzin bilgisi güncellenemedi. " +
                            "code=${requestConsentError.errorCode}, " +
                            "message=${requestConsentError.message}"
                )

                /*
                 * Güncelleme hatasında UMP, önceki oturumdaki geçerli
                 * izin durumunu kullanabilir.
                 */
                updatePrivacyOptionsButton()

                requestAdsIfAllowed()
            }
        )

        /*
         * Önceki uygulama oturumundan geçerli izin varsa,
         * form sürecini beklemeden reklam başlatılabilir.
         * mobileAdsInitialized değişkeni çift başlatmayı önler.
         */
        requestAdsIfAllowed()
    }

    private fun updatePrivacyOptionsButton() {

        if (!::consentInformation.isInitialized) {

            binding.btnPrivacyOptions.visibility =
                View.GONE

            return
        }

        val privacyOptionsRequired =
            consentInformation
                .privacyOptionsRequirementStatus ==
                    ConsentInformation
                        .PrivacyOptionsRequirementStatus
                        .REQUIRED

        binding.btnPrivacyOptions.visibility =
            if (privacyOptionsRequired) {

                View.VISIBLE

            } else {

                View.GONE
            }

        Log.d(
            CONSENT_LOG_TAG,
            "Gizlilik seçenekleri düğmesi: " +
                    if (privacyOptionsRequired) {
                        "gösteriliyor"
                    } else {
                        "gizli"
                    }
        )
    }

    private fun showPrivacyOptionsForm() {

        if (!::consentInformation.isInitialized) {

            Log.e(
                CONSENT_LOG_TAG,
                "Gizlilik seçenekleri açılamadı: " +
                        "izin bilgisi henüz hazırlanmadı."
            )

            return
        }

        UserMessagingPlatform
            .showPrivacyOptionsForm(
                this
            ) { formError ->

                if (formError != null) {

                    Log.e(
                        CONSENT_LOG_TAG,
                        "Gizlilik seçenekleri formu açılamadı. " +
                                "code=${formError.errorCode}, " +
                                "message=${formError.message}"
                    )

                } else {

                    Log.d(
                        CONSENT_LOG_TAG,
                        "Gizlilik tercihleri güncellendi."
                    )
                }

                updatePrivacyOptionsButton()

                reloadBannerAfterPrivacySelection()
            }
    }

    private fun reloadBannerAfterPrivacySelection() {

        destroyBannerAd()

        bannerLoadStarted =
            false

        if (!consentInformation.canRequestAds()) {

            binding.adViewContainer.visibility =
                View.GONE

            Log.d(
                CONSENT_LOG_TAG,
                "Yeni tercihten sonra reklam isteğine izin verilmiyor."
            )

            return
        }

        if (mobileAdsInitialized) {

            bannerLoadStarted =
                true

            binding.adViewContainer.post {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {
                    createAndLoadBannerAd()
                }
            }

        } else {

            initializeMobileAdsOnce()
        }
    }

    private fun requestAdsIfAllowed() {

        if (
            !::consentInformation.isInitialized ||
            !consentInformation.canRequestAds()
        ) {

            Log.d(
                CONSENT_LOG_TAG,
                "Henüz reklam isteği yapılamaz."
            )

            return
        }

        initializeMobileAdsOnce()
    }

    private fun initializeMobileAdsOnce() {

        if (mobileAdsInitialized) {

            Log.d(
                ADS_LOG_TAG,
                "AdMob SDK daha önce başlatıldı."
            )

            return
        }

        mobileAdsInitialized =
            true

        Log.d(
            ADS_LOG_TAG,
            "AdMob SDK başlatılıyor. " +
                    "build=${if (BuildConfig.DEBUG) "DEBUG/TEST" else "RELEASE/CANLI"}"
        )

        Thread {

            MobileAds.initialize(
                this
            ) { initializationStatus ->

                Log.d(
                    ADS_LOG_TAG,
                    "AdMob SDK hazır: $initializationStatus"
                )

                runOnUiThread {

                    if (
                        !isFinishing &&
                        !isDestroyed &&
                        !bannerLoadStarted
                    ) {

                        bannerLoadStarted =
                            true

                        binding.adViewContainer.post {
                            createAndLoadBannerAd()
                        }
                    }
                }
            }
        }.start()
    }

    private fun createAndLoadBannerAd() {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        destroyBannerAd()

        /*
         * INVISIBLE kullanıyoruz:
         * Alan ölçülebilir fakat reklam yüklenene kadar görünmez.
         */
        binding.adViewContainer.visibility =
            View.INVISIBLE

        binding.adViewContainer.post {

            if (
                isFinishing ||
                isDestroyed
            ) {
                return@post
            }

            val containerWidthPixels =
                binding.adViewContainer.width

            if (containerWidthPixels <= 0) {

                Log.e(
                    ADS_LOG_TAG,
                    "Banner alanının genişliği ölçülemedi."
                )

                binding.adViewContainer.visibility =
                    View.GONE

                return@post
            }

            val density =
                resources.displayMetrics.density

            val containerWidthDp =
                (containerWidthPixels / density)
                    .toInt()
                    .coerceAtLeast(1)

            Log.d(
                ADS_LOG_TAG,
                "Adaptive banner genişliği: " +
                        "$containerWidthDp dp"
            )

            val adaptiveAdSize =
                AdSize
                    .getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        this@MainActivity,
                        containerWidthDp
                    )

            val newAdView =
                AdView(this@MainActivity)

            newAdView.adUnitId =
                bannerAdUnitId

            newAdView.setAdSize(
                adaptiveAdSize
            )

            newAdView.adListener =
                object : AdListener() {

                    override fun onAdLoaded() {

                        Log.d(
                            ADS_LOG_TAG,
                            "Adaptive banner başarıyla yüklendi."
                        )

                        if (
                            !isFinishing &&
                            !isDestroyed &&
                            bannerAdView === newAdView
                        ) {
                            binding.adViewContainer.visibility =
                                View.VISIBLE
                        }
                    }

                    override fun onAdFailedToLoad(
                        adError: LoadAdError
                    ) {

                        Log.e(
                            ADS_LOG_TAG,
                            "Banner yüklenemedi. " +
                                    "code=${adError.code}, " +
                                    "domain=${adError.domain}, " +
                                    "message=${adError.message}"
                        )

                        if (
                            !isFinishing &&
                            !isDestroyed &&
                            bannerAdView === newAdView
                        ) {
                            binding.adViewContainer.visibility =
                                View.GONE
                        }
                    }

                    override fun onAdImpression() {

                        Log.d(
                            ADS_LOG_TAG,
                            "Banner gösterimi kaydedildi."
                        )
                    }
                }

            bannerAdView =
                newAdView

            binding.adViewContainer.apply {

                removeAllViews()

                addView(
                    newAdView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            val adRequest =
                AdRequest
                    .Builder()
                    .build()

            Log.d(
                ADS_LOG_TAG,
                "Banner yükleniyor. " +
                        "unitId=$bannerAdUnitId"
            )

            newAdView.loadAd(
                adRequest
            )
        }
    }

    private fun destroyBannerAd() {

        val currentAdView =
            bannerAdView
                ?: return

        val parent =
            currentAdView.parent

        if (parent is ViewGroup) {
            parent.removeView(
                currentAdView
            )
        }

        currentAdView.destroy()

        bannerAdView =
            null

        if (::binding.isInitialized) {
            binding.adViewContainer.visibility =
                View.GONE
        }
    }

    private fun prepareIntroAnimation(
        splashScreen: SplashScreen,
        isFreshLaunch: Boolean
    ) {

        if (!isFreshLaunch) {

            finishIntroImmediately()

            return
        }

        introFinished =
            false

        introBrandRevealed =
            false

        introStarted =
            false

        systemSplashExited =
            false

        introCompositionReady =
            false

        binding.introOverlay.apply {

            visibility =
                View.VISIBLE

            alpha =
                1f
        }

        /*
         * İlk anda ikon ekranın ortasında bulunur.
         * Yazı gelirken grup normal konumuna kayar.
         */
        binding.introBrandContainer.translationX =
            dpToPx(68f)

        binding.brandWordmark.apply {

            alpha =
                0f

            translationX =
                dpToPx(22f)
        }

        /*
         * Android'in sabit sistem logosunu
         * çok kısa bir geçişle kaldırıyoruz.
         */
        splashScreen.setOnExitAnimationListener {
                splashProvider ->

            splashProvider.view
                .animate()
                .alpha(0f)
                .setDuration(
                    SYSTEM_SPLASH_EXIT_DURATION_MS
                )
                .withEndAction {

                    splashProvider.remove()

                    systemSplashExited =
                        true

                    tryStartIntroAnimation()
                }
                .start()
        }

        /*
         * Lottie dosyasını belleğe yüklüyoruz.
         */
        LottieCompositionFactory
            .fromRawRes(
                this,
                R.raw.varlikcep_intro
            )
            .addListener { composition ->

                binding.lottieIntro.setComposition(
                    composition
                )

                /*
                 * JSON animasyonunun doğal süresi
                 * ne olursa olsun Lottie kısmını
                 * 1800 milisaniyeye ayarlar.
                 */
                introPlaybackSpeed =
                    if (composition.duration > 0f) {

                        composition.duration /
                                INTRO_LOTTIE_DURATION_MS
                                    .toFloat()

                    } else {

                        1f
                    }

                introCompositionReady =
                    true

                binding.root.removeCallbacks(
                    introLoadTimeoutRunnable
                )

                tryStartIntroAnimation()
            }
            .addFailureListener {

                finishIntroImmediately()
            }

        /*
         * Dosya yüklenemezse uygulama boş
         * intro ekranında takılı kalmaz.
         */
        binding.root.postDelayed(
            introLoadTimeoutRunnable,
            INTRO_LOAD_TIMEOUT_MS
        )
    }

    private fun tryStartIntroAnimation() {

        if (
            introFinished ||
            introStarted ||
            !systemSplashExited ||
            !introCompositionReady
        ) {
            return
        }

        introStarted =
            true

        startIntroAnimation()
    }

    private fun startIntroAnimation() {

        binding.lottieIntro.apply {

            progress =
                0f

            speed =
                introPlaybackSpeed

            repeatCount =
                0

            removeAllUpdateListeners()
            removeAllAnimatorListeners()

            addAnimatorUpdateListener {
                    animator ->

                if (
                    !introBrandRevealed &&
                    animator.animatedFraction >=
                    BRAND_REVEAL_PROGRESS
                ) {
                    revealBrandName()
                }
            }

            addAnimatorListener(
                object :
                    AnimatorListenerAdapter() {

                    override fun onAnimationEnd(
                        animation: Animator
                    ) {

                        revealBrandName()

                        finishIntroAnimation()
                    }
                }
            )

            playAnimation()
        }

        /*
         * Beklenmeyen bir Lottie problemi olursa
         * intro ekranını güvenli biçimde kapatır.
         */
        binding.introOverlay.postDelayed(
            introFallbackRunnable,
            INTRO_FALLBACK_TIMEOUT_MS
        )
    }

    private fun revealBrandName() {

        if (introBrandRevealed) {
            return
        }

        introBrandRevealed =
            true

        /*
         * İkon sola yerleşir.
         */
        binding.introBrandContainer
            .animate()
            .translationX(0f)
            .setDuration(
                BRAND_ANIMATION_DURATION_MS
            )
            .setInterpolator(
                DecelerateInterpolator()
            )
            .start()

        /*
         * VarlıkCep yazısı sağdan belirir.
         */
        binding.brandWordmark
            .animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(
                BRAND_ANIMATION_DURATION_MS
            )
            .setInterpolator(
                DecelerateInterpolator()
            )
            .start()
    }

    private fun finishIntroAnimation() {

        if (introFinished) {
            return
        }

        introFinished =
            true

        binding.root.removeCallbacks(
            introLoadTimeoutRunnable
        )

        binding.introOverlay.removeCallbacks(
            introFallbackRunnable
        )

        /*
         * 1800 ms Lottie + 200 ms fade
         * yaklaşık 2 saniyelik toplam intro verir.
         */
        binding.introOverlay
            .animate()
            .alpha(0f)
            .setDuration(
                INTRO_FADE_DURATION_MS
            )
            .setInterpolator(
                DecelerateInterpolator()
            )
            .withEndAction {

                binding.lottieIntro.cancelAnimation()

                binding.introOverlay.visibility =
                    View.GONE

                binding.introOverlay.alpha =
                    1f

                startPostIntroTasks()
            }
            .start()
    }

    private fun finishIntroImmediately() {

        if (!::binding.isInitialized) {
            return
        }

        introFinished =
            true

        binding.root.removeCallbacks(
            introLoadTimeoutRunnable
        )

        binding.introOverlay.removeCallbacks(
            introFallbackRunnable
        )

        binding.introOverlay
            .animate()
            .cancel()

        binding.introBrandContainer
            .animate()
            .cancel()

        binding.brandWordmark
            .animate()
            .cancel()

        binding.lottieIntro.cancelAnimation()

        binding.introOverlay.visibility =
            View.GONE

        binding.introOverlay.alpha =
            1f

        startPostIntroTasks()
    }

    private fun dpToPx(
        dp: Float
    ): Float {

        return dp *
                resources.displayMetrics.density
    }

    private fun configureSwipeHint(
        isFreshLaunch: Boolean
    ) {

        val preferences =
            getSharedPreferences(
                "wallet_prefs",
                MODE_PRIVATE
            )

        val shownLaunchCount =
            preferences.getInt(
                "swipe_hint_launch_count",
                0
            )

        val shouldShow =
            shownLaunchCount < 3

        binding.tvSwipeHint.visibility =
            if (shouldShow) {

                View.VISIBLE

            } else {

                View.GONE
            }

        if (
            isFreshLaunch &&
            shouldShow
        ) {

            preferences.edit()
                .putInt(
                    "swipe_hint_launch_count",
                    shownLaunchCount + 1
                )
                .apply()
        }
    }

    private fun setupViewPager(
        shouldSelectDefaultPage: Boolean
    ) {

        binding.viewPager.adapter =
            ViewPagerAdapter(this)

        binding.viewPager.isUserInputEnabled =
            true

        binding.viewPager.offscreenPageLimit =
            2

        if (shouldSelectDefaultPage) {

            binding.viewPager.setCurrentItem(
                ViewPagerAdapter.DEFAULT_PAGE,
                false
            )
        }

        val pageTitles =
            arrayOf(
                getString(
                    R.string.tab_portfolio
                ),
                getString(
                    R.string.tab_budget
                ),
                getString(
                    R.string.tab_markets
                )
            )

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->

            tab.text =
                pageTitles[position]

            tab.contentDescription =
                pageTitles[position]

        }.attach()

        updateSwipeHint(
            ViewPagerAdapter.DEFAULT_PAGE
        )

        binding.viewPager
            .registerOnPageChangeCallback(
                object :
                    ViewPager2.OnPageChangeCallback() {

                    override fun onPageSelected(
                        position: Int
                    ) {

                        super.onPageSelected(
                            position
                        )

                        updateSwipeHint(
                            position
                        )
                    }
                }
            )
    }

    private fun updateSwipeHint(
        position: Int
    ) {

        binding.tvSwipeHint.text =
            when (position) {

                ViewPagerAdapter.INVESTMENTS_PAGE ->
                    getString(
                        R.string.swipe_hint_investments
                    )

                ViewPagerAdapter.DASHBOARD_PAGE ->
                    getString(
                        R.string.swipe_hint_dashboard
                    )

                ViewPagerAdapter.CURRENCY_PAGE ->
                    getString(
                        R.string.swipe_hint_markets
                    )

                else ->
                    null
            }
    }
}