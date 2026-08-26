package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.data.GoldRate
import com.epatay.digitalwallet.databinding.ItemGoldRateBinding

class GoldAdapter : RecyclerView.Adapter<GoldAdapter.GoldViewHolder>() {

    private var rates: List<GoldRate> = emptyList()

    class GoldViewHolder(val binding: ItemGoldRateBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoldViewHolder =
        GoldViewHolder(
            ItemGoldRateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: GoldViewHolder, position: Int) {
        val rate = rates[position]
        val name = rate.type.displayName.replace("Ata / Cumhuriyet", "Cumhuriyet")
        holder.binding.tvGoldName.text = name
        holder.binding.tvGoldBuying.text = GoldRateFormatter.price(rate.buyingPrice)
        holder.binding.tvGoldSelling.text = GoldRateFormatter.price(rate.sellingPrice)
        holder.binding.tvGoldSpread.text = GoldRateFormatter.price(rate.spread)
        
        
    }

    override fun getItemCount(): Int = rates.size

    fun submitList(newRates: List<GoldRate>) {
        rates = newRates
        notifyDataSetChanged()
    }
}
