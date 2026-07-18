package com.epatay.digitalwallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
    }

    private fun setupViewPager() {

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
        binding.viewPager.setCurrentItem(
            ViewPagerAdapter.DEFAULT_PAGE,
            false
        )

        val pageTitles = arrayOf(
            "Portföyüm",
            "Bütçem",
            "Piyasalar"
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
                    "Bütçeme geçmek için sola kaydır →"

                ViewPagerAdapter.DASHBOARD_PAGE ->
                    "← Portföyüm   •   Piyasalar →"

                ViewPagerAdapter.CURRENCY_PAGE ->
                    "← Bütçeme geçmek için sağa kaydır"

                else ->
                    ""
            }
    }
}