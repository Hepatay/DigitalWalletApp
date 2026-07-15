package com.epatay.digitalwallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.epatay.digitalwallet.databinding.ActivityMainBinding
import com.epatay.digitalwallet.ui.ViewPagerAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewPager2 ve Adapter kurulumu
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
    }
}