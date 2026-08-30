package com.epatay.digitalwallet.ui.tutorial

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnLayout
import androidx.viewpager2.widget.ViewPager2
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.*
import com.epatay.digitalwallet.ui.ViewPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object TutorialManager {

    private const val PREF_TUTORIAL_STEP = "premium_tutorial_step"
    private const val PREF_DEMO_SEEDED = "has_seeded_demo_data"
    const val MAX_STEP = 14

    private var currentOverlayRoot: View? = null

    private fun dismissAnyOpenBottomSheet(activity: Activity) {
        if (activity is androidx.fragment.app.FragmentActivity) {
            val fragments = activity.supportFragmentManager.fragments
            for (f in fragments) {
                if (f is com.google.android.material.bottomsheet.BottomSheetDialogFragment) {
                    try { f.dismissAllowingStateLoss() } catch (ignored: Exception) { }
                }
                for (child in f.childFragmentManager.fragments) {
                    if (child is com.google.android.material.bottomsheet.BottomSheetDialogFragment) {
                        try { child.dismissAllowingStateLoss() } catch (ignored: Exception) { }
                    }
                }
            }
        }
    }

    private fun getOverlayContainer(activity: Activity): ViewGroup? {
        if (activity is androidx.fragment.app.FragmentActivity) {
            val fragments = activity.supportFragmentManager.fragments
            for (f in fragments) {
                if (f is com.google.android.material.bottomsheet.BottomSheetDialogFragment && f.dialog?.isShowing == true) {
                    val decor = f.dialog?.window?.decorView as? ViewGroup
                    if (decor != null) return decor
                }
                for (child in f.childFragmentManager.fragments) {
                    if (child is com.google.android.material.bottomsheet.BottomSheetDialogFragment && child.dialog?.isShowing == true) {
                        val decor = child.dialog?.window?.decorView as? ViewGroup
                        if (decor != null) return decor
                    }
                }
            }
        }
        return activity.findViewById<ViewGroup>(android.R.id.content)
    }

    private fun getStatusBarHeight(activity: Activity): Int {
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) activity.resources.getDimensionPixelSize(resourceId) else (28 * activity.resources.displayMetrics.density).toInt()
    }

    /**
     * Restarts tutorial from the beginning (used from Settings or debugging)
     */
    fun restartTutorial(activity: Activity) {
        val prefs = activity.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_TUTORIAL_STEP, 0).apply()
        seedDemoData(activity)

        dismissAnyOpenBottomSheet(activity)

        val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        viewPager?.setCurrentItem(ViewPagerAdapter.DASHBOARD_PAGE, false)
        bottomNav?.selectedItemId = R.id.nav_budget

        viewPager?.postDelayed({
            startStepIfReady(activity, 0, null)
        }, 300)
    }

    /**
     * Checks if tutorial step is active and displays overlay if ready
     */
    fun startStepIfReady(activity: Activity, stepNumber: Int, vararg targetViews: View?) {
        val prefs = activity.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        val currentStep = prefs.getInt(PREF_TUTORIAL_STEP, 0)
        
        if (currentStep == stepNumber && currentStep < MAX_STEP) {
            if (currentStep == 0) {
                seedDemoData(activity)
            }
            showOverlay(activity, stepNumber, *targetViews)
        }
    }

    private fun showOverlay(activity: Activity, step: Int, vararg targetViews: View?) {
        // Remove existing overlay cleanly
        currentOverlayRoot?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        val rootContent = getOverlayContainer(activity) ?: activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val overlay = LayoutInflater.from(activity).inflate(R.layout.layout_tutorial_overlay, rootContent, false)
        currentOverlayRoot = overlay
        rootContent.addView(overlay)

        val canvasView = overlay.findViewById<TutorialCanvasView>(R.id.tutorialCanvasView)
        val card = overlay.findViewById<MaterialCardView>(R.id.tutorialCard)
        val tvTitle = overlay.findViewById<TextView>(R.id.tvTutorialTitle)
        val tvDesc = overlay.findViewById<TextView>(R.id.tvTutorialDesc)
        val btnNext = overlay.findViewById<MaterialButton>(R.id.btnTutorialNext)
        val btnSkip = overlay.findViewById<MaterialButton>(R.id.btnTutorialSkip)

        val mainContainer = activity.findViewById<View>(R.id.mainContent)
        
        if (step in 5..7) {
            // In steps 5, 6, 7: Live BottomSheets (Monthly Report, Savings Goals, Settings) are open!
            // No blur and no dark dimming so the actual underlying data/graphs are 100% crystal clear.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainContainer != null) {
                mainContainer.setRenderEffect(null)
            }
            canvasView.visibility = View.GONE
            canvasView.useBlurFallback = false
        } else {
            canvasView.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainContainer != null) {
                mainContainer.setRenderEffect(RenderEffect.createBlurEffect(22f, 22f, Shader.TileMode.CLAMP))
                canvasView.useBlurFallback = false
            } else {
                canvasView.useBlurFallback = true
            }
        }

        canvasView.setTargets(*targetViews)

        // Setup texts & button labels per step
        btnNext.text = "Devam"
        when (step) {
            0 -> {
                tvTitle.text = "VarlıkCep'e Hoş Geldin 👋"
                tvDesc.text = "Gelir, gider ve tüm varlıklarını tek noktadan güvenle yönetmeye hazır mısın? Sana özel hazırladığımız kısa rehbere başlayalım!"
                btnNext.text = "Tura Başla"
            }
            1 -> {
                tvTitle.text = "Kasa & Net Bakiye 💳"
                tvDesc.text = "Aylık toplam gelirin, harcamaların ve kalan net bakiyen burada anlık olarak özetlenir."
            }
            2 -> {
                tvTitle.text = "Hızlı İşlem Ekle ➕"
                tvDesc.text = "Yeni bir gelir veya gider kaydını bu butona dokunarak saniyeler içinde cüzdanına ekleyebilirsin."
            }
            3 -> {
                tvTitle.text = "Harcama & Gelir Kayıtları 📋"
                tvDesc.text = "Eklediğin tüm işlemler bu listede tarih sırasıyla listelenir. Harcamalarının detaylarını ve kategorilerini buradan görebilirsin."
            }
            4 -> {
                tvTitle.text = "Düzenli & Yaklaşan Ödemeler 📅"
                tvDesc.text = "Kira, fatura ve abonelik gibi düzenli ödemelerin vadesi yaklaştığında burada listelenir. Günü geldiğinde tek tıkla işleme dönüştürebilirsin."
            }
            5 -> {
                tvTitle.text = "Aylık Finans Raporu & Grafikler 📊"
                tvDesc.text = "İçinde bulunduğunuz ayın toplam gelir, gider ve bütçe durumunu; kategorilere göre harcama dağılımınızı renkli pasta grafiğiyle buradan detaylıca inceleyebilirsiniz."
            }
            6 -> {
                tvTitle.text = "Birikim Hedefleri 🎯"
                tvDesc.text = "Hayalleriniz için hedefler oluşturabilir (örneğin Yaz Tatili Fonu), birikim ekleyip çekebilir, hedefe kalan gün ve günlük tasarruf önerilerini buradan takip edebilirsiniz."
            }
            7 -> {
                tvTitle.text = "Ayarlar, Tercihler ve Tema ⚙️"
                tvDesc.text = "Karanlık veya Aydınlık tema modunu seçebilir, aylık bütçe limitinizi belirleyebilir, günlük 16:00 portföy bildirimini açabilir ve adil kullanım kotanızı buradan yönetebilirsiniz."
            }
            8 -> {
                tvTitle.text = "Google Hesabı & Yedekleme ☁️"
                tvDesc.text = "Verilerini bulutta güvenle yedeklemek ve tüm cihazlarından erişebilmek için Google hesabınla giriş yapabilirsin."
                btnNext.text = "Portföye Geç"
            }
            9 -> {
                tvTitle.text = "Portföy & Varlıklarım 📈"
                tvDesc.text = "Altın ve döviz birikimlerinin toplam piyasa değeri ve toplam kâr/zarar durumun burada canlı hesaplanır."
            }
            10 -> {
                tvTitle.text = "Alış, Satış ve Kâr/Zarar Mantığı 💡"
                tvDesc.text = "Varlık eklerken girdiğin Alış Kuru ile güncel Piyasa Satış Fiyatı (Makas) anlık karşılaştırılır. Net kâr veya zarar tutarın her varlık için otomatik olarak hesaplanır."
            }
            11 -> {
                tvTitle.text = "Varlık ve Birikim Ekle 💰"
                tvDesc.text = "Satın aldığın gram altın, çeyrek altın, dolar veya euro birikimlerini alış fiyatınla portföyüne buradan kaydedebilirsin."
                btnNext.text = "Piyasalara Geç"
            }
            12 -> {
                tvTitle.text = "TCMB Gösterge Döviz Kurları 🏛️"
                tvDesc.text = "Türkiye Cumhuriyet Merkez Bankası (TCMB) resmi gösterge niteliğindeki kurlarıdır. Hafta içi her iş gününde saat 15:30'da güncellenir."
            }
            13 -> {
                tvTitle.text = "Canlı Altın Fiyatları 🪙"
                tvDesc.text = "Gram, Çeyrek, Yarım ve Tam altın fiyatları piyasa verileri baz alınarak anlık ve gösterge niteliğinde sunulur."
                btnNext.text = "Turu Tamamla"
            }
        }

        // Dynamic positioning & sizing
        val statusBarHeight = getStatusBarHeight(activity)
        val density = activity.resources.displayMetrics.density
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        val isCompactStep = step in 5..7
        if (isCompactStep) {
            // Sleek & compact banner layout for live sheets - perfectly positioned under status bar
            params.gravity = Gravity.TOP
            params.topMargin = statusBarHeight + (10 * density).toInt()
            params.bottomMargin = 0
            params.leftMargin = (16 * density).toInt()
            params.rightMargin = (16 * density).toInt()

            card.radius = 16 * density
            val innerLayout = (card as? ViewGroup)?.getChildAt(0) as? android.widget.LinearLayout
            innerLayout?.setPadding(
                (14 * density).toInt(),
                (10 * density).toInt(),
                (14 * density).toInt(),
                (10 * density).toInt()
            )
            tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            tvDesc.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11.5f)
            (tvDesc.layoutParams as? android.widget.LinearLayout.LayoutParams)?.topMargin = (3 * density).toInt()
            ((btnNext.parent as? View)?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.topMargin = (6 * density).toInt()
            
            btnNext.minHeight = 0
            btnNext.setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
            btnSkip.minHeight = 0
            btnSkip.setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
        } else {
            params.leftMargin = (20 * density).toInt()
            params.rightMargin = (20 * density).toInt()

            val isCardAtTop = when (step) {
                2, 3, 10, 11 -> true
                else -> false
            }

            if (isCardAtTop) {
                params.gravity = Gravity.TOP
                params.topMargin = statusBarHeight + (16 * density).toInt()
                params.bottomMargin = 0
            } else {
                params.gravity = Gravity.BOTTOM
                params.bottomMargin = (76 * density).toInt()
                params.topMargin = 0
            }
        }
        card.layoutParams = params

        val isCardAtTop = (step in 5..7) || when (step) {
            2, 3, 10, 11 -> true
            else -> false
        }

        // Spring Bounce Entrance Animation
        val startY = if (isCardAtTop) -180f else 180f
        card.translationY = startY
        card.alpha = 0f
        card.doOnLayout {
            val animY = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, 0f)
            val animAlpha = ObjectAnimator.ofFloat(card, View.ALPHA, 1f)
            AnimatorSet().apply {
                playTogether(animY, animAlpha)
                interpolator = OvershootInterpolator(1.1f)
                duration = 350
                start()
            }
        }

        btnSkip.setOnClickListener {
            finishTutorial(activity)
        }

        btnNext.setOnClickListener {
            advanceStep(activity, step + 1)
        }
    }

    private fun advanceStep(activity: Activity, nextStep: Int) {
        val prefs = activity.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_TUTORIAL_STEP, nextStep).apply()

        // Clear current overlay blur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.findViewById<View>(R.id.mainContent)?.setRenderEffect(null)
        }
        currentOverlayRoot?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        currentOverlayRoot = null

        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

        when (nextStep) {
            1 -> {
                val target = activity.findViewById<View>(R.id.cardDashboard)
                showOverlay(activity, 1, target)
            }
            2 -> {
                val target = activity.findViewById<View>(R.id.fabAddExpense)
                showOverlay(activity, 2, target)
            }
            3 -> {
                // Collapse recurring & quick actions if they were open so transactions list is clear
                val recurringContent = activity.findViewById<View>(R.id.layoutRecurringContent)
                if (recurringContent?.visibility == View.VISIBLE) {
                    activity.findViewById<View>(R.id.layoutRecurringHeader)?.performClick()
                }
                val quickActionsContent = activity.findViewById<View>(R.id.layoutQuickActionsContent)
                if (quickActionsContent?.visibility == View.VISIBLE) {
                    activity.findViewById<View>(R.id.layoutQuickActionsHeader)?.performClick()
                }
                
                val transactionsHeader = activity.findViewById<View>(R.id.layoutTransactionsHeader)
                val rvTransactions = activity.findViewById<View>(R.id.rvTransactions)
                showOverlay(activity, 3, transactionsHeader, rvTransactions)
            }
            4 -> {
                // Ensure Recurring section is expanded
                val recurringHeader = activity.findViewById<View>(R.id.layoutRecurringHeader)
                val recurringContent = activity.findViewById<View>(R.id.layoutRecurringContent)
                if (recurringContent?.visibility != View.VISIBLE) {
                    recurringHeader?.performClick()
                }
                val cardRecurring = activity.findViewById<View>(R.id.cardRecurring)
                showOverlay(activity, 4, cardRecurring ?: recurringHeader)
            }
            5 -> {
                dismissAnyOpenBottomSheet(activity)
                activity.findViewById<View>(R.id.btnMonthlyReport)?.performClick()
                activity.window.decorView.postDelayed({
                    showOverlay(activity, 5, null)
                }, 400)
            }
            6 -> {
                dismissAnyOpenBottomSheet(activity)
                activity.findViewById<View>(R.id.btnSavingsGoals)?.performClick()
                activity.window.decorView.postDelayed({
                    showOverlay(activity, 6, null)
                }, 400)
            }
            7 -> {
                dismissAnyOpenBottomSheet(activity)
                activity.findViewById<View>(R.id.btnSettings)?.performClick()
                activity.window.decorView.postDelayed({
                    showOverlay(activity, 7, null)
                }, 400)
            }
            8 -> {
                dismissAnyOpenBottomSheet(activity)
                val target = activity.findViewById<View>(R.id.btnProfile)
                showOverlay(activity, 8, target)
            }
            9 -> {
                dismissAnyOpenBottomSheet(activity)
                // Navigate smoothly to Portfolio Page (Index 0 in ViewPagerAdapter)
                viewPager?.setCurrentItem(ViewPagerAdapter.INVESTMENTS_PAGE, true)
                bottomNav?.selectedItemId = R.id.nav_portfolio
                viewPager?.postDelayed({
                    val target = activity.findViewById<View>(R.id.cardGrandTotal)
                    showOverlay(activity, 9, target)
                }, 400)
            }
            10 -> {
                val target = activity.findViewById<View>(R.id.rvInvestments) ?: activity.findViewById<View>(R.id.llInvestmentDetails)
                showOverlay(activity, 10, target)
            }
            11 -> {
                val target = activity.findViewById<View>(R.id.fabAddInvestment)
                showOverlay(activity, 11, target)
            }
            12 -> {
                // Navigate smoothly to Markets Page (Index 2 in ViewPagerAdapter)
                viewPager?.setCurrentItem(ViewPagerAdapter.CURRENCY_PAGE, true)
                bottomNav?.selectedItemId = R.id.nav_markets
                viewPager?.postDelayed({
                    val tabs = activity.findViewById<TabLayout>(R.id.marketsTabs)
                    val pager = activity.findViewById<ViewPager2>(R.id.marketsPager)
                    tabs?.getTabAt(0)?.select()
                    showOverlay(activity, 12, tabs, pager)
                }, 400)
            }
            13 -> {
                val tabs = activity.findViewById<TabLayout>(R.id.marketsTabs)
                val pager = activity.findViewById<ViewPager2>(R.id.marketsPager)
                tabs?.getTabAt(1)?.select()
                viewPager?.postDelayed({
                    showOverlay(activity, 13, tabs, pager)
                }, 250)
            }
            else -> {
                finishTutorial(activity)
            }
        }
    }

    private fun finishTutorial(activity: Activity) {
        val prefs = activity.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_TUTORIAL_STEP, MAX_STEP).apply()
        
        dismissAnyOpenBottomSheet(activity)

        // Clean up demo data cleanly
        cleanDemoData(activity)

        // Clear render effect and remove overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.findViewById<View>(R.id.mainContent)?.setRenderEffect(null)
        }
        currentOverlayRoot?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        currentOverlayRoot = null

        // Return user to Dashboard smoothly
        val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        viewPager?.setCurrentItem(ViewPagerAdapter.DASHBOARD_PAGE, true)
        com.epatay.digitalwallet.util.InAppNotification.show(
            activity,
            "Tebrikler! VarlıkCep'i keşfetmeye hazırsın.",
            com.epatay.digitalwallet.util.NotificationType.SUCCESS,
            3000L
        )
    }

    private fun seedDemoData(context: Context) {
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_DEMO_SEEDED, false)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TransactionDatabase.getDatabase(context.applicationContext)
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val todayStr = dateFormat.format(Date())
                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
                val yesterdayStr = dateFormat.format(yesterdayCal.time)
                val currentMonthKey = TransactionDateUtils.currentMonthKey(Calendar.getInstance())

                // 1. Transactions (Gelir & Gider)
                val demoTransactions = listOf(
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_1",
                        title = "Aylık Maaş Geliri",
                        amount = 55000.0,
                        category = "Maaş",
                        date = todayStr,
                        type = TransactionType.INCOME,
                        occurredOn = TransactionDateUtils.toDateKey(todayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_2",
                        title = "Freelance Ek Gelir",
                        amount = 12500.0,
                        category = "Ek Gelir",
                        date = todayStr,
                        type = TransactionType.INCOME,
                        occurredOn = TransactionDateUtils.toDateKey(todayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_3",
                        title = "Süpermarket Alışverişi",
                        amount = 3450.0,
                        category = "Market",
                        date = todayStr,
                        type = TransactionType.EXPENSE,
                        occurredOn = TransactionDateUtils.toDateKey(todayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_4",
                        title = "Kira ve Aidat",
                        amount = 16000.0,
                        category = "Ev",
                        date = yesterdayStr,
                        type = TransactionType.EXPENSE,
                        occurredOn = TransactionDateUtils.toDateKey(yesterdayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_5",
                        title = "Restoran & Akşam Yemeği",
                        amount = 1250.0,
                        category = "Yiyecek ve İçecek",
                        date = todayStr,
                        type = TransactionType.EXPENSE,
                        occurredOn = TransactionDateUtils.toDateKey(todayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_6",
                        title = "Elektrik & Su Faturası",
                        amount = 950.0,
                        category = "Fatura ve Abonelikler",
                        date = yesterdayStr,
                        type = TransactionType.EXPENSE,
                        occurredOn = TransactionDateUtils.toDateKey(yesterdayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_7",
                        title = "Benzin & Akaryakıt",
                        amount = 1800.0,
                        category = "Ulaşım",
                        date = todayStr,
                        type = TransactionType.EXPENSE,
                        occurredOn = TransactionDateUtils.toDateKey(todayStr)
                    ),
                    Transaction(
                        uuid = "DEMO_TUTORIAL_TX_8",
                        title = "Giyim Alışverişi",
                        amount = 2200.0,
                        category = "Alışveriş",
                        date = yesterdayStr,
                        type = TransactionType.EXPENSE,
                        occurredOn = TransactionDateUtils.toDateKey(yesterdayStr)
                    )
                )

                for (tx in demoTransactions) {
                    db.transactionDao().insertTransaction(tx)
                }

                // 2. Recurring Transactions
                db.recurringTransactionDao().insert(
                    RecurringTransaction(
                        uuid = "DEMO_TUTORIAL_REC_1",
                        title = "Ev İnterneti Faturası",
                        amount = 390.0,
                        category = "Fatura ve Abonelikler",
                        dayOfMonth = 15,
                        type = TransactionType.EXPENSE,
                        isActive = true
                    )
                )
                db.recurringTransactionDao().insert(
                    RecurringTransaction(
                        uuid = "DEMO_TUTORIAL_REC_2",
                        title = "Müzik/Dizi Abonelikleri",
                        amount = 260.0,
                        category = "Eğlence",
                        dayOfMonth = 1,
                        type = TransactionType.EXPENSE,
                        isActive = true
                    )
                )

                // 3. Category Budgets (Kategori Limitleri)
                db.categoryBudgetDao().upsert(
                    CategoryBudget(
                        monthKey = currentMonthKey,
                        category = "Market",
                        limitAmount = 6000.0
                    )
                )
                db.categoryBudgetDao().upsert(
                    CategoryBudget(
                        monthKey = currentMonthKey,
                        category = "Yiyecek ve İçecek",
                        limitAmount = 3500.0
                    )
                )

                // 4. Savings Goals (Birikim Hedefleri)
                val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, 4) }
                val targetDateKey = TransactionDateUtils.currentDateKey(targetCal)
                db.savingsGoalDao().insertGoal(
                    SavingsGoal(
                        uuid = "DEMO_TUTORIAL_GOAL_1",
                        title = "Yaz Tatili Fonu",
                        targetAmount = 30000.0,
                        targetDateKey = targetDateKey
                    )
                )
                db.savingsGoalDao().insertEntry(
                    SavingsGoalEntry(
                        uuid = "DEMO_TUTORIAL_GOAL_ENTRY_1",
                        goalId = "DEMO_TUTORIAL_GOAL_1",
                        amountDelta = 14000.0,
                        occurredOn = TransactionDateUtils.toDateKey(todayStr),
                        note = "İlk birikim"
                    )
                )

                // 5. Investments (Currency)
                val currencyRates = try { db.currencyRateDao().getAllRates() } catch (e: Exception) { emptyList() }
                val usdRate = currencyRates.firstOrNull { it.currencyCode.equals("USD", ignoreCase = true) }?.forexSelling
                    ?.takeIf { it > 0.0 } ?: 33.60
                val eurRate = currencyRates.firstOrNull { it.currencyCode.equals("EUR", ignoreCase = true) }?.forexSelling
                    ?.takeIf { it > 0.0 } ?: 37.10

                db.investmentDao().insertInvestment(
                    InvestmentItem(
                        uuid = "DEMO_TUTORIAL_INV_USD",
                        assetName = "USD",
                        amount = 1500.0,
                        buyPrice = usdRate,
                        buyDate = todayStr,
                        note = "Örnek Döviz Birikimi"
                    )
                )
                db.investmentDao().insertInvestment(
                    InvestmentItem(
                        uuid = "DEMO_TUTORIAL_INV_EUR",
                        assetName = "EUR",
                        amount = 800.0,
                        buyPrice = eurRate,
                        buyDate = todayStr,
                        note = "Örnek Euro Birikimi"
                    )
                )

                // 6. Gold Assets (Canlı Altın Kuru yoksa varsayılan)
                val goldRates = try { db.goldRateDao().getAllOnce() } catch (e: Exception) { emptyList() }
                val gramGoldRate = goldRates.firstOrNull { it.displayName.contains("Gram", ignoreCase = true) || it.type.contains("GRAM", ignoreCase = true) }?.sellingPrice
                    ?.takeIf { it > 0.0 } ?: 2950.0

                db.userGoldAssetDao().insert(
                    UserGoldAssetEntity(
                        uuid = "DEMO_TUTORIAL_GOLD_1",
                        goldType = "Gram Altın",
                        quantity = 20.0,
                        unit = "gr",
                        purchaseUnitPrice = gramGoldRate,
                        totalPurchaseCost = 20.0 * gramGoldRate,
                        purchaseDate = System.currentTimeMillis(),
                        note = "Örnek Altın Birikimi"
                    )
                )

                prefs.edit().putBoolean(PREF_DEMO_SEEDED, true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cleanDemoData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TransactionDatabase.getDatabase(context.applicationContext)
                val currentMonthKey = TransactionDateUtils.currentMonthKey(Calendar.getInstance())

                // 1. Clear via batch queries
                db.transactionDao().clearDemoTransactions()
                db.recurringTransactionDao().clearDemoRecurring()
                db.categoryBudgetDao().clearDemoBudgets()
                db.savingsGoalDao().clearDemoEntries()
                db.savingsGoalDao().clearDemoGoals()
                db.investmentDao().clearDemoInvestments()
                db.userGoldAssetDao().clearDemoGold()

                // 2. Extra safety by individual IDs
                for (i in 1..8) {
                    db.transactionDao().hardDeleteTransactionById("DEMO_TUTORIAL_TX_$i")
                }
                db.recurringTransactionDao().hardDelete("DEMO_TUTORIAL_REC_1")
                db.recurringTransactionDao().hardDelete("DEMO_TUTORIAL_REC_2")
                db.categoryBudgetDao().hardDelete(currentMonthKey, "Market")
                db.categoryBudgetDao().hardDelete(currentMonthKey, "Yiyecek ve İçecek")
                db.savingsGoalDao().hardDeleteEntry("DEMO_TUTORIAL_GOAL_ENTRY_1")
                db.savingsGoalDao().hardDeleteGoal("DEMO_TUTORIAL_GOAL_1")
                db.investmentDao().hardDeleteInvestmentById("DEMO_TUTORIAL_INV_USD")
                db.investmentDao().hardDeleteInvestmentById("DEMO_TUTORIAL_INV_EUR")
                db.userGoldAssetDao().hardDelete("DEMO_TUTORIAL_GOLD_1")

                // Also clean from Firestore if user logged in during tutorial
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    for (i in 1..8) {
                        firestore.collection("transactions").document("DEMO_TUTORIAL_TX_$i").delete()
                    }
                    firestore.collection("recurring_transactions_table").document("DEMO_TUTORIAL_REC_1").delete()
                    firestore.collection("recurring_transactions_table").document("DEMO_TUTORIAL_REC_2").delete()
                    firestore.collection("category_budgets").document("${currentMonthKey}_Market").delete()
                    firestore.collection("category_budgets").document("${currentMonthKey}_Yiyecek ve İçecek").delete()
                    firestore.collection("savings_goals").document("DEMO_TUTORIAL_GOAL_1").delete()
                    firestore.collection("savings_goal_entries").document("DEMO_TUTORIAL_GOAL_ENTRY_1").delete()
                    firestore.collection("investments").document("DEMO_TUTORIAL_INV_USD").delete()
                    firestore.collection("investments").document("DEMO_TUTORIAL_INV_EUR").delete()
                    firestore.collection("user_gold_assets").document("DEMO_TUTORIAL_GOLD_1").delete()
                } catch (ignored: Exception) { }

                val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean(PREF_DEMO_SEEDED, false).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

