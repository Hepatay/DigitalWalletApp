package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.data.CurrencyItem
import com.epatay.digitalwallet.databinding.ItemCurrencyBinding
import java.util.Locale

class CurrencyAdapter(
    private var currencyList: List<CurrencyItem>
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    class CurrencyViewHolder(val binding: ItemCurrencyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder =
        CurrencyViewHolder(
            ItemCurrencyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val item = currencyList[position]
        
        holder.binding.ivFlag.setImageResource(item.flagResId)
        
        val unitText = if (item.unit > 1) " (${item.unit}x)" else ""
        val upperName = item.name.uppercase(Locale("tr", "TR"))
        holder.binding.tvCurrencyCode.text = "${item.code}$unitText - $upperName"
        
        holder.binding.tvCurrencyBuying.text = GoldRateFormatter.price(item.forexBuying)
        holder.binding.tvCurrencySelling.text = GoldRateFormatter.price(item.forexSelling)
            
        if (item.spreadTl != null) {
            holder.binding.tvCurrencySpread.visibility = android.view.View.VISIBLE
            holder.binding.tvCurrencySpread.text = GoldRateFormatter.price(item.spreadTl)
        } else {
            holder.binding.tvCurrencySpread.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount(): Int = currencyList.size

    fun updateData(newList: List<CurrencyItem>) {
        currencyList = newList
        notifyDataSetChanged()
    }
}
