package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyItem

class CurrencyAdapter(private var currencyList: List<CurrencyItem>) :
    RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    // Varsayılan miktar 1.0 olarak başlar
    private var multiplier: Double = 1.0

    class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRate: TextView = view.findViewById(R.id.tvCurrencyRate)
        val tvName: TextView = view.findViewById(R.id.tvCurrencyName)
        val ivFlag: android.widget.ImageView = view.findViewById(R.id.ivFlag) // İkonu bul
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_currency, parent, false)
        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val item = currencyList[position]

        // Girilen miktar ile kuru çarpıyoruz
        val totalTry = item.rateValue * multiplier

        // Çıktıyı dinamik olarak ekrana basıyoruz (Örn: 50 USD = 1600.00 TRY)
        val amountString = if (multiplier % 1.0 == 0.0) multiplier.toInt().toString() else multiplier.toString()
        holder.tvRate.text = "$amountString ${item.code} = ${String.format("%.2f", totalTry)} TRY"
        holder.tvName.text = item.name

        holder.ivFlag.setImageResource(item.flagIcon)
    }

    override fun getItemCount(): Int = currencyList.size

    fun updateData(newList: List<CurrencyItem>) {
        currencyList = newList
        notifyDataSetChanged()
    }

    // EditText'ten gelen yeni miktarı alıp listeyi yenileyen fonksiyon
    fun updateMultiplier(newMultiplier: Double) {
        multiplier = newMultiplier
        notifyDataSetChanged()
    }
}