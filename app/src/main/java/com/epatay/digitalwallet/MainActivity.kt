package com.epatay.digitalwallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.data.Expense
import com.epatay.digitalwallet.databinding.ActivityMainBinding
import com.epatay.digitalwallet.ui.ExpenseAdapter
import com.epatay.digitalwallet.ui.ExpenseViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseViewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adapter'ı başlat ve RecyclerView'a bağla
        val adapter = ExpenseAdapter()
        binding.rvTransactions.adapter = adapter
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)

        // ViewModel'i başlat
        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        // Veritabanını dinle ve liste değiştikçe Adapter'a gönder
        expenseViewModel.allExpenses.observe(this) { expenses ->
            adapter.setData(expenses)
        }

        // Test için: Butona basıldıkça veritabanına yeni kayıt eklesin
        binding.fabAddExpense.setOnClickListener {
            val ornekHarcama = Expense(
                title = "Kahve",
                amount = 120.0,
                category = "Gıda",
                date = "14.07.2026"
            )
            expenseViewModel.insert(ornekHarcama)
        }
    }
}