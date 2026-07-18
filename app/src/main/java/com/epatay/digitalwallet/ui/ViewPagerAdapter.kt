package com.epatay.digitalwallet.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    activity: AppCompatActivity
) : FragmentStateAdapter(activity) {

    companion object {

        // Sayfa sıraları
        const val INVESTMENTS_PAGE = 0
        const val DASHBOARD_PAGE = 1
        const val CURRENCY_PAGE = 2

        // Uygulamanın açılacağı varsayılan sayfa
        const val DEFAULT_PAGE = DASHBOARD_PAGE

        const val PAGE_COUNT = 3
    }

    override fun getItemCount(): Int {
        return PAGE_COUNT
    }

    override fun createFragment(position: Int): Fragment {

        return when (position) {

            INVESTMENTS_PAGE ->
                AnalysisFragment()

            DASHBOARD_PAGE ->
                DashboardFragment()

            CURRENCY_PAGE ->
                CurrencyFragment()

            else ->
                throw IllegalArgumentException(
                    "Geçersiz sayfa konumu: $position"
                )
        }
    }
}