package com.epatay.digitalwallet.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.epatay.digitalwallet.ui.DashboardFragment
import com.epatay.digitalwallet.ui.AnalysisFragment

class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    // Toplam kaç sayfamız olduğunu belirtiyoruz (0: Analiz, 1: Dashborad, 2: Doviz)
    override fun getItemCount(): Int = 3

    // Kaydırdıkça hangi sayfanın açılacağını belirliyoruz
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AnalysisFragment()
            1 -> DashboardFragment()
            else -> CurrencyFragment() // Yeni sayfamız
        }
    }
}