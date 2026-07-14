package com.epatay.digitalwallet

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.epatay.digitalwallet.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // XML'deki artı butonuna tıklandığında çalışacak kod
        binding.fabAddExpense.setOnClickListener {
            Toast.makeText(this, "Yeni harcama ekranına gidilecek", Toast.LENGTH_SHORT).show()
        }
    }
}