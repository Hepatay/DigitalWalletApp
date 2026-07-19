package com.epatay.digitalwallet

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.epatay.digitalwallet.databinding.ActivityMainBinding
import com.epatay.digitalwallet.ui.ViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager(savedInstanceState == null)
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
