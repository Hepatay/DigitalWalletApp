package com.epatay.digitalwallet.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.databinding.ItemInvestmentBinding
import kotlin.math.abs

class InvestmentAdapter : RecyclerView.Adapter<InvestmentAdapter.InvestmentViewHolder>() {

    private var investmentList = emptyList<InvestmentItem>()
    private var onItemLongClickListener: ((InvestmentItem) -> Unit)? = null

    class InvestmentViewHolder(val binding: ItemInvestmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val binding = ItemInvestmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvestmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        val currentItem = investmentList[position]

        holder.binding.tvAssetName.text = currentItem.assetName
        holder.binding.tvAmount.text = "${currentItem.amount} Adet"
        holder.binding.tvBuyPrice.text = "Maliyet Kur: ₺${"%.2f".format(currentItem.buyPrice)}"
        holder.binding.tvBuyDate.text = currentItem.buyDate

        // --- KÂR / ZARAR HESAPLAMASI ---
        val currencyManager = com.epatay.digitalwallet.data.CurrencyManager(holder.itemView.context)
        val rates = currencyManager.getSavedRates()?.conversion_rates

        // when bloğu burada kapanmalı
        val anlikKur = when (currentItem.assetName) {
            "USD" -> {
                val rate = rates?.get("USD")?.toDouble() ?: currentItem.buyPrice
                // Eğer sistem bize 0.02 gibi ters bir değer veriyorsa, onu 1'e bölerek (tersini alarak) düzeltiyoruz
                if (rate < 1.0) (1.0 / rate) else rate
            }
            "EUR" -> {
                val rate = rates?.get("EUR")?.toDouble() ?: currentItem.buyPrice
                if (rate < 1.0) (1.0 / rate) else rate
            }
            "GBP" -> {
                val rate = rates?.get("GBP")?.toDouble() ?: currentItem.buyPrice
                if (rate < 1.0) (1.0 / rate) else rate
            }
            else -> currentItem.buyPrice
        }
        // Hesaplama kodları when bloğunun DIŞINDA
        val toplamMaliyet = currentItem.amount * currentItem.buyPrice
        val guncelDeger = currentItem.amount * anlikKur
        val fark = guncelDeger - toplamMaliyet

        // Log ile takibi kolaylaştıralım
        android.util.Log.d("DEBUG_LOG", "${currentItem.assetName} | Maliyet: ${currentItem.buyPrice} | Güncel: $anlikKur | Fark: $fark")

        // Görselleştirme
        if (abs(fark) < 0.01) {
            holder.binding.tvProfitLoss.text = "₺0.00"
            holder.binding.tvProfitLoss.setTextColor(Color.parseColor("#888888"))
        } else if (fark > 0) {
            holder.binding.tvProfitLoss.text = String.format("+₺%.2f Kâr", fark)
            holder.binding.tvProfitLoss.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.binding.tvProfitLoss.text = String.format("-₺%.2f Zarar", abs(fark))
            holder.binding.tvProfitLoss.setTextColor(Color.parseColor("#F44336"))
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClickListener?.invoke(currentItem)
            true
        }
    }

    override fun getItemCount(): Int = investmentList.size

    fun setData(investments: List<InvestmentItem>) {
        this.investmentList = investments
        notifyDataSetChanged()
    }

    fun setOnItemLongClickListener(listener: (InvestmentItem) -> Unit) {
        this.onItemLongClickListener = listener
    }
}