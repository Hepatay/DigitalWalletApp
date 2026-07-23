package com.epatay.digitalwallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.epatay.digitalwallet.databinding.ActivityMainBinding
import com.epatay.digitalwallet.recurring.RecurringTransactionScheduler
import com.epatay.digitalwallet.ui.ViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var timeReceiverRegistered = false
    private var timeRefreshPending = false

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
                    timeRefreshPending = true
                    binding.root.post {
                        timeRefreshPending = false

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

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RecurringTransactionScheduler.schedule(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val isFreshLaunch =
            savedInstanceState == null

        configureSwipeHint(
            isFreshLaunch
        )

        setupViewPager(isFreshLaunch)
    }

    override fun onResume() {
        super.onResume()
        RecurringTransactionScheduler.runNow(this)
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
            timeReceiverRegistered = true
        }
    }

    override fun onStop() {
        if (timeReceiverRegistered) {
            unregisterReceiver(
                timeChangeReceiver
            )
            timeReceiverRegistered = false
        }

        super.onStop()
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

    private fun setupViewPager(shouldSelectDefaultPage: Boolean) {

        binding.viewPager.adapter =
            ViewPagerAdapter(this)

        // Sayfalar arasında yatay kaydırmayı açar
        binding.viewPager.isUserInputEnabled = true

        // Üç sayfayı bellekte tutar
        binding.viewPager.offscreenPageLimit = 2

        /*
         * Uygulama açıldığında ortadaki
         * Bütçem ekranı gösterilir.
         */
        if (shouldSelectDefaultPage) {
            binding.viewPager.setCurrentItem(
                ViewPagerAdapter.DEFAULT_PAGE,
                false
            )
        }

        val pageTitles = arrayOf(
            getString(R.string.tab_portfolio),
            getString(R.string.tab_budget),
            getString(R.string.tab_markets)
        )

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->

            tab.text = pageTitles[position]
            tab.contentDescription = pageTitles[position]

        }.attach()

        updateSwipeHint(
            ViewPagerAdapter.DEFAULT_PAGE
        )

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    updateSwipeHint(position)
                }
            }
        )
    }

    private fun updateSwipeHint(position: Int) {

        binding.tvSwipeHint.text =
            when (position) {

                ViewPagerAdapter.INVESTMENTS_PAGE ->
                    getString(R.string.swipe_hint_investments)

                ViewPagerAdapter.DASHBOARD_PAGE ->
                    getString(R.string.swipe_hint_dashboard)

                ViewPagerAdapter.CURRENCY_PAGE ->
                    getString(R.string.swipe_hint_markets)

                else ->
                    null
            }
    }
}
